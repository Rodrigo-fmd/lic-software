// Ler teclas. Funções retornam '0' ... '9', 'A' ... 'D', '#', '*' ou NONE.
object KBD {

    const val NONE = 0
    private const val MASK_RXD= 0x0
    private const val MASK_RXCLOCK = 0x80
    private const val MASK_KEY_BITS = 0x1E
    private const val KEYS = "147*2580369#ABCD"
    private const val DVAL_MASK = 0x10
    private const val KEY_CODE_MASK = 0xF
    private const val ACK_MASK = 0x80
    // Inicia a classe
    fun init() = HAL.clrBits(ACK_MASK) //HAL.setBits(MASK_RXCLOCK)



    // Retorna de imediato a tecla premida ou NONE se nao ha tecla premida.
 /*   fun getKey(): Char {
        var trama = 0

        if (!HAL.isBit(MASK_RXD)) {
            for (i in 0 until 7) {
                HAL.setBits(MASK_RXCLOCK)
                trama = trama or (HAL.readBits(MASK_RXD) shl i)
                HAL.clrBits(MASK_RXCLOCK)
            }
            val keyCode = (trama and MASK_KEY_BITS) shr 1
            return KEYS[keyCode]
        }
        return NONE.toChar()
    }*/

    fun getKey(): Char {
        if (HAL.isBit(DVAL_MASK)) {
            val key = HAL.readBits(KEY_CODE_MASK)
            HAL.setBits(ACK_MASK)
            while (HAL.isBit(DVAL_MASK)) {
                // Espera que a tecla seja solta
            }
            HAL.clrBits(ACK_MASK)
            return KEYS[key]
        } else {
            return NONE.toChar()
        }
    }

    // Retorna a tecla premida, caso ocorra antes do 'timeout' (em milissegundos), ou NONE caso contrario.
    fun waitKey(timeout: Long) : Char{
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < timeout) {
            val key = getKey()
            if (key != NONE.toChar()){
                return key
            }
        }
        return NONE.toChar()
    }
}

fun main() {
    // Exemplo de uso
    KBD.init()
    while (true) {
        val key = KBD.waitKey(10000) // Espera por uma tecla durante 10 segundos
        if (key != KBD.NONE.toChar()) {
            println("Tecla pressionada: $key")
        } else {
            println("Nenhuma tecla pressionada.")
        }
    }
}