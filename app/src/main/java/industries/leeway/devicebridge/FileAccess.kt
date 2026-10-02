package industries.leeway.devicebridge

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract

object FileAccess {
    const val REQUEST_OPEN_TREE = 4101

    fun requestDirectory(activity: Activity) {
        val intent=Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PREFIX_URI_PERMISSION)
        }
        activity.startActivityForResult(intent, REQUEST_OPEN_TREE)
    }

    fun persistDirectory(activity: Activity, data: Intent?): Uri? {
        val uri=data?.data ?: return null
        val flags=(data.flags and
            (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION))
        activity.contentResolver.takePersistableUriPermission(uri, flags)
        activity.getSharedPreferences("leeway_device_bridge", Activity.MODE_PRIVATE)
            .edit().putString("authorized_tree_uri", uri.toString()).apply()
        ReceiptStore.record(activity,"device.files.authorize","PASS",DocumentsContract.getTreeDocumentId(uri))
        return uri
    }
}
