/*
REGION: LEEWAY.BRAIN.ADAPTER.ANDROID
TAG: BRAIN_BOUND_FILE_METADATA_SENSOR
WHO: Owner-authorized Agent Lee application; WHAT: Observe the Brain-bound private file scope.
WHEN: Startup, local file changes and bounded reconciliation while the existing overlay service runs.
WHERE: Android adapter only; paths resolve from this device's Brain resource map.
WHY: Preserve file state without copying contents, inventing hashes or creating another Brain.
HOW: Shared reconciler, no-follow native scanner, existing SQLite transaction and FileObserver.
LIMIT: External document grants and OS-suspended timing require separate qualification.
LICENSE: MIT
*/
package industries.leeway.pocket

import android.content.Context
import android.os.FileObserver
import industries.leeway.brain.*
import org.json.JSONObject
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

object AndroidBrainIngestion {
    private const val RECONCILE_SECONDS = 60L
    private val scanLock=Any()
    private var executor: ScheduledExecutorService? = null
    private var observer: FileObserver? = null
    private val queued = AtomicBoolean(false)
    @Volatile private var lastState = JSONObject().put("state","NOT_STARTED").toString()
    fun status(): String = lastState

    /** Returns directories to watch. No user file is opened for content, modified or removed. */
    fun reconcilePrivateFiles(context: Context): List<File> = synchronized(scanLock) {
        val identity=AndroidDigitalBrainAdapter.identity(context)
        LeeWayBodyDatabases(context).use { dbs ->
            val store=AndroidDigitalBrainAdapter.SqliteBrainStore(dbs.brain.writableDatabase)
            val binding=store.binding("app-private-files") ?: error("PRIVATE_FILES_RESOURCE_UNBOUND")
            val handle=ResourceHandle(binding.logicalId,identity.deviceId,binding.revision)
            DigitalBrain.resolveResource(store,identity,handle)
            val uri=java.net.URI(binding.resourceUri)
            check(uri.scheme=="file" && uri.rawAuthority.isNullOrEmpty()) { "PRIVATE_FILES_NATIVE_URI_REQUIRED" }
            val rawRoot=File(uri).toPath()
            check(!Files.isSymbolicLink(rawRoot)) { "INGESTION_ROOT_LINK_REJECTED" }
            check(rawRoot.toRealPath()==context.filesDir.toPath().toRealPath()) { "PRIVATE_FILES_GRANT_MISMATCH" }
            val excluded=mutableSetOf<Path>()
            listOf(context.cacheDir,context.noBackupFilesDir,context.getDatabasePath(LeeWayBodyDatabases.BRAIN_NAME).parentFile)
                .filterNotNull().forEach { excluded.add(it.toPath().toAbsolutePath().normalize()) }
            val began=android.os.SystemClock.elapsedRealtime()
            val census=NativeFileCensus.collect(rawRoot,excluded,readRootIdentity={ path -> val stat=android.system.Os.lstat(path.toString());stat.st_dev.toString()+":"+stat.st_ino.toString() })
            val result=BrainIngestion.reconcile(identity,store,FileCensus(UUID.randomUUID().toString(),handle,System.currentTimeMillis(),
                census.entries,census.omissions.isEmpty(),census.omissions))
            lastState=JSONObject().put("state",if(result.enumerationComplete)"LOCAL_METADATA_CENSUS_COMMITTED" else "PARTIAL_CENSUS_COMMITTED_NO_ABSENCE_DELETIONS")
                .put("bodyId",identity.deviceId).put("resourceId",binding.logicalId).put("created",result.created)
                .put("modified",result.modified).put("deleted",result.deleted).put("restored",result.restored)
                .put("unchanged",result.unchanged).put("observedEntries",census.entries.size)
                .put("contentState",result.contentState).put("freshnessState",result.freshnessState)
                .put("formulaState",result.formulaState).put("omissions",org.json.JSONArray(census.omissions))
                .put("observationToCommitMs",android.os.SystemClock.elapsedRealtime()-began)
                .put("renameIdentityPolicy","PATH_KEYED_PROVIDER_REPORTS_CREATE_DELETE; NO_RENAME_INFERENCE").toString()
            census.directories
        }
    }

    /** Reuses the existing foreground service; this is a local sensor, not an automation scheduler. */
    @Synchronized fun start(context: Context) {
        if(executor!=null)return
        val app=context.applicationContext
        val worker=Executors.newSingleThreadScheduledExecutor { task -> Thread(task,"leeway-brain-file-observer").apply { isDaemon=true } }
        executor=worker
        fun runScan() {
            try {
                val dirs=reconcilePrivateFiles(app)
                synchronized(this) {
                    if(executor!==worker)return
                    observer?.stopWatching()
                    val mask=FileObserver.CREATE or FileObserver.DELETE or FileObserver.MODIFY or FileObserver.CLOSE_WRITE or FileObserver.MOVED_FROM or FileObserver.MOVED_TO or FileObserver.DELETE_SELF or FileObserver.MOVE_SELF
                    observer=object:FileObserver(dirs,mask) {
                        override fun onEvent(event:Int,path:String?) {
                            if(queued.compareAndSet(false,true)) {
                                try { worker.schedule({queued.set(false);runScan()},250,TimeUnit.MILLISECONDS) }
                                catch(_:java.util.concurrent.RejectedExecutionException){queued.set(false)}
                            }
                        }
                    }.also { it.startWatching() }
                }
            } catch(error:Exception) {
                lastState=JSONObject().put("state","INGESTION_BLOCKED").put("reason",error.javaClass.simpleName)
                    .put("message",error.message?.take(200)).put("dataCleared",false).toString()
            }
        }
        worker.scheduleWithFixedDelay({runScan()},0,RECONCILE_SECONDS,TimeUnit.SECONDS)
    }
    @Synchronized fun stop() {observer?.stopWatching();observer=null;executor?.shutdownNow();executor=null;queued.set(false)}
}
