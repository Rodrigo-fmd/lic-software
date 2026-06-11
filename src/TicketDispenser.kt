// Controla o mecanismo de dispensa de bilhetes.
object TicketDispenser{

    const val PRT = 0x200
    const val FN = 0x40
    // Inicia a classe, estabelecendo os valores iniciais.
    fun init() {
        SerialEmitter.init()
        //HAL.clrBits()
    }

    // Envia comando para dispensar um bilhete
    fun activatePrintingTicket( roundTrip : Boolean, origin : Int, destination : Int) {

        val roundTripInt = if (roundTrip) 1 else 0
        val originDest = ((destination shl 4) or origin) shl 1
        var trama = (originDest or roundTripInt) or PRT

        SerialEmitter.send(SerialEmitter.Peripheral.TICKET, trama)

        if(HAL.isBit(FN)) {
            trama = trama.xor(PRT)
            SerialEmitter.send(SerialEmitter.Peripheral.TICKET, trama)
        }
    }
}