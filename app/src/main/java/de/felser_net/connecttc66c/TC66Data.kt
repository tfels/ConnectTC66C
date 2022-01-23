package de.felser_net.connecttc66c

import android.annotation.SuppressLint
import android.util.Log
import java.nio.ByteBuffer
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

private const val TAG = "TC66Data"

// see https://www.modbustools.com/modbus_crc16.htm
private val CRC_TABLE = intArrayOf(
     0X0000, 0XC0C1, 0XC181, 0X0140, 0XC301, 0X03C0, 0X0280, 0XC241,
     0XC601, 0X06C0, 0X0780, 0XC741, 0X0500, 0XC5C1, 0XC481, 0X0440,
     0XCC01, 0X0CC0, 0X0D80, 0XCD41, 0X0F00, 0XCFC1, 0XCE81, 0X0E40,
     0X0A00, 0XCAC1, 0XCB81, 0X0B40, 0XC901, 0X09C0, 0X0880, 0XC841,
     0XD801, 0X18C0, 0X1980, 0XD941, 0X1B00, 0XDBC1, 0XDA81, 0X1A40,
     0X1E00, 0XDEC1, 0XDF81, 0X1F40, 0XDD01, 0X1DC0, 0X1C80, 0XDC41,
     0X1400, 0XD4C1, 0XD581, 0X1540, 0XD701, 0X17C0, 0X1680, 0XD641,
     0XD201, 0X12C0, 0X1380, 0XD341, 0X1100, 0XD1C1, 0XD081, 0X1040,
     0XF001, 0X30C0, 0X3180, 0XF141, 0X3300, 0XF3C1, 0XF281, 0X3240,
     0X3600, 0XF6C1, 0XF781, 0X3740, 0XF501, 0X35C0, 0X3480, 0XF441,
     0X3C00, 0XFCC1, 0XFD81, 0X3D40, 0XFF01, 0X3FC0, 0X3E80, 0XFE41,
     0XFA01, 0X3AC0, 0X3B80, 0XFB41, 0X3900, 0XF9C1, 0XF881, 0X3840,
     0X2800, 0XE8C1, 0XE981, 0X2940, 0XEB01, 0X2BC0, 0X2A80, 0XEA41,
     0XEE01, 0X2EC0, 0X2F80, 0XEF41, 0X2D00, 0XEDC1, 0XEC81, 0X2C40,
     0XE401, 0X24C0, 0X2580, 0XE541, 0X2700, 0XE7C1, 0XE681, 0X2640,
     0X2200, 0XE2C1, 0XE381, 0X2340, 0XE101, 0X21C0, 0X2080, 0XE041,
     0XA001, 0X60C0, 0X6180, 0XA141, 0X6300, 0XA3C1, 0XA281, 0X6240,
     0X6600, 0XA6C1, 0XA781, 0X6740, 0XA501, 0X65C0, 0X6480, 0XA441,
     0X6C00, 0XACC1, 0XAD81, 0X6D40, 0XAF01, 0X6FC0, 0X6E80, 0XAE41,
     0XAA01, 0X6AC0, 0X6B80, 0XAB41, 0X6900, 0XA9C1, 0XA881, 0X6840,
     0X7800, 0XB8C1, 0XB981, 0X7940, 0XBB01, 0X7BC0, 0X7A80, 0XBA41,
     0XBE01, 0X7EC0, 0X7F80, 0XBF41, 0X7D00, 0XBDC1, 0XBC81, 0X7C40,
     0XB401, 0X74C0, 0X7580, 0XB541, 0X7700, 0XB7C1, 0XB681, 0X7640,
     0X7200, 0XB2C1, 0XB381, 0X7340, 0XB101, 0X71C0, 0X7080, 0XB041,
     0X5000, 0X90C1, 0X9181, 0X5140, 0X9301, 0X53C0, 0X5280, 0X9241,
     0X9601, 0X56C0, 0X5780, 0X9741, 0X5500, 0X95C1, 0X9481, 0X5440,
     0X9C01, 0X5CC0, 0X5D80, 0X9D41, 0X5F00, 0X9FC1, 0X9E81, 0X5E40,
     0X5A00, 0X9AC1, 0X9B81, 0X5B40, 0X9901, 0X59C0, 0X5880, 0X9841,
     0X8801, 0X48C0, 0X4980, 0X8941, 0X4B00, 0X8BC1, 0X8A81, 0X4A40,
     0X4E00, 0X8EC1, 0X8F81, 0X4F40, 0X8D01, 0X4DC0, 0X4C80, 0X8C41,
     0X4400, 0X84C1, 0X8581, 0X4540, 0X8701, 0X47C0, 0X4680, 0X8641,
     0X8201, 0X42C0, 0X4380, 0X8341, 0X4100, 0X81C1, 0X8081, 0X4040
)

class TC66Data(cipherText: ByteArray) {

    private val aesKey = byteArrayOf(88, 33, -6, 86, 1, -78, -16, 38, -121, -1, 18, 4, 98, 42, 79, -80, -122, -12, 2, 96, -127, 111, -102, 11, -89, -15, 6, 97, -102, -72, 114, -120)
    private var data:ByteArray = aesDecrypt(cipherText, aesKey)
    private var dataIdx = 0

