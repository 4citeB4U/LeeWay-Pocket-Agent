<#
REGION: LEEWAY.BRAIN.INGESTION.REPAIR
TAG: CONNECT_PORTABLE_INGESTION_TO_EXISTING_BRAIN_STORE
WHO: Creator-authorized Agent Lee.
WHAT: Connect metadata reconciliation to existing Brain SQLite and existing overlay lifecycle.
WHEN: After portable bootstrap repair; WHERE: verified candidate checkout, never installed automatically.
WHY: Startup-only observations do not keep owner files current.
HOW: Exact base and tracked-blob checks; in-memory replacements; dry run; bounded apply; receipt.
LICENSE: MIT
#>
[CmdletBinding()]
param([switch]$Apply)
Set-StrictMode -Version Latest
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
$head=(& git -C $root rev-parse HEAD|Out-String).Trim()
if((& git -C $root remote get-url origin|Out-String).Trim() -ne 'https://github.com/4citeB4U/LeeWay-Pocket-Agent.git'){throw 'SOURCE_ORIGIN_MISMATCH'}
if($head -ne '2956987a523abb5ab19875fcc092415286829858'){throw 'SOURCE_BASE_CHANGED_REVIEW_REQUIRED'}
$prefix='app/src/main/java/industries/leeway/pocket/'
$targets=@('AndroidDigitalBrainAdapter.kt','LeeWayBodyDatabases.kt','MainActivity.kt','PocketOverlayService.kt'|ForEach-Object{$prefix+$_})
$changes=[ordered]@{};$before=@()
function Read-Source([string]$p){[IO.File]::ReadAllText((Join-Path $root $p)).Replace("`r`n","`n")}
function Replace-Once([string]$text,[string]$old,[string]$new){if(([regex]::Matches($text,[regex]::Escape($old))).Count -ne 1){throw ('EXACT_REPLACEMENT_BOUNDARY_FAILED:'+ $old.Substring(0,[Math]::Min(80,$old.Length)))};$text.Replace($old,$new)}
foreach($rel in $targets){
 $full=Join-Path $root $rel
 $expected=(& git -C $root rev-parse ('HEAD:'+ $rel)|Out-String).Trim()
 $actual=(& git -C $root hash-object --path=$rel $full|Out-String).Trim()
 if($expected -ne $actual){throw ('TRACKED_INPUT_CHANGED:'+ $rel)}
 $before+=@{path=$rel;sha256=(Get-FileHash $full -Algorithm SHA256).Hash;blob=$actual}
}
$schema=Read-Source ($prefix+'LeeWayBodyDatabases.kt')
$schema=Replace-Once $schema 'SQLiteOpenHelper(c,BRAIN_NAME,null,2)' 'SQLiteOpenHelper(c,BRAIN_NAME,null,3)'
$schema=Replace-Once $schema '        override fun onUpgrade(db:SQLiteDatabase,o:Int,n:Int){if(o<2)createOwnership(db)}' @'
        private fun createIngestion(db: SQLiteDatabase) {
            fun columns(table: String): Set<String> = db.rawQuery("PRAGMA table_info("+table+")",null).use { c ->
                val names=mutableSetOf<String>();while(c.moveToNext())names.add(c.getString(1));names
            }
            if("source_root" !in columns("nodes")) db.execSQL("ALTER TABLE nodes ADD COLUMN source_root TEXT")
            if("detail_json" !in columns("sync_events")) db.execSQL("ALTER TABLE sync_events ADD COLUMN detail_json TEXT")
            db.execSQL("CREATE INDEX IF NOT EXISTS brain_nodes_source_root ON nodes(source_root)")
            db.execSQL("CREATE INDEX IF NOT EXISTS brain_nodes_parent ON nodes(parent_id)")
        }
        override fun onOpen(db: SQLiteDatabase){super.onOpen(db)}
        override fun onUpgrade(db:SQLiteDatabase,o:Int,n:Int){if(o<2)createOwnership(db);if(o<3)createIngestion(db)}
