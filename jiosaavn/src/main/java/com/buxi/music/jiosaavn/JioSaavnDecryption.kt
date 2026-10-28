package com.buxi.music.jiosaavn

import io.ktor.util.*
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

object JioSaavnDecryption {
    private const val KEY = "38346591"
    private const val ALGORITHM = "DES/ECB/PKCS5Padding"

    fun decryptMediaUrl(encryptedUrl: String): String {
        return try {
            val keySpec = SecretKeySpec(KEY.toByteArray(), "DES")
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.DECRYPT_MODE, keySpec)
            
            val encryptedBytes = encryptedUrl.decodeBase64Bytes()
            val decryptedBytes = cipher.doFinal(encryptedBytes)
            
            String(decryptedBytes)
        } catch (e: Exception) {
            ""
        }
    }

    fun getDownloadLink(decryptedUrl: String, quality: String = "_320"): String {
        return decryptedUrl.replace("_96", quality)
    }
}
