import isel.leic.utils.Time

object TUI {

    // remember last shown lines to avoid unnecessary LCD writes
    private var prevTopShown: String = ""
    private var prevBottomShown: String = ""

    private enum class MenuMode { HOME, ID, LIST }

    private fun centeredLine(text: String): String {
        if (text.length >= LCD.COLS) return text.take(LCD.COLS)
        val left = (LCD.COLS - text.length) / 2
        val right = LCD.COLS - text.length - left
        return " ".repeat(left) + text + " ".repeat(right)
    }

    fun showCentered(top: String, bottom: String) {
        // avoid clearing/writing the LCD when content didn't change
        if (top == prevTopShown && bottom == prevBottomShown) return
        prevTopShown = top
        prevBottomShown = bottom
        LCD.clear()
        LCD.cursor(0, 0)
        LCD.write(centeredLine(top))
        LCD.cursor(1, 0)
        LCD.write(centeredLine(bottom))
    }

    /**
     * Mostra a tela inicial: "Welcome" centrado na linha 0 e nome da estação na linha 1.
     */
    fun initialScreen(stationName: String) {
        showCentered("Welcome!", stationName)
    }

    fun buyMenu(stations: List<Station>): Station? {
        val sorted = stations.sortedBy { it.id }
        val byId = sorted.associateBy { it.id }
        var mode = MenuMode.HOME
        var digits = ""
        var index = 0
        var dirty = true

        while (true) {
            if (dirty) {
                when (mode) {
                    MenuMode.HOME -> showCentered("Buy ticket", "#=ID *=LIST")
                    MenuMode.ID -> showCentered("Enter station ID", "ID:$digits #=OK *=BK")
                    MenuMode.LIST -> {
                        val station = sorted[index]
                        showCentered(station.name, "#=CONFIRM *=BACK")
                    }
                }
                dirty = false
            }

            when (val key = KBD.waitKey(80)) {
                KBD.NONE.toChar() -> {}

                '#' -> {
                    when (mode) {
                        MenuMode.HOME -> {
                            mode = MenuMode.ID
                            digits = ""
                            dirty = true
                        }

                        MenuMode.ID -> {
                            val selected = digits.toIntOrNull()?.let { byId[it] }
                            if (selected != null) {
                                return selected
                            } else {
                                showCentered("Unknown ID", "Try again")
                                Time.sleep(1000)
                                digits = ""
                                dirty = true
                            }
                        }

                        MenuMode.LIST -> {
                            val selected = sorted[index]
                            return selected
                        }
                    }
                }

                '*' -> {
                    when (mode) {
                        MenuMode.HOME -> {
                            mode = MenuMode.LIST
                            dirty = true
                        }

                        MenuMode.ID -> {
                            mode = MenuMode.HOME
                            digits = ""
                            dirty = true
                        }

                        MenuMode.LIST -> {
                            mode = MenuMode.HOME
                            dirty = true
                        }
                    }
                }

                'A' -> {
                    if (mode == MenuMode.LIST && index > 0) {
                        index--
                        dirty = true
                    }
                }

                'B' -> {
                    if (mode == MenuMode.LIST && index < sorted.lastIndex) {
                        index++
                        dirty = true
                    }
                }

                in '0'..'9' -> {
                    if (mode == MenuMode.ID) {
                        digits += key
                        dirty = true
                    }
                }

                else -> {}
            }
        }
    }

