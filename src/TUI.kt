import isel.leic.utils.Time

object TUI {

    // remember last shown lines to avoid unnecessary LCD writes
    private var prevTopShown: String = ""
    private var prevBottomShown: String = ""

    private enum class MenuMode { HOME, ID, LIST }

    fun init(){
        KBD.init()
        LCD.init()
    }

    private fun centeredLine(text: String): String {
        if (text.length >= LCD.COLS) return text.take(LCD.COLS)
        val left = (LCD.COLS - text.length) / 2
        val right = LCD.COLS - text.length - left
        return " ".repeat(left) + text + " ".repeat(right)
    }

    fun waitKey(timeout: Long): Char {
        return KBD.waitKey(timeout)
    }

    fun showTop(top: String) {
        if (top == prevTopShown) return
        prevTopShown = top
        LCD.cursor(0, 0)
        LCD.write(centeredLine(top))
    }

    fun showBottom(bottom: String) {
        if (bottom == prevBottomShown) return
        prevBottomShown = bottom
        LCD.cursor(1, 0)
        LCD.write(centeredLine(bottom))
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

    fun buyMenu(stations: List<Station>, currentStationId: Int): Station? {
        val sorted = stations.filter { it.id != currentStationId }.sortedBy { it.id }
        val byId = stations.associateBy { it.id }
        var mode = MenuMode.HOME
        var digits = ""
        var index = 0
        var dirty = true

        while (true) {
            if (dirty) {
                when (mode) {
                    MenuMode.HOME -> showCentered("Buy ticket", "#=ID *=LIST")
                    MenuMode.ID   -> showCentered("Enter station ID", "ID:$digits #=OK *=BK")
                    MenuMode.LIST -> showCentered(sorted[index].name, "#=CONFIRM *=BACK")
                }
                dirty = false
            }

            when (val key = KBD.waitKey(80)) {
                KBD.NONE.toChar() -> {}
                '#' -> when (mode) {
                    MenuMode.HOME -> { mode = MenuMode.ID; digits = ""; dirty = true }
                    MenuMode.ID   -> {
                        val selected = digits.toIntOrNull()?.let { byId[it] }
                        if (selected != null) {
                            if (selected.id == currentStationId) {
                                showCentered("Current station", "Choose another")
                                Time.sleep(1000)
                                digits = ""
                                dirty = true
                            } else return selected
                        }
                        else { showCentered("Unknown ID", "Try again"); Time.sleep(1000); digits = ""; dirty = true }
                    }
                    MenuMode.LIST -> return sorted[index]
                }
                '*' -> when (mode) {
                    MenuMode.HOME -> { mode = MenuMode.LIST; dirty = true }
                    MenuMode.ID   -> { mode = MenuMode.HOME; digits = ""; dirty = true }
                    MenuMode.LIST -> { mode = MenuMode.HOME; dirty = true }
                }
                'A' -> if (mode == MenuMode.LIST && index > 0) { index--; dirty = true }
                'B' -> if (mode == MenuMode.LIST && index < sorted.lastIndex) { index++; dirty = true }
                in '0'..'9' -> if (mode == MenuMode.ID) { digits += key; dirty = true }
                else -> {}
            }
        }
    }

    /**
     * Calcula o preço base (ida) entre origin e dest.
     */
    private fun calculateBasePrice(origin: Station, dest: Station, stations: List<Station>): Double {
        val sorted = stations.sortedBy { it.id }
        val start = sorted.indexOfFirst { it.id == origin.id }
        val end   = sorted.indexOfFirst { it.id == dest.id }

        if (start == end) return 0.0
        return if (start < end) sorted.subList(start + 1, end + 1).sumOf { it.price }
        else                    sorted.subList(end, start).sumOf { it.price }
    }

    /**
     * Fase de pagamento usando o CoinAcceptor.
     * Lê moedas em loop até ter valor suficiente.
     * '#' cancela, '*' alterna ida/ida-volta.
     */
    fun buyPhase(
        origin: Station,
        dest: Station,
        stations: List<Station>,
        testMode: Boolean = false,
        coinCounters: MutableMap<Float, Int>? = null,
        ticketCounters: MutableMap<String, Int>? = null
    ): Boolean {
        val basePrice = calculateBasePrice(origin, dest, stations)

        var roundTrip = false
        var total     = basePrice
        var inserted  = 0.0
        // conta quantas moedas de cada valor foram inseridas nesta compra
        val insertedCoins = mutableMapOf<Float, Int>()

        if (testMode) {
            return printingPhase(origin, dest, roundTrip, total, total)
        }

        var prevTop    = ""
        var prevBottom = ""

        while (inserted < total) {
            val top    = if (roundTrip) "RoundTrip ${"%.2f".format(total)}" else "Single ${"%.2f".format(total)}"
            val bottom = "In:${"%.2f".format(inserted)} #=Cancel"

            if (top != prevTop || bottom != prevBottom) {
                showCentered(top, bottom)
                prevTop    = top
                prevBottom = bottom
            }

            // Teclas: só para cancelar ou alternar tipo de bilhete
            when (KBD.waitKey(100)) {
                '#' -> {
                    CoinAcceptor.ejectCoins()
                    showCentered("Cancelled", "Refund ${"%.2f".format(inserted)}")
                    Time.sleep(1500)
                    return false
                }
                '*' -> {
                    roundTrip = !roundTrip
                    total     = basePrice * (if (roundTrip) 2.0 else 1.0)
                    prevTop   = "" // forçar atualização do LCD
                }
                else -> { /* ignorar outras teclas */}
            }

            // Ler moeda do CoinAcceptor — retorna o valor em euros
            val coinValue = CoinAcceptor.addCoins()
            if (coinValue > 0) {
                inserted += coinValue
                // incrementar o contador desta
                insertedCoins[coinValue] = (insertedCoins[coinValue] ?: 0) + 1
                prevTop = "" // forçar atualização do LCD com novo valor
            }
        }

        // Valor suficiente — avançar para impressão
        val ok = printingPhase(origin, dest, roundTrip, total, inserted)
        if (ok) {
            // Somar as moedas desta compra aos contadores globais
            CoinAcceptor.collectCoins()
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

    /**
     * Fase de impressão: mostra informações do bilhete, inicia o mecanismo de impressão,
     * aguarda confirmação (com timeout) e deixa o utilizador recolher o bilhete.
     * Permite cancelar a operação premindo '#'; nesse caso devolve as moedas.
     */
    private fun printingPhase(origin: Station, dest: Station, roundTrip: Boolean, total: Double, inserted: Double): Boolean {
        val ticketType = if (roundTrip) "RoundTrip" else "Single"

        // Mostrar tipo de bilhete e origem
        showCentered(ticketType, origin.name)
        Time.sleep(2000)
        // Mostrar destino
        showCentered("Destination", dest.name)
        Time.sleep(2000)
        // Mostrar preço
        showCentered("Total:${"%.2f".format(total)}", "")
        Time.sleep(2000)

        // Se o utilizador pressionar '#', cancela
        showCentered("Printing...", "Press #=Cancel")

        val key = KBD.waitKey(4000)
        if (key == '#') {
            // Utilizador cancelou durante impressão
            CoinAcceptor.ejectCoins()
            showCentered("Cancelled", "refund ${"%.2f".format(inserted)}")
            Time.sleep(2000)
            return false
        }

        showCentered("Collect", "Ticket")
        TicketDispenser.activatePrintingTicket(roundTrip, origin.id, dest.id)
        LCD.clear()
        showTop("Done")

        return true
    }
}