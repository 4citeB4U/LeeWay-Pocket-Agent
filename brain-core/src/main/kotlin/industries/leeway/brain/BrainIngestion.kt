/*
REGION: LEEWAY.BRAIN.INGESTION
TAG: PORTABLE_METADATA_RECONCILIATION
WHO: Creator-authorized Brain engineering; authorized native adapters supply observations.
WHAT: Extend the existing Brain with create/change/removal reconciliation and retained history.
WHEN: Authorized census and change recovery; WHERE: shared core, not a new index service.
WHY: A startup census is not continuous ingestion; partial scans must not delete owner records.
HOW: Reuse Brain ownership/resource handles, nodes, provenance, sync_events and tombstones.
LINEAGE: Recovered Digital Brain live_sync.py metadata operations; Formula placement is not ported or claimed.
LICENSE: MIT
*/
package industries.leeway.brain

private val objectKeyPattern = Regex("[a-f0-9]{64}")
private val scanIdPattern = Regex("[A-Za-z0-9][A-Za-z0-9._-]{0,159}")

data class FileObservation(
    val objectKey: String,
    val parentKey: String?,
    val title: String,
    val directory: Boolean,
    val sourceUri: String,
    val metadataVersion: String,
    val sizeBytes: Long?,
    val sourceModifiedAtMs: Long?
)

data class StoredFileObservation(
    val nodeId: String, val resourceId: String, val observation: FileObservation,
    val observedAtMs: Long, val active: Boolean
)

data class FileCensus(
    val scanId: String, val handle: ResourceHandle, val observedAtMs: Long,
    val entries: List<FileObservation>, val enumerationComplete: Boolean,
    val omissions: List<String> = emptyList()
)

data class BrainFileChange(
    val eventId: String, val operation: String, val previous: StoredFileObservation?,
    val current: StoredFileObservation, val capturedAtMs: Long
)

/** Native implementation commits these operations to the existing Brain tables in one transaction. */
interface BrainIngestionStore : BrainStore {
    fun resourceFiles(resourceId: String): List<StoredFileObservation>
    fun putFile(record: StoredFileObservation, parentNodeId: String)
    fun retainTombstone(record: StoredFileObservation, reason: String, capturedAtMs: Long)
    fun clearTombstone(nodeId: String)
    fun appendFileChange(change: BrainFileChange)
}

data class BrainIngestionResult(
    val resourceId: String, val scanId: String, val created: Int, val modified: Int,
    val renamed: Int, val restored: Int, val deleted: Int, val unchanged: Int,
    val enumerationComplete: Boolean, val omissions: List<String>,
    val contentState: String = "METADATA_ONLY_CONTENT_NOT_READ",
    val freshnessState: String = "SOURCE_CHANGE_TO_COMMIT_NOT_MEASURED",
    val formulaState: String = "NOT_EXECUTED"
)

object BrainIngestion {
    fun resourceNodeId(identity: BodyIdentity, logicalResourceId: String) =
        DigitalBrain.rootId(identity) + ":user:files:resource:" + logicalResourceId

    private fun validate(census: FileCensus) {
        require(scanIdPattern.matches(census.scanId)) { "INGESTION_SCAN_ID_INVALID" }
        require(census.observedAtMs >= 0) { "INGESTION_OBSERVATION_TIME_INVALID" }
        require(!census.enumerationComplete || census.omissions.isEmpty()) { "COMPLETE_CENSUS_CANNOT_CONTAIN_OMISSIONS" }
        val byKey = census.entries.associateBy { it.objectKey }
        require(byKey.size == census.entries.size) { "DUPLICATE_SOURCE_OBJECT_KEY" }
        val resolved = mutableSetOf<String>()
        for (entry in census.entries) {
            require(objectKeyPattern.matches(entry.objectKey)) { "SOURCE_OBJECT_KEY_INVALID" }
            require(entry.title.isNotBlank() && entry.sourceUri.isNotBlank()) { "SOURCE_OBSERVATION_INCOMPLETE" }
            require(entry.metadataVersion.isNotBlank()) { "SOURCE_METADATA_VERSION_REQUIRED" }
            require(entry.sizeBytes == null || entry.sizeBytes >= 0) { "SOURCE_SIZE_INVALID" }
            require(entry.sourceModifiedAtMs == null || entry.sourceModifiedAtMs >= 0) { "SOURCE_TIME_INVALID" }
            var cursor: FileObservation? = entry
            val visiting = mutableSetOf<String>()
            while (cursor != null && cursor.objectKey !in resolved) {
                require(visiting.add(cursor.objectKey)) { "SOURCE_PARENT_CYCLE" }
                val parent = cursor.parentKey
                cursor = if (parent == null) null else {
                    val row = byKey[parent] ?: throw IllegalArgumentException("SOURCE_PARENT_MISSING")
                    require(row.directory) { "SOURCE_PARENT_NOT_DIRECTORY" }
                    row
                }
            }
            resolved.addAll(visiting)
        }
    }