    // pac1
    var productName = ""
    var version = ""
    var serialNumber = 0
    var unknown_1_16 = 0
    var unknown_1_20 = 0
    var unknown_1_24 = 0
    var unknown_1_28 = 0
    var unknown_1_32 = 0
    var unknown_1_36 = 0
    var unknown_1_40 = 0
    var numRuns = 0
    var voltage = 0.0
    var current = 0.0
    var power = 0.0
    // pac2
    var resistance = 0.0
    var group0_charge = 0
    var group0_energy = 0
    var group1_charge = 0
    var group1_energy = 0
    var temperature_sign = 0
    var temperature = 0
    var d_plus_voltage = 0.0
    var d_minus_voltage = 0.0
    var unknown_2_40 = 0
    var unknown_2_44 = 0
    var unknown_2_48 = 0
    var unknown_2_52 = 0
    var unknown_2_56 = 0

    fun decode(): Boolean {
        if(data.size != 192) {
            Log.e(TAG, "data length error (${data.size} != 192)")
            return false
        }

        if(getNextString4() != "pac1") {
            Log.e(TAG, "Identifier pac1 not found")
            return false
        }

        productName = getNextString4()
        version = getNextString4()
        serialNumber = getNextInt32()
        unknown_1_16 = getNextInt32()
        unknown_1_20 = getNextInt32()
        unknown_1_24 = getNextInt32()
        unknown_1_28 = getNextInt32()
        unknown_1_32 = getNextInt32()
        unknown_1_36 = getNextInt32()
        unknown_1_40 = getNextInt32()
        numRuns = getNextInt32()
        voltage = getNextInt32()/10000.0
        current = getNextInt32()/100000.0
        power = getNextInt32()/10000.0

        var crc16 = getNextInt32() // CRC-16/MODBUS zero-extended to fit a 32-bit field.
        var calculatedCRC = calcCRC16Modbus(data.sliceArray(0..59))
        if(crc16 != calculatedCRC) {
            Log.e(TAG, "CRC error in pac1")
            return false
        }

        if(getNextString4() != "pac2") {
            Log.e(TAG, "Identifier pac2 not found")
            return false
        }

        resistance = getNextInt32()/10.0
        group0_charge = getNextInt32()
        group0_energy = getNextInt32()
        group1_charge = getNextInt32()
        group1_energy = getNextInt32()
        temperature_sign = getNextInt32() // (1 for negative)
        temperature = getNextInt32() // (Celsius or Fahrenheit)
        d_plus_voltage = getNextInt32()/100.0 //(multiply by 1e-2 for Volt)
        d_minus_voltage = getNextInt32()/100.0 //(multiply by 1e-2 for Volt)
        unknown_2_40 = getNextInt32()
        unknown_2_44 = getNextInt32()
        unknown_2_48 = getNextInt32()
        unknown_2_52 = getNextInt32()
        unknown_2_56 = getNextInt32()

        crc16 = getNextInt32() // CRC-16/MODBUS zero-extended to fit a 32-bit field.
        calculatedCRC = calcCRC16Modbus(data.sliceArray(64+0..64+59))
        if(crc16 != calculatedCRC) {
            Log.e(TAG, "CRC error in pac2")
            return false
        }
        Log.d(TAG, toString())
        return true
    }


    override fun toString(): String {
        return "TC66Data(\n" +
                "    productName = $productName\n" +
                "    version     = $version\n" +
                "    serialNumber= $serialNumber\n" +
                "    numRuns     = $numRuns\n" +
                "    voltage     = $voltage V\n" +
                "    current     = $current A\n" +
                "    power       = $power W\n" +

                "    resistance    = $resistance Ω\n" +
                "    group0_charge = $group0_charge mAh\n" +
                "    group0_energy = $group0_energy mWh\n" +
                "    group1_charge = $group1_charge mAh\n" +
                "    group1_energy = $group1_energy mWh\n" +
                "    temperature   = " + (if(temperature_sign==0) "+" else "-") + " $temperature °C/°F\n" +
                "    D+            = $d_plus_voltage V\n" +
                "    D-            = $d_minus_voltage V\n" +
        ")"
    }

    private fun getNextString4() : String {
        val slicedData = data.sliceArray(dataIdx..dataIdx+3)
        dataIdx += 4
        return String(slicedData, Charsets.US_ASCII)
    }

    private fun getNextInt32() : Int {
        val slicedBytes = data.sliceArray(dataIdx..dataIdx+3)
        dataIdx += 4
        slicedBytes.reverse()
        return ByteBuffer.wrap(slicedBytes).int
    }

    private fun calcCRC16Modbus(data:ByteArray): Int {
        // see https://www.modbustools.com/modbus_crc16.htm

        var  crcWord = 0xFFFF

        for (byte in data) {
            val temp = (byte.toInt() xor crcWord) and 0xff
            crcWord = crcWord shr 8
            crcWord = crcWord xor CRC_TABLE[temp]
        }
        return crcWord
    }


    @SuppressLint("getInstance") // suppress ECB warning, we have to use ECB ;-)
    private fun aesDecrypt(cipherText:ByteArray, key:ByteArray): ByteArray {

        val secretKey = SecretKeySpec(key, "AES")
        val cipher: Cipher = Cipher.getInstance("AES/ECB/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey)
        return cipher.doFinal(cipherText)
    }
}