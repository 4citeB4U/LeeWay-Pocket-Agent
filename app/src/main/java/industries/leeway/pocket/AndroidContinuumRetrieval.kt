/*
LEEWAY HEADER - DO NOT REMOVE
REGION: LEEWAY.CONTINUUM.POCKET.ADAPTER
TAG: NATIVE_CALLER_OF_EXISTING_CONTINUUM_READ_API
5WH: WHAT = Adapt native retrieval to the viewer's local read API.
WHY = Use one owner/storage boundary for visual inspection and Agent Lee retrieval.
WHO = Existing Pocket owner. WHERE = Existing Android body. WHEN = Explicit Continuum search.
HOW = Fixed local URI, GET only, worker-thread calls, bounded bytes, no HTTP connection or database creation.
AUTHORIZED ROLES: OWNER_READ / RETRIEVE. No admission, ledger or voice authority.
LICENSE: MIT
*/
package industries.leeway.pocket

import android.content.Context
import android.net.Uri
import android.os.Looper
import java.io.ByteArrayOutputStream

internal class AndroidContinuumRetrieval(context: Context) {
    private val application = context.applicationContext
    private val reader = LocalContinuumRetrieval(ContinuumReadApi { route, parameters ->
        check(Looper.myLooper() != Looper.getMainLooper()) { "CONTINUUM_RETRIEVAL_WORKER_REQUIRED" }
        check(route in setOf("status", "search", "content")) { "CONTINUUM_RETRIEVAL_ROUTE_INVALID" }
        val builder = Uri.parse("https://appassets.androidplatform.net/api/continuum/$route").buildUpon()
        parameters.forEach { (key, value) -> builder.appendQueryParameter(key, value) }
        val response = AndroidContinuumReadAdapter.response(application, builder.build(), "GET")
        val bytes = response.data.use { input ->
            check(input != null) { "CONTINUUM_RESPONSE_MISSING" }
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(8_192)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                check(output.size() + count <= LocalContinuumRetrieval.MAX_BYTES) { "CONTINUUM_RESPONSE_LIMIT" }
                output.write(buffer, 0, count)
            }
            output.toByteArray()
        }
        ContinuumReadResult(response.statusCode, response.responseHeaders ?: emptyMap(), bytes)
    })

    fun requireExistingStorage() = reader.requireExistingStorage()
    fun retrieve(query: String, currentEventId: Long) = reader.retrieve(query, currentEventId)
}