    fun reconcile(identity: BodyIdentity, store: BrainIngestionStore, census: FileCensus): BrainIngestionResult {
        validate(census)
        return store.atomic {
            // Re-read permission and revision at commit, not only before the scan started.
            DigitalBrain.resolveResource(store, identity, census.handle)
            val resourceId = census.handle.logicalId
            val root = resourceNodeId(identity, resourceId)
            val existing = store.resourceFiles(resourceId)
            check(existing.all { it.nodeId.startsWith(root + ":") && it.resourceId == resourceId }) { "INGESTION_STORE_SCOPE_MISMATCH" }
            check(existing.map { it.observation.objectKey }.distinct().size == existing.size) { "INGESTION_STORE_DUPLICATE_IDENTITY" }
            check(existing.none { it.observedAtMs > census.observedAtMs }) { "STALE_CENSUS_REJECTED" }
            val old = existing.associateBy { it.observation.objectKey }
            store.upsertNode(BrainNode(root, DigitalBrain.rootId(identity) + ":user:files", "universe",
                "Authorized files", mapOf("resourceLogicalId" to resourceId, "contentState" to "METADATA_ONLY")), census.observedAtMs)
            var created=0; var modified=0; var renamed=0; var restored=0; var deleted=0; var unchanged=0; var sequence=0
            fun record(op: String, before: StoredFileObservation?, current: StoredFileObservation) {
                sequence += 1
                store.appendFileChange(BrainFileChange(census.scanId + ":" + sequence, op, before, current, census.observedAtMs))
            }
            for (entry in census.entries) {
                val before = old[entry.objectKey]
                val current = StoredFileObservation(root + ":" + entry.objectKey, resourceId, entry, census.observedAtMs, true)
                val op = when {
                    before == null -> { created++; "create" }
                    !before.active -> { restored++; "restore" }
                    before.observation == entry -> { unchanged++; null }
                    before.observation.sourceUri != entry.sourceUri || before.observation.parentKey != entry.parentKey || before.observation.title != entry.title -> { renamed++; "rename-or-move-source-identity-retained" }
                    else -> { modified++; "modify" }
                }
                val parent = if (entry.parentKey == null) root else root + ":" + entry.parentKey
                store.putFile(current, parent)
                if (op != null) { store.clearTombstone(current.nodeId); record(op, before, current) }
            }
            // Absence is meaningful only after a complete, still-authorized enumeration.
            if (census.enumerationComplete) {
                val present = census.entries.mapTo(mutableSetOf()) { it.objectKey }
                for (before in existing) {
                    if (before.active && before.observation.objectKey !in present) {
                        val removed = before.copy(observedAtMs=census.observedAtMs, active=false)
                        store.retainTombstone(removed, "ABSENT_FROM_COMPLETE_AUTHORIZED_CENSUS", census.observedAtMs)
                        record("delete", before, removed); deleted++
                    }
                }
            }
            BrainIngestionResult(resourceId,census.scanId,created,modified,renamed,restored,deleted,unchanged,
                census.enumerationComplete,census.omissions)
        }
    }
}
