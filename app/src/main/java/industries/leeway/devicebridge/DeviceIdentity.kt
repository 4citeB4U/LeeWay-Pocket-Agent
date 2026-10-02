package industries.leeway.devicebridge

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONObject
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.util.UUID

object DeviceIdentity {
    private const val ALIAS="leeway_device_identity"
    private const val PREFS="leeway_device_bridge"
    private const val ID_KEY="device_id"

    fun ensure(context: Context): JSONObject {
        val prefs=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE)
        val id=prefs.getString(ID_KEY,null) ?: ("LDB-"+UUID.randomUUID().toString()).also {
            prefs.edit().putString(ID_KEY,it).apply()
        }

        val store=KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if(!store.containsAlias(ALIAS)) {
            val generator=KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC,"AndroidKeyStore")
            generator.initialize(
                KeyGenParameterSpec.Builder(
                    ALIAS,
                    KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
                ).setDigests(KeyProperties.DIGEST_SHA256)
                 .setAlgorithmParameterSpec(java.security.spec.ECGenParameterSpec("secp256r1"))
                 .build()
            )
            generator.generateKeyPair()
        }
        val cert=store.getCertificate(ALIAS)
        val publicKey=cert.publicKey.encoded
        val fingerprint=MessageDigest.getInstance("SHA-256").digest(publicKey)
            .joinToString(""){"%02x".format(it)}
        return JSONObject().apply {
            put("deviceId",id)
            put("keyAlias",ALIAS)
            put("algorithm",cert.publicKey.algorithm)
            put("publicKeyFingerprintSha256",fingerprint)
            put("publicKeyDerBase64",Base64.encodeToString(publicKey,Base64.NO_WRAP))
            put("authority","ANDROID_KEYSTORE")
        }
    }
}
