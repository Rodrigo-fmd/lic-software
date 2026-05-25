data class Station(val id: Int, val name: String, val price: Double)

object Stations {

    // Assume the stations file is well-formed (ID, name..., price) and parse directly.
    fun load(filePath: String = "stations.txt"): List<Station> =
        FileAccess.readStations(filePath)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { line ->
                val tokens = line.split(Regex("\\s+"))
                val id = tokens.first().toInt()
                val price = tokens.last().toDouble()
                val name = tokens.subList(1, tokens.size - 1).joinToString(" ")
                Station(id, name, price)
            }
}



