@file:Suppress("unused")

object CoinDeposit {

    private val coinOrder = listOf(1.0, 0.5, 0.2, 0.1)

    fun load(): MutableMap<Double, Int> {
        val counters = coinOrder.associateWith { 0 }.toMutableMap()
        FileAccess.readCoins().forEach { raw ->
            val line = raw.trim()
            val parts = line.split(';')
            if (parts.size >= 2) {
                val value = parts[0].toDouble()
                val count = parts[1].toInt()
                counters[value] = count
            }
        }
        return counters
    }

    fun save(counters: Map<Double, Int>) {
        val lines = coinOrder.map { value -> "${value};${counters[value]}" }
        FileAccess.writeCoins(lines)
    }

}


