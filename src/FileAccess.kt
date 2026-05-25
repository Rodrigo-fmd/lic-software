import java.io.File

object FileAccess {
    private const val STATIONS_FILE = "stations.txt"
    private const val COINS_FILE = "coins.txt"
    private const val TICKETS_FILE = "tickets.txt"

    fun readStations(filePath: String = STATIONS_FILE): List<String> {
        val f = File(filePath)
        return if (f.exists()) f.readLines() else emptyList()
    }

    fun writeStations(lines: List<String>, filePath: String = STATIONS_FILE) {
        File(filePath).writeText(lines.joinToString("\n"))
    }

    fun readCoins(filePath: String = COINS_FILE): List<String> {
        val f = File(filePath)
        return if (f.exists()) f.readLines() else emptyList()
    }

    fun writeCoins(lines: List<String>, filePath: String = COINS_FILE) {
        File(filePath).writeText(lines.joinToString("\n"))
    }

    fun readTickets(filePath: String = TICKETS_FILE): List<String> {
        val f = File(filePath)
        return if (f.exists()) f.readLines() else emptyList()
    }

    fun writeTickets(lines: List<String>, filePath: String = TICKETS_FILE) {
        File(filePath).writeText(lines.joinToString("\n"))
    }
}



