/*
REGION: LeeWay Android platform adapter
TAG: LEEWAY-DEVICE-OPERATOR-ANDROID-MEDIA
WHO: Owner-authorized LeeWay Device Bridge
WHAT: Inspect shared image/video metadata and open owner-confirmed deletion requests.
WHEN: Governed media scan/delete capabilities are requested.
WHERE: Native Android runtime inside canonical LEEWAY-DEVICE-BRIDGE.
WHY: Enable evidence-first media cleanup without blind destructive deletion.
HOW: MediaStore query -> candidate classification -> Android delete confirmation -> rescan/receipt.
LICENSE: MIT
*/
package industries.leeway.devicebridge

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import org.json.JSONArray
import org.json.JSONObject

object MediaOperator {
    fun scan(context: Context, maxItems: Int = 500): JSONObject {
        val limit = maxItems.coerceIn(1, 2000)
        val images = scanCollection(context, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, false, limit)
        val videos = scanCollection(context, MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true, limit)
        return JSONObject().apply {
            put("ok", true)
            put("images", images)
            put("videos", videos)
            put("classificationRule", "zeroBytes=EMPTY_CANDIDATE; zeroDurationVideo=SUSPICIOUS_CANDIDATE; neither classification authorizes deletion")
        }
    }

    private fun scanCollection(
        context: Context,
        collection: Uri,
        video: Boolean,
        limit: Int
    ): JSONObject {
        val projection = mutableListOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.DATE_MODIFIED
        )
        if (Build.VERSION.SDK_INT >= 29) projection += MediaStore.MediaColumns.RELATIVE_PATH
        if (video) projection += MediaStore.Video.VideoColumns.DURATION

        val items = JSONArray()
        var zeroBytes = 0
        var zeroDuration = 0
        context.contentResolver.query(
            collection,
            projection.toTypedArray(),
            null,
            null,
            "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
            val pathCol = if (Build.VERSION.SDK_INT >= 29) cursor.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH) else -1
            val durationCol = if (video) cursor.getColumnIndex(MediaStore.Video.VideoColumns.DURATION) else -1

            while (cursor.moveToNext() && items.length() < limit) {
                val id = cursor.getLong(idCol)
                val size = cursor.getLong(sizeCol)
                val duration = if (durationCol >= 0 && !cursor.isNull(durationCol)) cursor.getLong(durationCol) else null
                val uri = Uri.withAppendedPath(collection, id.toString())
                val empty = size == 0L
                val durationCandidate = video && duration != null && duration == 0L
                if (empty) zeroBytes += 1
                if (durationCandidate) zeroDuration += 1
                items.put(JSONObject().apply {
                    put("uri", uri.toString())
                    put("displayName", cursor.getString(nameCol))
                    put("sizeBytes", size)
                    put("mimeType", cursor.getString(mimeCol))
                    put("dateModifiedEpochSec", cursor.getLong(dateCol))
                    if (pathCol >= 0) put("relativePath", cursor.getString(pathCol))
                    if (duration != null) put("durationMs", duration)
                    put("zeroBytes", empty)
                    put("zeroDurationCandidate", durationCandidate)
                })
            }
        }
        return JSONObject().apply {
            put("countReturned", items.length())
            put("zeroByteCandidates", zeroBytes)
            put("zeroDurationCandidates", zeroDuration)
            put("items", items)
        }
    }

    fun requestDelete(context: Context, uriStrings: List<String>): JSONObject {
        val uris = uriStrings.mapNotNull { raw ->
            runCatching { Uri.parse(raw) }.getOrNull()
                ?.takeIf { it.scheme == "content" && it.authority == "media" }
        }.distinct()
        if (uris.isEmpty()) return blocked("NO_VALID_MEDIA_URIS")
        if (uris.size > 250) return blocked("DELETE_BATCH_TOO_LARGE")
        if (Build.VERSION.SDK_INT < 30) return blocked("OWNER_CONFIRMED_DELETE_REQUIRES_ANDROID_11_PLUS")

        val request = MediaStore.createDeleteRequest(context.contentResolver, uris)
        request.send()
        ReceiptStore.record(
            context,
            "device.media.delete.request",
            "PASS",
            "Android owner-confirmation opened count=${uris.size}; deletion not yet verified"
        )
        return JSONObject().apply {
            put("ok", true)
            put("state", "OWNER_DELETE_CONFIRMATION_OPENED")
            put("requestedCount", uris.size)
            put("deletedCount", JSONObject.NULL)
            put("verified", false)
        }
    }

    private fun blocked(reason: String) =
        JSONObject().put("ok", false).put("error", reason)
}
