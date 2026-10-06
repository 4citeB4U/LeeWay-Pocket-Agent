/*
REGION: LEEWAY.BRAIN.CORE
TAG: PORTABLE_BODY_IDENTITY_AND_RESOURCE_BINDINGS
WHO: Creator-authorized LeeWay engineering.
WHAT: Platform-independent portion extracted from the existing Brain bootstrap.
WHEN: Installation, reopen and authorized resource relocation.
WHERE: Shared Brain module; native storage, identity and observations are supplied by adapters.
WHY: No device model, physical path, operating system or LLM owns the Brain's identity.
HOW: Existing node/universe semantics with injected identity/storage; refuse ambiguous ownership.
LICENSE: MIT
*/
package industries.leeway.brain

private val logicalId = Regex("[A-Za-z0-9][A-Za-z0-9._-]{0,159}")

data class BodyIdentity(val deviceId: String, val publicKeyFingerprintSha256: String) {
    init {
        require(logicalId.matches(deviceId)) { "BODY_IDENTITY_INVALID" }
        require(Regex("[a-f0-9]{64}").matches(publicKeyFingerprintSha256)) { "BODY_KEY_FINGERPRINT_REQUIRED" }
    }
}

data class BrainNode(
    val id: String, val parentId: String?, val type: String,
    val title: String, val metadata: Map<String, String> = emptyMap()
)

data class ApplicationObservation(val id: String, val title: String, val metadata: Map<String, String>)

data class ResourceBinding(
    val logicalId: String, val bodyId: String, val resourceUri: String,
    val revision: Long, val ownerAuthorized: Boolean
)

data class ResourceHandle(val logicalId: String, val bodyId: String, val revision: Long)

/** Implemented by the existing native store; not another registry or remote authority. */
interface BrainStore {
    fun <T> atomic(operation: () -> T): T
    fun owner(): BodyIdentity?
    fun hasRecords(): Boolean
    fun claimOwner(identity: BodyIdentity)
    fun upsertNode(node: BrainNode, observedAtMs: Long)
    fun binding(logicalId: String): ResourceBinding?
    fun putBinding(binding: ResourceBinding)
}

data class BrainObservation(
    val device: Map<String, String>,
    val applications: List<ApplicationObservation>,
    val runtime: Map<String, String> = emptyMap()
)

data class BrainBootstrapResult(val bodyId: String, val rootNodeId: String, val observedApplicationCount: Int)

object DigitalBrain {
    fun rootId(identity: BodyIdentity) = "brain:" + identity.deviceId
    fun continuumRootId(identity: BodyIdentity) = "continuum:" + identity.deviceId

    private fun requireOwner(store: BrainStore, identity: BodyIdentity) {
        check(store.owner() == identity) { "BRAIN_OWNER_IDENTITY_MISMATCH" }
    }

    /** No I/O, OS calls, model calls or identity generation occur in the shared core. */
    fun bootstrap(
        identity: BodyIdentity, store: BrainStore, observation: BrainObservation, observedAtMs: Long
    ): BrainBootstrapResult {
        require(observedAtMs >= 0) { "OBSERVATION_TIME_INVALID" }
        require(observation.applications.all { logicalId.matches(it.id) }) { "APPLICATION_LOGICAL_ID_INVALID" }
        require(observation.applications.map { it.id }.distinct().size == observation.applications.size) {
            "DUPLICATE_APPLICATION_LOGICAL_ID"
        }
        return store.atomic {
            val owner = store.owner()
            if (owner == null) {
                check(!store.hasRecords()) { "EXISTING_BRAIN_OWNERSHIP_REQUIRES_VERIFIED_MIGRATION" }
                store.claimOwner(identity)
            }
            requireOwner(store, identity)
            val root = rootId(identity)
            val system = "$root:system"
            val user = "$root:user"
            val apps = "$system:applications"
            val nodes = listOf(
                BrainNode(root, null, "universe", "Digital Brain", mapOf("bodyId" to identity.deviceId, "architecture" to "recursive-universe")),
                BrainNode(system, root, "universe", "SYSTEM UNIVERSE"),
                BrainNode(user, root, "universe", "USER UNIVERSE"),
                BrainNode("$system:hardware", system, "universe", "HARDWARE UNIVERSE", observation.device),
                BrainNode("$system:runtime", system, "universe", "RUNTIME UNIVERSE", observation.runtime + ("bodyId" to identity.deviceId)),
                BrainNode(apps, system, "universe", "APPLICATION UNIVERSE"),
                BrainNode("$user:files", user, "universe", "FILESYSTEM UNIVERSE", mapOf("rootCapability" to "storage.authorized"))
            )
            nodes.forEach { store.upsertNode(it, observedAtMs) }
            observation.applications.forEach {
                store.upsertNode(BrainNode("$apps:${it.id}", apps, "application", it.title, it.metadata), observedAtMs)
            }
            BrainBootstrapResult(identity.deviceId, root, observation.applications.size)
        }
    }

    /** Bindings live in this device's Brain. URI strings are opaque to the shared core. */
    fun bindResource(store: BrainStore, identity: BodyIdentity, next: ResourceBinding): ResourceHandle = store.atomic {
        requireOwner(store, identity)
        require(logicalId.matches(next.logicalId)) { "RESOURCE_LOGICAL_ID_INVALID" }
        require(next.bodyId == identity.deviceId) { "RESOURCE_BODY_MISMATCH" }
        require(next.ownerAuthorized) { "RESOURCE_OWNER_AUTHORIZATION_REQUIRED" }
        require(next.resourceUri.isNotBlank() && next.revision > 0) { "RESOURCE_BINDING_INVALID" }
        val previous = store.binding(next.logicalId)
        if (previous != null) {
            require(previous.bodyId == identity.deviceId) { "RESOURCE_BODY_MISMATCH" }
            if (previous == next) return@atomic ResourceHandle(next.logicalId, next.bodyId, next.revision)
            require(next.revision > previous.revision) { "RESOURCE_BINDING_REVISION_MUST_ADVANCE" }
        }
        store.putBinding(next)
        ResourceHandle(next.logicalId, next.bodyId, next.revision)
    }

    fun resolveResource(store: BrainStore, identity: BodyIdentity, handle: ResourceHandle): ResourceBinding = store.atomic {
        requireOwner(store, identity)
        check(handle.bodyId == identity.deviceId) { "RESOURCE_BODY_MISMATCH" }
        val binding = store.binding(handle.logicalId) ?: error("RESOURCE_UNBOUND")
        check(binding.bodyId == identity.deviceId && binding.revision == handle.revision) { "RESOURCE_HANDLE_STALE" }
        check(binding.ownerAuthorized) { "RESOURCE_OWNER_AUTHORIZATION_REQUIRED" }
        binding
    }
}
