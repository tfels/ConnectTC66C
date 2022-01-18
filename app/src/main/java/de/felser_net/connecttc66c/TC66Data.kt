package de.felser_net.connecttc66c

import android.annotation.SuppressLint
import android.util.Log
import java.nio.ByteBuffer
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

private const val TAG = "TC66Data"

class TC66Data(cipherText: ByteArray) {

    private val aesKey = byteArrayOf(88, 33, -6, 86, 1, -78, -16, 38, -121, -1, 18, 4, 98, 42, 79, -80, -122, -12, 2, 96, -127, 111, -102, 11, -89, -15, 6, 97, -102, -72, 114, -120)
    private var data:ByteArray = aesDecrypt(cipherText, aesKey)
    private var dataIdx = 0

    companion object {
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
    }

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

    @SuppressLint("getInstance") // suppress ECB warning, we have to use ECB ;-)
    private fun aesDecrypt(cipherText:ByteArray, key:ByteArray): ByteArray {

        val secretKey = SecretKeySpec(key, "AES")
        val cipher: Cipher = Cipher.getInstance("AES/ECB/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey)
        return cipher.doFinal(cipherText)
    }
}