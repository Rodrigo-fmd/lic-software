import isel.leic.utils.Time

object CoinAcceptor {

    private const val MASK_COIN = 0x40
    private const val MASK_ID = 0x20
    private const val ENABLE_ACCEPT = 0x40

    // variável global para guardar estado anterior
    var previousCoinState = false

    fun checkAndAdd(currentAmount: Int): Int {
        var updatedAmount = currentAmount

        val coinDetected = HAL.isBit(MASK_COIN)

        if (coinDetected) {
            // Ativa ENABLE_ACCEPT enquanto o sinal da moeda estiver ativo
            HAL.setBits(ENABLE_ACCEPT)

            // Deteta rising edge do clock: só incrementa uma vez por ciclo
            if (!previousCoinState) {
                Time.sleep(25)
                val coinValue = if (HAL.isBit(MASK_ID)) 4 else 2
                updatedAmount += coinValue
            }
        } else {
            // desativa ENABLE_ACCEPT se não houver moeda
            HAL.clrBits(ENABLE_ACCEPT)
        }

        // atualiza estado anterior
        previousCoinState = coinDetected

        return updatedAmount
    }

}