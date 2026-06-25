import isel.leic.utils.Time

object CoinAcceptor {

    private const val MASK_COIN_ID = 0x7
    private const val MASK_COIN = 0x8
    private const val MASK_ACCEPT = 0x10
    private const val MASK_COLLECT = 0x40
    private const val MASK_EJECT = 0x20

    // coin
    fun addCoins(): Float {
        if(HAL.isBit(MASK_COIN)){
            if(HAL.readBits(MASK_COIN_ID) in 0..5){ // verifica se o valor da moeda é válido
                HAL.setBits(MASK_ACCEPT) // moeda aceite com valor correto
                while (true){ // espera a moeda ser processada
                    if(!HAL.isBit(MASK_COIN)){ // moeda processada
                        HAL.clrBits(MASK_ACCEPT) // limpa o bit de aceite
                        break
                    }
                }

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
        return 0f // valor caso algum erro aconteça
    }

    fun ejectCoins(): Float {
        HAL.setBits(MASK_EJECT) // ativa o processo de ejeção
        Time.sleep(2100) // espera um tempo para o processo de ejeção ocorrer
        HAL.clrBits(MASK_EJECT)
        return 0f // EJECT foi realizado
    }

    fun collectCoins(): Boolean{
        HAL.setBits(MASK_COLLECT) // ativa o processo de recolha
        Time.sleep(2100) // espera um tempo para o processo de recolha ocorrer
        HAL.clrBits(MASK_COLLECT)
        return true // COLLECT foi realizado
    }

}

fun main(){
    var deposit = 0f
    while (true) {
        val coin = CoinAcceptor.addCoins()
        deposit += coin
        if (coin > 0) {
            println("total: $deposit €")
            println(coin)
        }
    }
}