/*
REGION: LEEWAY.DEVICES.IDENTITY
TAG: INTERNAL_CANONICAL_DEVICE_IDENTITY_ADAPTER
WHO: Creator-authorized LeeWay; WHAT: Internalize the existing Device Bridge identity mechanism.
WHEN: Native bootstrap; WHERE: Android adapter only; WHY: Never ship a customer's fixed device ID.
HOW: Reuse AndroidKeyStore EC identity and installation UUID; reject incomplete identity state.
LINEAGE: LEEWAY-DEVICE-BRIDGE DeviceIdentity.kt blob 58827e5f513fb345d42fcc49557437aa5dc6faae.
LICENSE: MIT
*/
package industries.leeway.pocket.devices

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import industries.leeway.brain.BodyIdentity
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.spec.ECGenParameterSpec
import java.util.UUID

object DeviceIdentity {
    private const val ALIAS="leeway_device_identity"
    private const val PREFS="leeway_device_bridge"
    private const val ID_KEY="device_id"

    /** Read the existing canonical identity without creating a key or persisting preferences. */
    @Synchronized fun readExisting(context: Context): BodyIdentity {
        val id = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(ID_KEY, null)
            ?: error("DEVICE_IDENTITY_NOT_INITIALIZED")
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        check(store.containsAlias(ALIAS)) { "DEVICE_IDENTITY_INCOMPLETE_RECOVERY_REQUIRED" }
        val cert = store.getCertificate(ALIAS) ?: error("DEVICE_IDENTITY_KEY_UNAVAILABLE")
        val fingerprint = MessageDigest.getInstance("SHA-256").digest(cert.publicKey.encoded)
            .joinToString("") { b -> "%02x".format(b) }
        return BodyIdentity(id, fingerprint)
    }

    @Synchronized fun ensure(context: Context): BodyIdentity {
        val prefs=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE)
        var id=prefs.getString(ID_KEY,null)
        val store=KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val keyExists=store.containsAlias(ALIAS)
        check((id != null) == keyExists) { "DEVICE_IDENTITY_INCOMPLETE_RECOVERY_REQUIRED" }
        if(id == null) {
            val generator=KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC,"AndroidKeyStore")
            generator.initialize(KeyGenParameterSpec.Builder(ALIAS,KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY)
                .setDigests(KeyProperties.DIGEST_SHA256).setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1")).build())
            generator.generateKeyPair()
            id="LDB-"+UUID.randomUUID().toString()
            check(prefs.edit().putString(ID_KEY,id).commit()) { "DEVICE_IDENTITY_PERSISTENCE_FAILED" }
        }
        val cert=store.getCertificate(ALIAS) ?: error("DEVICE_IDENTITY_KEY_UNAVAILABLE")
        val fingerprint=MessageDigest.getInstance("SHA-256").digest(cert.publicKey.encoded).joinToString(""){b->"%02x".format(b)}
        return BodyIdentity(id,fingerprint)
    }
}