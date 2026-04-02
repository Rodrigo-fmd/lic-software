fun main(){
    HAL.init()
    SerialEmitter.init()
    KBD.init()
    LCD.init()

    var cols = 0
    while(true) {
        val key = KBD.waitKey(1000)
        if (cols == LCD.COLS) LCD.cursor(1,0)

        if (key != KBD.NONE.toChar()){
            LCD.write(key)
            cols++
        }
        if (cols == LCD.COLS * 2 + 1) {
            LCD.clear()
            cols = 0
        }
    }
}