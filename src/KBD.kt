// Ler teclas. Funções retornam '0' ... '9', 'A' ... 'D', '#', '*' ou NONE.
object KBD {

    const val NONE = 0
    private const val MASK_RXD= 0x80
    private const val MASK_RXCLOCK = 0x80
    private const val MASK_KEY_BITS = 0x1E
    private const val KEYS = "147*2580369#ABCD"
    // Inicia a classe
    fun init() {
        HAL.init()
        HAL.clrBits(MASK_RXCLOCK)
    }



    // Retorna de imediato a tecla premida ou NONE se nao ha tecla premida.
    fun getKey(): Char {

        if (!HAL.isBit(MASK_RXD)) { // deteta início de transmissão
            var keycode = 0

            HAL.setBits(MASK_RXCLOCK) // Consome o start bit
            HAL.clrBits(MASK_RXCLOCK)

            for (i in 0 until 4) {  // ler keycode
                HAL.setBits(MASK_RXCLOCK)
                HAL.clrBits(MASK_RXCLOCK)
                val bit = if (HAL.isBit(MASK_RXD)) 1 else 0
                keycode = keycode or (bit shl i)
            }

            HAL.setBits(MASK_RXCLOCK) // Consome o stop bit
            HAL.clrBits(MASK_RXCLOCK)
            HAL.setBits(MASK_RXCLOCK) // estabiliza sinal rxd para idle (=1)
            HAL.clrBits(MASK_RXCLOCK)

            return KEYS[keycode]
        }
        return NONE.toChar()
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