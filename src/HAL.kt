import isel.leic.UsbPort;

object HAL {

    private var output = 0b00000000

    // Inicia o objeto
    fun init() = UsbPort.write(output)

    // Retorna 'true' se o bit definido pela mask esta como valor lógico '1' no UsbPort
    fun isBit(mask : Int) : Boolean = mask.and(UsbPort.read()) == mask

    // Retorna os valores dos bits representados por mask presentes no UsbPort
    fun readBits(mask : Int) : Int = mask.and(UsbPort.read())

    // Escreve nos bits representados por mask os valores dos bits correspondentes em value
    fun writeBits(mask : Int, value: Int){
        output = output.and(mask.inv()).or(value.and(mask))
        UsbPort.write(output)
    }

    // Coloca os bits representados por mask no valor lógico '1'
    fun setBits(mask : Int){
        output = mask.or(output)
        UsbPort.write(output)
    }

    // Coloca os bits representados por mask no valor lógico '0'
    fun clrBits(mask : Int){
        output = (mask.inv()).and(output)
        UsbPort.write(output)
    }
}

fun main() {
    // Exemplo de uso
    HAL.init()
    HAL.setBits(0b00000001) // Define o bit 0
    HAL.clrBits(0b00000001) // Limpa o bit 0
    HAL.writeBits(0b00010001, 0b00000001) // Escreve o bit 0 com valor 1
    HAL.isBit(0b00000001) // Verifica se o bit 0 está definido
    HAL.readBits(0b00001111) // Lê os bits 0 a 3
}