import isel.leic.utils.Time

object CoinAcceptor {

    private const val MASK_COIN_ID = 0xE
    private const val MASK_COIN = 0x10
    private const val MASK_ACCEPT = 0x10
    private const val MASK_COLLECT = 0x20
    private const val MASK_EJECT = 0x40

    // coin
    fun addCoins(): Float {
        if(HAL.isBit(MASK_COIN)){
            if(HAL.readBits(MASK_COIN_ID) in 0..5){ // verifica se o valor da moeda é válido
                HAL.setBits(MASK_ACCEPT) // moeda aceite com valor correto

                val coinValue = HAL.readBits(MASK_COIN_ID)
                when(coinValue){ // moeda será adicionada ao moedeiro
                    0 -> return 0.05f
                    1 -> return 0.1f
                    2 -> return 0.20f
                    3 -> return 0.50f
                    4 -> return 1f
                    5 -> return 2f
                }
            }
        }
        return -1f // valor caso algum erro aconteça
    }

    fun ejectCoins(currentAmount: Float): Float {
        val start = System.currentTimeMillis()
        val timeout = 2000L // 2 segundos

        // Enquanto EJECT está ativo, verificamos se dura 2 segundos
        while (HAL.isBit(MASK_EJECT)) {
            val elapsed = System.currentTimeMillis() - start

            if (elapsed >= timeout) {
                // ejetar moedas se EJECT estiver ligado durante 2 segundos
                return 0f
            }
        }
        return currentAmount // EJECT não foi realizado
    }

    fun collectCoins(): Boolean{
        val start = System.currentTimeMillis()
        val timeout = 2000L // 2 segundos

        // Enquanto COLLECT está ativo, verificamos se dura 2 segundos
        while (HAL.isBit(MASK_COLLECT)) {
            val elapsed = System.currentTimeMillis() - start

            if (elapsed >= timeout) {
                // coletar moedas se COLLECT estiver ligado durante 2 segundos
                return true
            }
        }
        return false // COLLECT não foi realizado
    }

}