'@
$schema=Replace-Once $schema '        private fun createOwnership(db:SQLiteDatabase){' @'
        override fun onConfigure(db: SQLiteDatabase){super.onConfigure(db)}
        private fun createOwnership(db:SQLiteDatabase){
'@
# Existing hardware_stats creation is the last statement in onCreate.
$needle='          db.execSQL("CREATE TABLE hardware_stats(id INTEGER PRIMARY KEY AUTOINCREMENT,captured_at INTEGER,cpu_json TEXT,memory_json TEXT,storage_json TEXT,battery_json TEXT,thermal_json TEXT,display_json TEXT,sensors_json TEXT,network_json TEXT)");'
$schema=Replace-Once $schema $needle ($needle+"`n          createIngestion(db)")
$changes[$prefix+'LeeWayBodyDatabases.kt']=$schema
$adapter=Read-Source ($prefix+'AndroidDigitalBrainAdapter.kt')
$adapter=Replace-Once $adapter '    private class SqliteBrainStore(private val db: SQLiteDatabase): BrainStore {' '    internal class SqliteBrainStore(private val db: SQLiteDatabase): BrainIngestionStore {'
$adapter=Replace-Once $adapter '        override fun putBinding(binding: ResourceBinding) {' @'
        private fun encode(entry: FileObservation): JSONObject = JSONObject()
            .put("objectKey",entry.objectKey).put("parentKey",entry.parentKey ?: JSONObject.NULL)
            .put("title",entry.title).put("directory",entry.directory).put("sourceUri",entry.sourceUri)
            .put("metadataVersion",entry.metadataVersion).put("sizeBytes",entry.sizeBytes ?: JSONObject.NULL)
            .put("sourceModifiedAtMs",entry.sourceModifiedAtMs ?: JSONObject.NULL)
            .put("contentState","METADATA_ONLY_CONTENT_NOT_READ")
        private fun decode(value: String): FileObservation {
            val o=JSONObject(value)
            return FileObservation(o.getString("objectKey"),if(o.isNull("parentKey"))null else o.getString("parentKey"),
                o.getString("title"),o.getBoolean("directory"),o.getString("sourceUri"),o.getString("metadataVersion"),
                if(o.isNull("sizeBytes"))null else o.getLong("sizeBytes"),if(o.isNull("sourceModifiedAtMs"))null else o.getLong("sourceModifiedAtMs"))
        }
        override fun resourceFiles(resourceId: String): List<StoredFileObservation> =
            db.rawQuery("SELECT id,metadata_json,updated_at,status FROM nodes WHERE source_root=?",arrayOf(resourceId)).use { c ->
                val rows=mutableListOf<StoredFileObservation>()
                while(c.moveToNext()) rows.add(StoredFileObservation(c.getString(0),resourceId,decode(c.getString(1)),c.getLong(2),c.getString(3)!="tombstoned"))
                rows
            }
        override fun putFile(record: StoredFileObservation,parentNodeId: String) {
            val e=record.observation
            val values=ContentValues().apply {
                put("parent_id",parentNodeId);put("type",if(e.directory)"directory" else "file");put("title",e.title)
                put("source_path",e.sourceUri);put("source_root",record.resourceId);put("status",if(record.active)"observed" else "tombstoned")
                put("metadata_json",encode(e).toString());put("updated_at",record.observedAtMs);putNull("content_hash")
            }
            if(db.update("nodes",values,"id=?",arrayOf(record.nodeId))==0){values.put("id",record.nodeId);values.put("created_at",record.observedAtMs);db.insertOrThrow("nodes",null,values)}
        }
        override fun retainTombstone(record: StoredFileObservation,reason: String,capturedAtMs: Long) {
            val values=ContentValues().apply{put("status","tombstoned");put("updated_at",capturedAtMs)}
            check(db.update("nodes",values,"id=? AND source_root=?",arrayOf(record.nodeId,record.resourceId))==1) { "TOMBSTONE_SOURCE_SCOPE_MISMATCH" }
            val tombstone=ContentValues().apply{put("deleted_at",capturedAtMs);put("reason",reason);put("metadata_json",encode(record.observation).toString())}
            if(db.update("node_tombstones",tombstone,"node_id=?",arrayOf(record.nodeId))==0){tombstone.put("node_id",record.nodeId);db.insertOrThrow("node_tombstones",null,tombstone)}
        }
        override fun clearTombstone(nodeId: String){db.delete("node_tombstones","node_id=?",arrayOf(nodeId))}
        override fun appendFileChange(change: BrainFileChange) {
            val current=change.current;val previous=change.previous
            val detail=JSONObject().put("resourceId",current.resourceId).put("contentState","METADATA_ONLY_CONTENT_NOT_READ")
                .put("metadataVersion",current.observation.metadataVersion).put("previousMetadataVersion",previous?.observation?.metadataVersion ?: JSONObject.NULL)
                .put("sourceModifiedAtMs",current.observation.sourceModifiedAtMs ?: JSONObject.NULL)
                .put("freshnessState","SOURCE_CHANGE_TO_COMMIT_NOT_MEASURED").put("formulaState","NOT_EXECUTED")
            db.insertOrThrow("sync_events",null,ContentValues().apply {
                put("event_id",change.eventId);put("op",change.operation);put("node_id",current.nodeId)
                put("prev_node_id",previous?.nodeId);put("path",current.observation.sourceUri);put("prev_path",previous?.observation?.sourceUri)
                putNull("hash");putNull("prev_hash");put("captured_at",change.capturedAtMs);put("detail_json",detail.toString())
            })
            db.insertOrThrow("provenance",null,ContentValues().apply {
                put("node_id",current.nodeId);put("kind","AUTHORIZED_FILE_METADATA");put("source",current.observation.sourceUri)
                put("description",change.operation+"; scan-event="+change.eventId);put("captured_at",change.capturedAtMs)
            })
        }
        override fun putBinding(binding: ResourceBinding) {
'@
$adapter=Replace-Once $adapter '                .put("deviceBrain",JSONObject(deviceObservation())).put("runtimeBrain",JSONObject(runtimeObservation())).toString()' '                .put("deviceBrain",JSONObject(deviceObservation())).put("runtimeBrain",JSONObject(runtimeObservation())).put("ingestion",JSONObject(AndroidBrainIngestion.status())).toString()'
$changes[$prefix+'AndroidDigitalBrainAdapter.kt']=$adapter
$main=Read-Source ($prefix+'MainActivity.kt')
$main=Replace-Once $main '        AndroidDigitalBrainAdapter.bootstrap(this)' @'
        AndroidDigitalBrainAdapter.bootstrap(this)
        val app=applicationContext
        kotlin.concurrent.thread(name="leeway-brain-initial-census",isDaemon=true){
            runCatching{AndroidBrainIngestion.reconcilePrivateFiles(app)}
                .onFailure{android.util.Log.w("LeeWayBrain","Initial metadata census blocked: "+it.javaClass.simpleName)}
        }
'@
$changes[$prefix+'MainActivity.kt']=$main
$overlay=Read-Source ($prefix+'PocketOverlayService.kt')
$overlay=Replace-Once $overlay 'else startForeground(7141,n);attach()}' 'else startForeground(7141,n);attach();if(isEnabled(this))AndroidBrainIngestion.start(this)}'
$overlay=Replace-Once $overlay 'override fun onDestroy(){detach();super.onDestroy()}' 'override fun onDestroy(){AndroidBrainIngestion.stop();detach();super.onDestroy()}'
$changes[$prefix+'PocketOverlayService.kt']=$overlay
$plan=[ordered]@{schemaVersion='leeway.brain-ingestion-repair.v1';sourceBase=$head;status='DRY_RUN';changedPaths=@($changes.Keys);sourceBefore=$before;phoneModified=$false;formulaExecution='NOT_EXECUTED';scope='APP_PRIVATE_METADATA_SENSOR_NOT_EXTERNAL_FILES_OR_FULL_PRODUCT'}
if(!$Apply){$plan|ConvertTo-Json -Depth 8;return}
$encoding=New-Object Text.UTF8Encoding($false)
foreach($p in $changes.Keys){[IO.File]::WriteAllText((Join-Path $root $p),$changes[$p],$encoding)}
$plan.status='APPLIED_TESTS_REQUIRED'
$plan['sourceAfter']=@(foreach($p in $changes.Keys){@{path=$p;sha256=(Get-FileHash -LiteralPath (Join-Path $root $p) -Algorithm SHA256).Hash}})
$dir=Join-Path $root ('qualification/brain-ingestion-repair-'+[DateTime]::UtcNow.ToString('yyyyMMddTHHmmssfffZ'))
$null=[IO.Directory]::CreateDirectory($dir)
$plan|ConvertTo-Json -Depth 8|Set-Content -LiteralPath (Join-Path $dir 'receipt.json') -Encoding UTF8
$plan|ConvertTo-Json -Depth 8
Write-Output ('REPAIR_RECEIPT='+ (Join-Path $dir 'receipt.json'))
