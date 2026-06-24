import isel.leic.utils.Time
import kotlin.random.Random

private val COIN_ORDER = listOf(2.0f, 1.0f, 0.5f, 0.2f, 0.1f, 0.05f)

private data class StatsState(
    val coinCounters: MutableMap<Float, Int>,
    val ticketCounters: MutableMap<String, Int>
)

private fun loadStats(stations: List<Station>): StatsState =
    StatsState(
        CoinDeposit.load(),
        Tickets.load(stations)
    )

private fun saveStats(stations: List<Station>, stats: StatsState) {
    CoinDeposit.save(stats.coinCounters)
    Tickets.save(stations, stats.ticketCounters)
}

private fun showTicketCounters(stations: List<Station>, stats: StatsState) {
    val sorted = stations.sortedBy { it.id }
    if (sorted.isEmpty()) return
    var index = 0
    var dirty = true

    while (maintenance.on()) {
        if (dirty) {
            val station = sorted[index]
            val sold = stats.ticketCounters[station.name] ?: 0
            TUI.showCentered("Tickets -> $sold", station.name)
            dirty = false
        }

        when (TUI.waitKey(200)) {
            'A' -> if (index > 0) { index--; dirty = true }
            'B' -> if (index < sorted.lastIndex) { index++; dirty = true }
            '#', '*' -> return
        }
    }
}

private fun showCoinCounters(stats: StatsState) {
    var index = 0
    var dirty = true

    while (maintenance.on()) {
        if (dirty) {
            val value = COIN_ORDER[index]
            val count = stats.coinCounters[value] ?: 0
            TUI.showCentered("Coins", "${"%.2f".format(value)} -> $count")
            dirty = false
        }

        when (TUI.waitKey(200)) {
            'A' -> if (index > 0) { index--; dirty = true }
            'B' -> if (index < COIN_ORDER.lastIndex) { index++; dirty = true }
            '#', '*' -> return
        }
    }
}

private fun runMaintenance(currentStation: Station, stations: List<Station>, stats: StatsState): Boolean {
    val menuOptions = listOf(
        Pair("#", "Test mode"),
        Pair("A", "Consult Tickets"),
        Pair("B", "Consult Coins"),
        Pair("C", "Reset counters"),
        Pair("D", "Shutdown")
    )
    var menuIndex = 0
    var nextRotateTime = System.currentTimeMillis() + 2000

    while (maintenance.on()) {
        val now = System.currentTimeMillis()
        if (now >= nextRotateTime) {
            menuIndex = (menuIndex + 1) % menuOptions.size
            nextRotateTime = now + 2000
        }

        val (key, desc) = menuOptions[menuIndex]
        TUI.showCentered(desc, "Press $key")

        when (TUI.waitKey(200)) {
            '#' -> {
                val dest = TUI.buyMenu(stations)
                if (dest != null) {
                    TUI.buyPhase(currentStation, dest, stations, testMode = true)
                }
            }
            'A' -> showTicketCounters(stations, stats)
            'B' -> showCoinCounters(stats)
            'C' -> {
                TUI.showCentered("Reset counters", "*=Confirm")
                if (TUI.waitKey(3000) == '*') {
                    for (value in COIN_ORDER) stats.coinCounters[value] = 0
                    for (name in stats.ticketCounters.keys) stats.ticketCounters[name] = 0
                    saveStats(stations, stats)
                    TUI.showCentered("Reset done", "")
                    Time.sleep(1000)
                }
            }
            'D' -> {
                TUI.showCentered("Shutdown", "#=Confirm")
                if (TUI.waitKey(3000) == '#') {
                    saveStats(stations, stats)
                    TUI.showCentered("Saving...", "Bye")
                    Time.sleep(500)
                    return true
                }
            }
            else -> {}
        }
    }
    return false
}

fun main() {
    TUI.init()
    TicketDispenser.init()

    val stationList = Stations.load("stations.txt")
    val currentStation = stationList[Random.nextInt(stationList.size)]
    val stats = loadStats(stationList)
    var swapText = 0

    while (true) {

        if (maintenance.on()) {
            if (runMaintenance(currentStation, stationList, stats)) return
            continue
        }

        if(swapText % 2 == 0) TUI.showCentered("Welcome!", currentStation.name)
        else TUI.showCentered("Press # to buy","the ticket" )

        if (TUI.waitKey(2000) == '#') {
            val dest = TUI.buyMenu(stationList)
            if (dest != null) {
                val ok = TUI.buyPhase(
                    currentStation,
                    dest,
                    stationList,
                    testMode = false,
                    coinCounters = stats.coinCounters,
                    ticketCounters = stats.ticketCounters
                )
                if (ok) saveStats(stationList, stats)
            }
        }
        swapText++
    }
}