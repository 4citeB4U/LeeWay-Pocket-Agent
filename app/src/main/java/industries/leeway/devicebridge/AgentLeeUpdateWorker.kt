/*
REGION: LEEWAY.UPDATE.RUNTIME
TAG: LEEWAY.UPDATE.STANDARD.V1.WORKER
WHAT: Periodic background retrieval worker for verified Agent Lee updates.
WHY: Keeps opt-in automatic update retrieval independent of UI sessions.
HOW: WorkManager invokes checkAndStage; it never invokes requestApply.
*/
package industries.leeway.devicebridge

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

class AgentLeeUpdateWorker(
    appContext: Context,
    params: WorkerParameters
) : Worker(appContext, params) {
    override fun doWork(): Result {
        val result = AgentLeeUpdate.checkAndStage(applicationContext, force = false)
        if (result.optBoolean("ok")) return Result.success()

        val error = result.optString("error")
        return if (
            error.startsWith("UPDATE_CHECK_FAILED:") ||
            error.startsWith("UPDATE_METADATA_HTTP_5") ||
            error.startsWith("UPDATE_DOWNLOAD_HTTP_5") ||
            error.startsWith("UPDATE_DOWNLOAD_FAILED:")
        ) {
            Result.retry()
        } else {
            Result.failure()
        }
    }
}
