@file:Suppress("unused")

object CoinDeposit {

    private val coinOrder = listOf(2.0f, 1.0f, 0.5f, 0.2f, 0.1f, 0.05f)

    fun load(): MutableMap<Float, Int> {
        val counters = coinOrder.associateWith { 0 }.toMutableMap()
        FileAccess.readCoins().forEach { raw ->
            val line = raw.trim()
            val parts = line.split(';')
            if (parts.size >= 2) {
                val value = parts[0].toFloat()
                val count = parts[1].toInt()
                counters[value] = count
            }
        }
        return counters
    }

    fun save(counters: Map<Float, Int>) {
        val lines = coinOrder.map { value -> "${value};${counters[value]}" }
        FileAccess.writeCoins(lines)
    }

}


