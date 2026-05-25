@file:Suppress("unused")

object Tickets {

    fun load(stations: List<Station>): MutableMap<String, Int> {
        val counters = stations.associate { it.name to 0 }.toMutableMap()
        FileAccess.readTickets().forEach { raw ->
            val line = raw.trim()
            val parts = line.split(';')
            if (parts.size >= 3) {
                val sold = parts[1].toInt()
                val name = parts.subList(2, parts.size).joinToString(";")
                 counters[name] = sold
            }
        }
        return counters
    }

    fun save(stations: List<Station>, counters: Map<String, Int>) {
        val lines = stations.sortedBy { it.id }.map { station ->
            "${station.price};${counters[station.name]};${station.name}"
        }
        FileAccess.writeTickets(lines)
    }

}


