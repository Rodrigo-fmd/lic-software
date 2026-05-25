// Envia tramas para os diferentes modulos Serial Receiver.
object SerialEmitter{

    const val SELECT_LCD = 0x04
    const val SELECT_TICKET = 0x08
    const val DATA_SIZE = 10
    const val CLOCK_MASK = 0x02
    const val SDX_MASK = 0x01

    enum class Peripheral{LCD, TICKET}

    // Inicia a classe
    fun init( ){
        HAL.init()
        HAL.setBits(SELECT_LCD)
    }

    // Envia uma trama para o Serial Receiver
    // identificado o periferico de destino em ’addr’,
    // os bits de dados em ’data’
    // e em ’size’ o numero de bits a enviar.
    fun send (addr : Peripheral ,data : Int) {
         when(addr) {
            Peripheral.LCD -> {
                HAL.clrBits(SELECT_LCD)
                HAL.setBits(SELECT_TICKET)
            }
            Peripheral.TICKET -> {
                HAL.clrBits(SELECT_TICKET)
                HAL.setBits(SELECT_LCD)
            }
        }

        HAL.clrBits(CLOCK_MASK)

        for(i in 0 until DATA_SIZE){
            val bit = (data shr i) and 0x01

            HAL.writeBits(SDX_MASK, bit)

            HAL.setBits(CLOCK_MASK)
            HAL.clrBits(CLOCK_MASK)
        }

        if (addr == Peripheral.LCD )HAL.setBits(SELECT_LCD)
        else HAL.setBits(SELECT_TICKET)
    }

    // Retorna informacao se o periferico esta ocupado
    fun isBusy(): Boolean {
        return !HAL.isBit(SELECT_LCD)
    }
}
