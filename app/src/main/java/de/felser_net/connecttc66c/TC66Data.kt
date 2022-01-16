package de.felser_net.connecttc66c

import android.annotation.SuppressLint
import android.util.Log
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

private const val TAG = "TC66Data"

class TC66Data(cipherText: ByteArray) {

    private val aesKey = byteArrayOf(88, 33, -6, 86, 1, -78, -16, 38, -121, -1, 18, 4, 98, 42, 79, -80, -122, -12, 2, 96, -127, 111, -102, 11, -89, -15, 6, 97, -102, -72, 114, -120)
    private var data:ByteArray = aesDecrypt(cipherText, aesKey)

    init {
        val clearText = String(data, Charsets.US_ASCII)
        Log.d(TAG, "device: " + clearText.substring(4, 8))
    }

    fun decode() {
    }

    @SuppressLint("getInstance") // suppress ECB warning, we have to use ECB ;-)
    private fun aesDecrypt(cipherText:ByteArray, key:ByteArray): ByteArray {

        val secretKey = SecretKeySpec(key, "AES")
        val cipher: Cipher = Cipher.getInstance("AES/ECB/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey)
        return cipher.doFinal(cipherText)
    }
}