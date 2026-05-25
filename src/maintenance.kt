import isel.leic.utils.Time

object maintenance {
    private const val MAINTENANCE_MASK = 0x80

    fun on(): Boolean{
        return HAL.isBit(MAINTENANCE_MASK)
    }
}

fun main () {
    // Exemplo de uso
    HAL.init()
    Time.sleep(5000)
    if (maintenance.on()) {
        println("Manutenção ativa")
    } else {
        println("Manutenção inativa")
    }
}