import isel.leic.utils.Time

// Escreve no LCD usando a interface a 8 bits.
object LCD {

    // Dimensao do display.
    const val LINES = 2
    const val COLS = 16
    const val RS_MASK = 0x01
    const val CURSOR_POSITION_MASK = 0x80
    const val ENABLE_MASK = 0x200


    // Escreve um byte de comando/dados no LCD em serie
    private fun writeByteSerial(rs: Boolean ,data : Int){
        SerialEmitter.send(SerialEmitter.Peripheral.LCD, ((data shl 1) or if (rs) RS_MASK else 0) or ENABLE_MASK)
        SerialEmitter.send(SerialEmitter.Peripheral.LCD, ((data shl 1) or if (rs) RS_MASK else 0) or (ENABLE_MASK and 0))
    }

    // Escreve um byte de comando /dados no LCD
    private fun writeByte(rs : Boolean ,data : Int){
        writeByteSerial(rs, data)
    }

    // Escreve um comando no LCD
    private fun writeCMD(data : Int) {
        writeByte(false, data)
    }

    // Escreve um dado no LCD
    private fun writeDATA ( data : Int ){
        writeByte(true, data)
    }

    // Envia a sequencia de iniciacao para comunicacaoa 8 bits.
        fun init( ) {
            Time.sleep(16)
            writeCMD(0x30) //set 8 bits
            Time.sleep(5)
            writeCMD(0x30) //set 8 bits
            Time.sleep(1)
            writeCMD(0x30) //set 8 bits
            writeCMD( 0x38) //set 2 lines and 5*8 dots
            writeCMD(0x08) //Display off
            writeCMD( 0x01) //Display clear
            writeCMD(0x06) //Entry mode set
            writeCMD(0x0F) //Display on, cursor on, blink on
        }

    // Escreve um carater na posicao corrente.
    fun write(c : Char) {
         writeDATA(c.code)
    }

    // Escreve uma string na posicao corrente.
    fun write(text : String){
        for (c in text)
            write(c)
    }

    // Envia comando para posicionar cursor (’line’ : 0 . . LINES - 1 , ’column’ : 0 . . COLS - 1)
    fun cursor(line: Int ,column : Int){
        if(line == 0) writeCMD(column or CURSOR_POSITION_MASK)
        else writeCMD((0x40 + column) or CURSOR_POSITION_MASK)
    }

    // Envia comando para limpar o ecra e posicionar o cursor em (0 ,0)
    fun clear( ){
        writeCMD(0x01)
    }
}

fun main() {
    HAL.init()
    SerialEmitter.init()
    KBD.init()
    LCD.init()
    LCD.clear()
    LCD.cursor(0, 0)
    LCD.write("Hello, World!")
}