    /**
     * Gerencia a fase de pagamento: calcula o preço entre origin e dest (somando as paragens
     * entre origem e destino, excluindo a origem), permite inserir moedas e alternar tipo
     * de bilhete (ida/ida-volta com '*'). '#' cancela a compra e devolve. Quando o montante
     * inserido é suficiente, inicia a impressão e espera que o utilizador recolha o bilhete.
     * Retorna true se a compra foi concluída, false se cancelada.
     */
    fun buyPhase(
        origin: Station,
        dest: Station,
        stations: List<Station>,
        testMode: Boolean = false,
        coinCounters: MutableMap<Double, Int>? = null,
        ticketCounters: MutableMap<String, Int>? = null
    ): Boolean {
        // encontrar índices
        val sorted = stations.sortedBy { it.id }
        val start = sorted.indexOfFirst { it.id == origin.id }
        val end = sorted.indexOfFirst { it.id == dest.id }

        val basePrice = if (start == end) 0.0 else {
            if (start < end) sorted.subList(start + 1, end + 1).sumOf { it.price }
            else sorted.subList(end, start).sumOf { it.price }
        }

        var roundTrip = false
        var total = basePrice
        var inserted = 0.0
        val insertedCoins = mutableMapOf<Double, Int>()

        // moedas: teclas 1,2,5,0 -> 0.10,0.20,0.50,1.00 euros
        val coins = mapOf('1' to 0.1, '2' to 0.2, '5' to 0.5, '0' to 1.0)

        if (testMode) {
            return printingPhase(origin, dest, roundTrip, total, total)
        }

        // iremos actualizar o ecrã apenas quando o texto mudar para evitar piscar
        var prevTop = ""
        var prevBottom = ""

        while (true) {
            // preparar as linhas a mostrar
            val top = if (roundTrip) "RoundTrip ${"%.2f".format(total)}" else "Single ${"%.2f".format(total)}"
            val bottom = "In:${"%.2f".format(inserted)} #=Cancel"
            // só actualizar o LCD quando houver alteração de texto
            if (top != prevTop || bottom != prevBottom) {
                showCentered(top, bottom)
                prevTop = top
                prevBottom = bottom
            }

            val key = KBD.waitKey(200)
            when (key) {
                KBD.NONE.toChar() -> {}
                '#' -> {
                    // cancelar e devolver
                    showCentered("Cancelled", "Refunded ${"%.2f".format(inserted)}")
                    Time.sleep(1500)
                    return false
                }
                '*' -> {
                    roundTrip = !roundTrip
                    total = basePrice * (if (roundTrip) 2.0 else 1.0)
                }
                in coins.keys -> {
                    val value = coins[key] ?: 0.0
                    inserted += value
                    insertedCoins[value] = (insertedCoins[value] ?: 0) + 1
                    Time.sleep(200)
                }
                else -> {
                    // ignorar outras teclas
                }
            }

            if (inserted >= total) {
                // suficiente -> iniciar impressão
                val ok = printingPhase(origin, dest, roundTrip, total, inserted)
                if (ok) {
                    coinCounters?.let { counters ->
                        for ((value, count) in insertedCoins) {
                            counters[value] = (counters[value] ?: 0) + count
                        }
                    }
                    ticketCounters?.let { counters ->
                        counters[dest.name] = (counters[dest.name] ?: 0) + 1
                    }
                }
                return ok
            }
        }
    }

    /**
     * Fase de impressão: mostra informações do bilhete, inicia o mecanismo de impressão,
     * aguarda confirmação (com timeout) e deixa o utilizador recolher o bilhete.
     * Permite cancelar a operação premindo '#'; nesse caso devolve as moedas.
     */
    private fun printingPhase(origin: Station, dest: Station, roundTrip: Boolean, total: Double, inserted: Double): Boolean {
        val ticketType = if (roundTrip) "RoundTrip" else "Single"
        val change = inserted - total

        // Mostrar tipo de bilhete e origem
        showCentered(ticketType, origin.name)
        Time.sleep(2000)

        // Mostrar destino
        showCentered("Destination", dest.name)
        Time.sleep(2000)

        // Mostrar preço e troco
        showCentered("Total:${"%.2f".format(total)}", "")
        Time.sleep(2000)

        // Iniciar impressão, mostrando estado
        showCentered("Printing...", "Please wait...")
        Time.sleep(2000)
        TicketDispenser.activatePrintingTicket(roundTrip, origin.id, dest.id)

        // Aguardar confirmação de impressão (com timeout)
        // Se o utilizador pressionar '#', cancela
        val startTime = System.currentTimeMillis()
        val timeout = 5000L // 5 segundos para impressão
        var prevBottom = "Please wait..."

        while (System.currentTimeMillis() - startTime < timeout) {
            val top = "Printing..."
            val bottom = "Press #=Cancel"
            if (bottom != prevBottom) {
                showCentered(top, bottom)
                prevBottom = bottom
            }
            val key = KBD.waitKey(200)
            if (key == '#') {
                // Utilizador cancelou durante impressão
                showCentered("Cancelled", "Refunded ${"%.2f".format(inserted)}")
                Time.sleep(1500)
                return false
            }
        }

        // Impressão concluída, mostrar instruções para recolher bilhete
        showCentered("Take ticket", "Press any key")
        KBD.waitKey(5000)
        return true
    }
}
