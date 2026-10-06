<#
REGION: LEEWAY.ENGINEERING.PORTABILITY
TAG: REMOVE_FIXED_DEVICE_AND_ANDROID_MASTER_ASSUMPTIONS
WHO: Creator-authorized Agent Lee.
WHAT: Complete the inspected portable Brain extraction and existing release-gate repair.
WHEN: Before package qualification; WHERE: the verified Pocket source checkout only.
WHY: Fixed customer identity and Android-only master policy are defects, not product features.
HOW: Dry run, exact source checks, in-memory change set, bounded writes/deletions, tests and receipt.
LICENSE: MIT
#>
[CmdletBinding()]
param([switch]$Apply)
Set-StrictMode -Version Latest
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
$origin=(& git -C $root remote get-url origin|Out-String).Trim()
$head=(& git -C $root rev-parse HEAD|Out-String).Trim()
$branch=(& git -C $root branch --show-current|Out-String).Trim()
if($origin -ne 'https://github.com/4citeB4U/LeeWay-Pocket-Agent.git'){throw 'SOURCE_ORIGIN_MISMATCH'}
if($head -ne 'c7a325a9197fc898fb35b86f7b5aa2414f64f212' -or $branch -ne 'repair/portable-golden-brain-bootstrap'){throw 'SOURCE_REVISION_CHANGED_REVIEW_REQUIRED'}
$expected=@{
 'contracts/golden-apk-release.v1.json'='3549BF92DD77824AB962FE496F13D7BBD4E6871A6F4AC789A579109F6EAA1030'
 'scripts/golden-release-gate.mjs'='2BF7A8848461825295AF596C050B57029DDC72FADD25BDD1D98C9D75CAA03014'
 'tests/golden-release-gate.test.mjs'='A27FC37AAC64386F80F7FED9969FB8CBCA8DC90415588E0C40A766A52A839570'
 'app/src/main/java/industries/leeway/pocket/FoldDigitalBrain.kt'='426504566BBFCC521874217D941DEC703063ED7188EA9B7364EB8F1EBEF15D3A'
 'app/src/main/java/industries/leeway/pocket/LeeWayBodyDatabases.kt'='4E65417DD0A88FF3AEC8D2E76AB7813E79047329F2C14B7794211A372346CE5A'
 'app/build.gradle.kts'='55F9DDC96B65C71C1B2EA51B8E2A6550B0B0B6F8FCEBE8313F9B22AC9A8E9493'
 'build.gradle.kts'='BFB8BAD8DF538D435011EA283CA8BC67F524B83BFC34353C2C9B793E709EE175'
 'settings.gradle.kts'='84374E052C68B010E063986B7F9B8F70FEB10029141E96DCA50FB55288193775'
}
foreach($p in $expected.Keys){if((Get-FileHash -LiteralPath (Join-Path $root $p) -Algorithm SHA256).Hash -ne $expected[$p]){throw ('SOURCE_HASH_CHANGED:'+ $p)}}
$changes=[ordered]@{}
$deletions=New-Object 'System.Collections.Generic.List[string]'
$utf8=New-Object System.Text.UTF8Encoding($false)
function Read-Source([string]$p){return [IO.File]::ReadAllText((Join-Path $root $p)).Replace("`r`n","`n")}
function Plan([string]$p,[string]$text){$changes[$p]=$text.Replace("`r`n","`n")}
function Replace-Once([string]$text,[string]$old,[string]$new){
 if(([regex]::Matches($text,[regex]::Escape($old))).Count -ne 1){throw ('REPLACEMENT_BOUNDARY_NOT_UNIQUE:'+ $old.Substring(0,[Math]::Min(85,$old.Length)))}
 return $text.Replace($old,$new)
}
$prefix='app/src/main/java/industries/leeway/pocket/'
$core='brain-core/src/main/kotlin/industries/leeway/brain/DigitalBrain.kt'
if(!(Test-Path -LiteralPath (Join-Path $root $core))){throw 'EXTRACTED_PORTABLE_CORE_MISSING'}
Plan 'settings.gradle.kts' ((Read-Source 'settings.gradle.kts')+[Environment]::NewLine+'include(":brain-core")'+[Environment]::NewLine)
Plan 'build.gradle.kts' (Replace-Once (Read-Source 'build.gradle.kts') '    id("org.jetbrains.kotlin.android") version "2.0.21" apply false' "    id(`"org.jetbrains.kotlin.android`") version `"2.0.21`" apply false`n    id(`"org.jetbrains.kotlin.jvm`") version `"2.0.21`" apply false")
$appGradle=Replace-Once (Read-Source 'app/build.gradle.kts') 'dependencies {' "dependencies {`n    implementation(project(`":brain-core`"))"
$appGradle=Replace-Once $appGradle 'versionCode = 26' 'versionCode = 27'
$appGradle=Replace-Once $appGradle 'versionName = "1.0.0-unified-body-fold6-rc1"' 'versionName = "1.0.0-portable-body-candidate"'
Plan 'app/build.gradle.kts' $appGradle
Plan 'brain-core/build.gradle.kts' @'
/* REGION: LEEWAY.BRAIN.BUILD; TAG: SHARED_CORE_JVM_QUALIFICATION
WHO: LeeWay; WHAT: Build the OS-neutral Brain core; WHY: Native APIs belong in adapters.
WHEN: Build; WHERE: shared module; HOW: Kotlin/JVM, no Android plugin or model dependency.
LICENSE: MIT */
plugins { id("org.jetbrains.kotlin.jvm") }
kotlin { jvmToolchain(17) }
dependencies { testImplementation("junit:junit:4.13.2") }
'@
Plan ($prefix+'devices/DeviceIdentity.kt') @'
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
'@

# Remove the fixed-body implementation; the native class becomes only a storage/sensor adapter.
$deletions.Add($prefix+'FoldDigitalBrain.kt')
Plan ($prefix+'AndroidDigitalBrainAdapter.kt') @'
/*
REGION: LEEWAY.BRAIN.ADAPTER.ANDROID
TAG: NATIVE_STORAGE_AND_OBSERVATION_FOR_PORTABLE_BRAIN
WHO: Creator-authorized LeeWay; WHAT: Connect the existing local Brain tables to the shared core.
WHEN: Bootstrap, snapshot and binding resolution; WHERE: Android boundary only.
WHY: Reusable Brain behavior must not import Android APIs or a fixed device identity.
HOW: Canonical Device Bridge identity, SQLite transactions and platform-provided storage URIs.
LICENSE: MIT
*/
package industries.leeway.pocket

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.os.Build
import android.os.Environment
import android.os.StatFs
import industries.leeway.brain.*
import industries.leeway.pocket.devices.DeviceIdentity
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

object AndroidDigitalBrainAdapter {
    fun identity(context: Context): BodyIdentity = DeviceIdentity.ensure(context.applicationContext)
    private fun stableId(value: String) = MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { b -> "%02x".format(b) }

    private fun deviceObservation(): Map<String,String> {
        val stat=StatFs(Environment.getDataDirectory().path)
        return mapOf("manufacturer" to Build.MANUFACTURER,"model" to Build.MODEL,"device" to Build.DEVICE,
            "android" to Build.VERSION.RELEASE,"sdk" to Build.VERSION.SDK_INT.toString(),
            "storageBytes" to stat.totalBytes.toString(),"storageAvailableBytes" to stat.availableBytes.toString())
    }

    private fun runtimeObservation() = mapOf(
        "agentId" to "agent-lee", "brainBootstrap" to "LOCAL_IMPLEMENTATION_REQUIRES_DEVICE_QUALIFICATION",
        "continuum" to "LOCAL_SCHEMA_ONLY_CANONICAL_RUNTIME_UNBOUND",
        "ldwmd" to "LOCAL_SCHEMA_ONLY_CANONICAL_RUNTIME_UNBOUND",
        "conversation" to "CANONICAL_EXECUTOR_UNBOUND", "voice" to "SELECTED_RENDERER_REQUIRES_QUALIFICATION",
        "skills" to "AUTHORITY_REFERENCE_NOT_EXECUTION", "formula" to "NOT_EXECUTED"
    )

    @Synchronized fun bootstrap(context: Context) {
        val body=identity(context)
        LeeWayBodyDatabases(context).use { dbs ->
            val store=SqliteBrainStore(dbs.brain.writableDatabase)
            val apps=context.packageManager.getInstalledApplications(0).map { app ->
                ApplicationObservation(stableId(app.packageName),app.packageName,mapOf("package" to app.packageName))
            }
            DigitalBrain.bootstrap(body,store,BrainObservation(deviceObservation(),apps,runtimeObservation()),System.currentTimeMillis())
            // This adapter observes platform storage. Only the Brain persists these physical mappings.
            val resources=mapOf(
                "app-private-files" to context.filesDir.toURI().toString(),
                "brain-storage" to context.getDatabasePath(LeeWayBodyDatabases.BRAIN_NAME).toURI().toString(),
                "continuum-storage" to context.getDatabasePath(LeeWayBodyDatabases.CONTINUUM_NAME).toURI().toString(),
                "working-memory-storage" to context.getDatabasePath(LeeWayBodyDatabases.WORKING_NAME).toURI().toString()
            )
            resources.forEach { (logicalId,uri) ->
                val old=store.binding(logicalId)
                val revision=if(old==null)1L else if(old.resourceUri==uri && old.ownerAuthorized)old.revision else Math.addExact(old.revision,1)
                DigitalBrain.bindResource(store,body,ResourceBinding(logicalId,body.deviceId,uri,revision,true))
            }
            // These existing scaffolds are not promoted to canonical Continuum or LDWMD execution.
            val continuum=dbs.continuum.writableDatabase
            val root=DigitalBrain.continuumRootId(body)
            val names=mapOf("" to "Device Continuum",":brain" to "Digital Brain Universe",":workstation" to "Workstation Universe",":runtime" to "Runtime Universe",":evidence" to "Evidence Universe")
            continuum.beginTransaction()
            try {
                names.forEach { (suffix,title) ->
                    val values=ContentValues().apply {
                        put("id",root+suffix);if(suffix.isEmpty())putNull("parent_id")else put("parent_id",root)
                        put("kind",if(suffix.isEmpty())"body" else suffix.removePrefix(":"));put("title",title)
                        put("authority",body.deviceId);put("schema_version","leeway.continuum.v1")
                        put("trust_state","LOCAL");put("health_state","UNQUALIFIED");put("metadata_json","{}")
                        put("created_at",System.currentTimeMillis());put("updated_at",System.currentTimeMillis())
                    }
                    continuum.insertWithOnConflict("universes",null,values,SQLiteDatabase.CONFLICT_IGNORE)
                }
                continuum.setTransactionSuccessful()
            } finally {continuum.endTransaction()}
            dbs.ldwmd.writableDatabase
        }
    }

    @Synchronized fun snapshot(context: Context): String {
        val body=identity(context)
        return LeeWayBodyDatabases(context).use { dbs ->
            val db=dbs.brain.readableDatabase
            check(SqliteBrainStore(db).owner()==body) { "BRAIN_OWNER_IDENTITY_MISMATCH" }
            val counts=JSONObject()
            listOf("nodes","edges","provenance","formula_passports","sync_events","node_tombstones","experiences","hardware_stats").forEach { table ->
                db.rawQuery("SELECT COUNT(*) FROM "+table,null).use { c -> if(c.moveToFirst()) counts.put(table,c.getLong(0)) }
            }
            val nodes=JSONArray()
            db.rawQuery("SELECT id,parent_id,title,type,metadata_json FROM nodes WHERE type='universe' ORDER BY title",null).use { c ->
                while(c.moveToNext())nodes.put(JSONObject().put("id",c.getString(0)).put("parentId",if(c.isNull(1))JSONObject.NULL else c.getString(1))
                    .put("title",c.getString(2)).put("type",c.getString(3)).put("metadata",JSONObject(c.getString(4))))
            }
            JSONObject().put("schemaVersion","leeway.digital-brain.context.v1").put("bodyId",body.deviceId)
                .put("state","LOCAL_BOOTSTRAP_ONLY_NOT_FULL_BRAIN_ACCEPTANCE").put("counts",counts).put("universes",nodes)
                .put("informationBrain",JSONObject().put("counts",counts).put("universes",nodes))
                .put("deviceBrain",JSONObject(deviceObservation())).put("runtimeBrain",JSONObject(runtimeObservation())).toString()
        }
    }

    private class SqliteBrainStore(private val db: SQLiteDatabase): BrainStore {
        override fun <T> atomic(operation: () -> T): T {
            db.beginTransaction()
            try {val value=operation();db.setTransactionSuccessful();return value} finally {db.endTransaction()}
        }
        override fun owner(): BodyIdentity? = db.rawQuery("SELECT device_id,key_fingerprint FROM brain_owner WHERE singleton=1",null).use { c ->
            if(c.moveToFirst())BodyIdentity(c.getString(0),c.getString(1))else null
        }
        override fun hasRecords(): Boolean = db.rawQuery("SELECT COUNT(*) FROM nodes",null).use {c->c.moveToFirst();c.getLong(0)>0}
        override fun claimOwner(identity: BodyIdentity) {
            check(owner()==null && !hasRecords()) { "BRAIN_OWNERSHIP_NOT_EMPTY" }
            db.insertOrThrow("brain_owner",null,ContentValues().apply{put("singleton",1);put("device_id",identity.deviceId);put("key_fingerprint",identity.publicKeyFingerprintSha256)})
        }
        override fun upsertNode(node: BrainNode, observedAtMs: Long) {
            val values=ContentValues().apply {
                put("parent_id",node.parentId);put("type",node.type);put("title",node.title);put("status","observed")
                put("metadata_json",JSONObject(node.metadata).toString());put("updated_at",observedAtMs)
            }
            if(db.update("nodes",values,"id=?",arrayOf(node.id))==0){values.put("id",node.id);values.put("created_at",observedAtMs);db.insertOrThrow("nodes",null,values)}
        }
        override fun binding(logicalId: String): ResourceBinding? = db.rawQuery("SELECT body_id,resource_uri,revision,owner_authorized FROM brain_resource_bindings WHERE logical_id=?",arrayOf(logicalId)).use {c->
            if(c.moveToFirst())ResourceBinding(logicalId,c.getString(0),c.getString(1),c.getLong(2),c.getInt(3)==1)else null
        }
        override fun putBinding(binding: ResourceBinding) {
            val v=ContentValues().apply{put("body_id",binding.bodyId);put("resource_uri",binding.resourceUri);put("revision",binding.revision);put("owner_authorized",if(binding.ownerAuthorized)1 else 0)}
            if(db.update("brain_resource_bindings",v,"logical_id=?",arrayOf(binding.logicalId))==0){v.put("logical_id",binding.logicalId);db.insertOrThrow("brain_resource_bindings",null,v)}
        }
    }
}
'@
$db=Read-Source ($prefix+'LeeWayBodyDatabases.kt')
$db=Replace-Once $db 'class LeeWayBodyDatabases(context:Context) {' @'
class LeeWayBodyDatabases(context:Context): java.io.Closeable {
    companion object {
        const val BRAIN_NAME="leeway-digital-brain.db"
        const val CONTINUUM_NAME="leeway-continuum.db"
        const val WORKING_NAME="leeway-working-memory.db"
    }
    init {
        val expected=setOf(BRAIN_NAME,CONTINUUM_NAME,WORKING_NAME)
        val other=context.databaseList().filter { name -> name.startsWith("leeway-") && name.endsWith(".db") && name !in expected }
        check(other.isEmpty()) { "EXISTING_DATABASE_SET_REQUIRES_OWNER_MIGRATION" }
    }
    override fun close(){brain.close();continuum.close();ldwmd.close()}
'@
$db=Replace-Once $db 'SQLiteOpenHelper(c,"leeway-digital-brain-phone-fold6.db",null,1)' 'SQLiteOpenHelper(c,BRAIN_NAME,null,2)'
$db=Replace-Once $db 'SQLiteOpenHelper(c,"leeway-continuum-phone-fold6.db",null,1)' 'SQLiteOpenHelper(c,CONTINUUM_NAME,null,1)'
$db=Replace-Once $db 'SQLiteOpenHelper(c,"leeway-ldwmd-phone-fold6.db",null,1)' 'SQLiteOpenHelper(c,WORKING_NAME,null,1)'
$db=Replace-Once $db '        override fun onCreate(db:SQLiteDatabase){ db.execSQL("""' "        override fun onCreate(db:SQLiteDatabase){ createOwnership(db); db.execSQL(`"`"`""
# Only replace the first Brain upgrade hook; existing independent stores remain separate.
$needle='        override fun onUpgrade(db:SQLiteDatabase,o:Int,n:Int){}'
$at=$db.IndexOf($needle)
if($at -lt 0){throw 'BRAIN_UPGRADE_HOOK_MISSING'}
$ownership=@'
        private fun createOwnership(db:SQLiteDatabase){
            db.execSQL("CREATE TABLE IF NOT EXISTS brain_owner(singleton INTEGER PRIMARY KEY CHECK(singleton=1),device_id TEXT NOT NULL UNIQUE,key_fingerprint TEXT NOT NULL)")
            db.execSQL("CREATE TABLE IF NOT EXISTS brain_resource_bindings(logical_id TEXT PRIMARY KEY,body_id TEXT NOT NULL,resource_uri TEXT NOT NULL,revision INTEGER NOT NULL CHECK(revision>0),owner_authorized INTEGER NOT NULL CHECK(owner_authorized IN (0,1)))")
        }
        override fun onUpgrade(db:SQLiteDatabase,o:Int,n:Int){if(o<2)createOwnership(db)}
'@
$db=$db.Substring(0,$at)+$ownership+$db.Substring($at+$needle.Length)
Plan ($prefix+'LeeWayBodyDatabases.kt') $db
$main=Read-Source ($prefix+'MainActivity.kt')
$main=$main.Replace('FoldDigitalBrain','AndroidDigitalBrainAdapter').Replace('bootstrapFoldBrain','bootstrapDeviceBrain')
$main=Replace-Once $main 'fun bodyId():String = "phone-fold6"' 'fun bodyId():String = AndroidDigitalBrainAdapter.identity(this@MainActivity).deviceId'
Plan ($prefix+'MainActivity.kt') $main
foreach($name in @('LeeWayAutomation.kt','LeeWayDeviceService.kt')) {
 Plan ($prefix+$name) (Replace-Once (Read-Source ($prefix+$name)) '.put("bodyId","phone-fold6")' '.put("bodyId",AndroidDigitalBrainAdapter.identity(context).deviceId)')
}
$activity=Read-Source ($prefix+'PocketVoiceActivity.kt')
$activity=Replace-Once $activity 'text="Agent Lee · phone-fold6"' 'text="Agent Lee"'
$activity=Replace-Once $activity '"continuum:phone-fold6:conversation"' '(industries.leeway.brain.DigitalBrain.continuumRootId(AndroidDigitalBrainAdapter.identity(this))+":conversation")'
Plan ($prefix+'PocketVoiceActivity.kt') $activity
$ui=Read-Source 'app/src/main/assets/agent_lee_sphere_transparent.html'
$ui=Replace-Once $ui 'Digital Brain · phone-fold6' 'Digital Brain'
$ui=Replace-Once $ui 'PHONE-FOLD6' 'THIS DEVICE'
Plan 'app/src/main/assets/agent_lee_sphere_transparent.html' $ui
# Package metadata describes shared voice lineage, not a particular recipient's body.
$lockPath='app/src/main/assets/voice/source-lock.json'
if(Test-Path (Join-Path $root $lockPath)){
 $lock=Read-Source $lockPath|ConvertFrom-Json
 $lock.PSObject.Properties.Remove('body');$lock.PSObject.Properties.Remove('sourceBody')
 Plan $lockPath ($lock|ConvertTo-Json -Depth 12)
}

# Replace the Android-only master policy. Keep all existing customer safety requirements.
$policy=Read-Source 'contracts/golden-apk-release.v1.json'|ConvertFrom-Json
$caseCount=0;foreach($g in $policy.requiredGroups.PSObject.Properties){$caseCount+=@($g.Value).Count}
if($caseCount -ne 46){throw 'REQUIRED_CASE_SET_CHANGED_REVIEW_REQUIRED'}
$policy.schemaVersion='leeway.golden-system-release.v1'
$policy.distribution.PSObject.Properties.Remove('oneAndroidApk')
$policy|Add-Member -NotePropertyName architecture -NotePropertyValue ([ordered]@{core='PLATFORM_NEUTRAL';nativeFormats='SEPARATE_HASH_BOUND_PLATFORM_PROFILES';deviceIdentity='RUNTIME_CREATED_NOT_COMPILED';platformAcceptance='SEPARATELY_EXECUTED_REQUIRED'})
Plan 'contracts/golden-system-release.v1.json' ($policy|ConvertTo-Json -Depth 20)
Plan 'contracts/platforms/android.v1.json' @'
{
  "schemaVersion":"leeway.platform-release-profile.v1",
  "id":"android",
  "nativeArtifactType":"APK",
  "packaging":{"oneAndroidApk":true,"oneLauncher":true,"releaseSigningRequired":true,"companionLeewayPackagesRequired":false},
  "qualification":"REQUIRES_ALL_SHARED_CASES_AND_ACTUAL_ANDROID_ARTIFACT_AND_DEVICE_EVIDENCE",
  "platformTested":false,
  "universalInstallerClaim":false
}
'@
$deletions.Add('contracts/golden-apk-release.v1.json')
# The previous one-off patch would reinstall the rejected policy. Remove it from the active source.
if(Test-Path (Join-Path $root 'scripts/Apply-GoldenReleaseGate.ps1')){$deletions.Add('scripts/Apply-GoldenReleaseGate.ps1')}
$gate=Read-Source 'scripts/golden-release-gate.mjs'
$gate=$gate.Replace('GOLDEN_APK_EVIDENCE_ADMISSION','GOLDEN_SYSTEM_EVIDENCE_ADMISSION').Replace('APK/source/contract','artifact/source/contract').Replace('golden-apk-release.v1.json','golden-system-release.v1.json').Replace('leeway.golden-apk-release.v1','leeway.golden-system-release.v1').Replace('leeway.golden-apk-evidence-assessment.v1','leeway.golden-system-evidence-assessment.v1')
$gate=Replace-Once $gate 'export function requiredCases(policy){' @'
export function loadPlatformProfile(profilePath){
 const bytes=fs.readFileSync(profilePath),profile=JSON.parse(bytes.toString('utf8').replace(/^\uFEFF/,''));
 if(profile.schemaVersion!=='leeway.platform-release-profile.v1'||!ID.test(profile.id||'')||typeof profile.nativeArtifactType!=='string'||!profile.nativeArtifactType.trim())throw Error('PLATFORM_PROFILE_INVALID');
 return{profile,sha256:digest(bytes)};
}
export function requiredCases(policy){
'@
$gate=Replace-Once $gate 'sourceCommit,expectedProfiles,dossier,evidenceRoot,verifyEvidence}={}){' 'sourceCommit,expectedProfiles,dossier,evidenceRoot,verifyEvidence,platformProfile,platformProfileSha256}={}){'
$gate=Replace-Once $gate " const profiles=Array.isArray(expectedProfiles)?expectedProfiles:[];" @'
 const platformValid=platformProfile?.schemaVersion==='leeway.platform-release-profile.v1'&&ID.test(platformProfile?.id||'')&&typeof platformProfile?.nativeArtifactType==='string'&&platformProfile.nativeArtifactType.trim().length>0&&HASH.test(platformProfileSha256||'');
 if(!platformValid)add('HASH_BOUND_PLATFORM_PROFILE_REQUIRED');
 const profiles=Array.isArray(expectedProfiles)?expectedProfiles:[];
'@
$gate=Replace-Once $gate "  if(e.artifactSha256!==artifactSha256||e.sourceCommit!==sourceCommit||e.policySha256!==policySha256){" "  if(!platformValid||e.platformId!==platformProfile.id||e.platformProfileSha256!==platformProfileSha256){add('EVIDENCE_IS_FOR_DIFFERENT_PLATFORM',meta);continue;}`n  if(e.artifactSha256!==artifactSha256||e.sourceCommit!==sourceCommit||e.policySha256!==policySha256){"
$gate=Replace-Once $gate 'bytes:Buffer.from(bytes),artifactSha256,sourceCommit,policySha256})' 'bytes:Buffer.from(bytes),artifactSha256,sourceCommit,policySha256,platformId:platformProfile.id,platformProfileSha256})'
$gate=Replace-Once $gate 'blockers,artifactSha256,sourceCommit,policySha256,releasePromoted:false,installed:false' 'blockers,artifactSha256,sourceCommit,policySha256,platformId:platformValid?platformProfile.id:null,platformProfileSha256,releasePromoted:false,installed:false'
$gate=$gate.Replace("'--apk'","'--artifact'").Replace("const apk=values.get('--artifact');if(!apk)throw Error('APK_PATH_REQUIRED');","const artifact=values.get('--artifact');if(!artifact)throw Error('ARTIFACT_PATH_REQUIRED');").Replace('fs.readFileSync(apk)','fs.readFileSync(artifact)')
$gate=Replace-Once $gate "'--artifact','--source-commit','--profiles'" "'--artifact','--platform-profile','--source-commit','--profiles'"
$gate=Replace-Once $gate ' const {policy,sha256}=loadGoldenPolicy();' " const {policy,sha256}=loadGoldenPolicy();`n if(!values.has('--platform-profile'))throw Error('PLATFORM_PROFILE_PATH_REQUIRED');`n const {profile:platformProfile,sha256:platformProfileSha256}=loadPlatformProfile(values.get('--platform-profile'));"
$gate=Replace-Once $gate 'assessGoldenEvidence({policy,policySha256:sha256,artifactSha256:' 'assessGoldenEvidence({policy,policySha256:sha256,platformProfile,platformProfileSha256,artifactSha256:'
Plan 'scripts/golden-release-gate.mjs' $gate
$wrapper=Read-Source 'Verify-SingleApk.ps1'
$wrapper=$wrapper.Replace('FoldDigitalBrain.kt','AndroidDigitalBrainAdapter.kt')
$wrapper=Replace-Once $wrapper "'--apk',`$ApkPath" "'--artifact',`$ApkPath,'--platform-profile',(Join-Path `$PSScriptRoot 'contracts\platforms\android.v1.json')"
$wrapper=Replace-Once $wrapper "'LEEWAY_SINGLE_APK_SOURCE_STRUCTURE_PASS_NOT_RUNTIME_ACCEPTANCE'" @'
if(!(Test-Path (Join-Path $PSScriptRoot 'brain-core/src/main/kotlin/industries/leeway/brain/DigitalBrain.kt'))){throw 'PORTABLE_BRAIN_CORE_MISSING'}
'LEEWAY_SINGLE_APK_SOURCE_STRUCTURE_PASS_NOT_RUNTIME_ACCEPTANCE'
'@
Plan 'Verify-SingleApk.ps1' $wrapper
$qualification=Read-Source 'scripts/Invoke-GoldenGateQualification.ps1'
$qualification=$qualification.Replace('golden-apk-release.v1.json','golden-system-release.v1.json')
$qualification=$qualification.Replace('--apk $ApkPath','--artifact $ApkPath --platform-profile contracts/platforms/android.v1.json')
Plan 'scripts/Invoke-GoldenGateQualification.ps1' $qualification
$tests=Read-Source 'tests/golden-release-gate.test.mjs'
$tests=Replace-Once $tests 'loadGoldenPolicy,requiredCases}' 'loadGoldenPolicy,requiredCases,loadPlatformProfile}'
$tests=Replace-Once $tests " const {policy,sha256:policySha256}=loadGoldenPolicy();const artifactSha256='a'.repeat(64),sourceCommit='b'.repeat(40);" @'
 const {policy,sha256:policySha256}=loadGoldenPolicy();const artifactSha256='a'.repeat(64),sourceCommit='b'.repeat(40);
 const {profile:platformProfile,sha256:platformProfileSha256}=loadPlatformProfile(new URL('../contracts/platforms/android.v1.json',import.meta.url));
'@
$tests=Replace-Once $tests 'profileId,caseId,artifactSha256,sourceCommit,policySha256,status:' 'profileId,caseId,platformId:platformProfile.id,platformProfileSha256,artifactSha256,sourceCommit,policySha256,status:'
$tests=Replace-Once $tests 'return{policy,policySha256,artifactSha256,sourceCommit,expectedProfiles:' 'return{policy,policySha256,platformProfile,platformProfileSha256,artifactSha256,sourceCommit,expectedProfiles:'
$tests+=@'

test('master release policy is platform-neutral and retains all 46 cases',()=>{
 const {policy}=loadGoldenPolicy();assert.equal(policy.schemaVersion,'leeway.golden-system-release.v1');
 assert.equal('oneAndroidApk' in policy.distribution,false);assert.equal(requiredCases(policy).length,46);
 assert.equal(policy.architecture.core,'PLATFORM_NEUTRAL');
 const {profile}=loadPlatformProfile(new URL('../contracts/platforms/android.v1.json',import.meta.url));
 assert.equal(profile.packaging.oneAndroidApk,true);assert.equal(profile.platformTested,false);
});
test('old Android master schema is no longer accepted',()=>{
 const {policy}=loadGoldenPolicy();policy.schemaVersion='leeway.golden-apk-release.v1';assert.throws(()=>requiredCases(policy),/GOLDEN_POLICY_INVALID/);
});
test('missing platform adapter profile cannot admit the master product',async t=>{
 const f=fixture(t);delete f.platformProfile;assert.ok(has(await assessGoldenEvidence(f),'HASH_BOUND_PLATFORM_PROFILE_REQUIRED'));
});
test('platform policy change invalidates otherwise matching evidence',async t=>{
 const f=fixture(t);f.platformProfileSha256='d'.repeat(64);assert.ok(has(await assessGoldenEvidence(f),'EVIDENCE_IS_FOR_DIFFERENT_PLATFORM'));
});
test('Android evidence cannot certify another platform',async t=>{
 const f=fixture(t);f.platformProfile={...f.platformProfile,id:'different-platform-test-only',nativeArtifactType:'TEST'};
 assert.ok(has(await assessGoldenEvidence(f),'EVIDENCE_IS_FOR_DIFFERENT_PLATFORM'));
});
'@
Plan 'tests/golden-release-gate.test.mjs' $tests

Plan 'brain-core/src/test/kotlin/industries/leeway/brain/DigitalBrainTest.kt' @'
/*
REGION: LEEWAY.BRAIN.QUALIFICATION
TAG: PORTABLE_BRAIN_BEHAVIOR_TESTS
WHO: LeeWay engineering; WHAT: Execute shared bootstrap and binding behavior with explicit test stores.
WHEN: Before native qualification; WHERE: Kotlin/JVM test harness, not a physical-device claim.
WHY: Fixed identities, stale handles and cross-body writes must fail; reuse must retain state.
HOW: Real shared functions with deterministic fixture inputs and no model dependency.
LICENSE: MIT
*/
package industries.leeway.brain

import org.junit.Assert.*
import org.junit.Test

class DigitalBrainTest {
    private val a=BodyIdentity("device-a","a".repeat(64))
    private val b=BodyIdentity("device-b","b".repeat(64))
    private val observation=BrainObservation(mapOf("kind" to "test-sensor"),listOf(ApplicationObservation("app-1","Test App",emptyMap())))
    private class Store: BrainStore {
        var identity: BodyIdentity?=null
        val nodes=linkedMapOf<String,BrainNode>()
        val created=linkedMapOf<String,Long>()
        val bindings=linkedMapOf<String,ResourceBinding>()
        override fun <T> atomic(operation: () -> T): T {
            val i=identity;val n=nodes.toMap();val c=created.toMap();val r=bindings.toMap()
            try{return operation()}catch(e:Exception){identity=i;nodes.clear();nodes.putAll(n);created.clear();created.putAll(c);bindings.clear();bindings.putAll(r);throw e}
        }
        override fun owner()=identity
        override fun hasRecords()=nodes.isNotEmpty()
        override fun claimOwner(identity:BodyIdentity){this.identity=identity}
        override fun upsertNode(node:BrainNode,observedAtMs:Long){nodes[node.id]=node;created.putIfAbsent(node.id,observedAtMs)}
        override fun binding(logicalId:String)=bindings[logicalId]
        override fun putBinding(binding:ResourceBinding){bindings[binding.logicalId]=binding}
    }
    private fun ready(identity:BodyIdentity=a)=Store().also{DigitalBrain.bootstrap(identity,it,observation,100)}
    private fun binding(body:BodyIdentity=a,uri:String="test-storage://owner/location",revision:Long=1)=ResourceBinding("workspace",body.deviceId,uri,revision,true)

    @Test fun freshBrainUsesSuppliedIdentity(){val s=ready();assertEquals(a,s.owner());assertTrue(s.nodes.containsKey("brain:"+a.deviceId));assertEquals(8,s.nodes.size)}
    @Test fun twoInstallationsHaveIndependentRoots(){val x=ready(a);val y=ready(b);assertTrue(x.nodes.keys.intersect(y.nodes.keys).isEmpty());assertNotEquals(x.owner(),y.owner())}
    @Test fun recursiveHierarchyIsRetained(){val s=ready();val root=DigitalBrain.rootId(a);assertEquals(root+":system",s.nodes[root+":system:applications"]?.parentId);assertEquals(root+":user",s.nodes[root+":user:files"]?.parentId)}
    @Test fun repeatedBootstrapDoesNotDuplicateOrResetCreatedTime(){val s=ready();val before=s.created.toMap();DigitalBrain.bootstrap(a,s,observation,500);assertEquals(8,s.nodes.size);assertEquals(before,s.created)}
    @Test fun existingOtherOwnerBlocksBeforeMutation(){val s=ready();val before=s.nodes.toMap();assertThrows(IllegalStateException::class.java){DigitalBrain.bootstrap(b,s,observation,200)};assertEquals(before,s.nodes);assertEquals(a,s.owner())}
    @Test fun unownedNonemptyBrainRequiresExplicitMigration(){val s=Store();s.nodes["existing"]=BrainNode("existing",null,"universe","Owner data");assertThrows(IllegalStateException::class.java){DigitalBrain.bootstrap(a,s,observation,200)};assertNull(s.owner());assertEquals(1,s.nodes.size)}
    @Test fun invalidBodyIdentityRejected(){assertThrows(IllegalArgumentException::class.java){BodyIdentity("../escape","a".repeat(64))};assertThrows(IllegalArgumentException::class.java){BodyIdentity("device","invalid")}}
    @Test fun duplicateApplicationIdentityRejected(){val s=Store();assertThrows(IllegalArgumentException::class.java){DigitalBrain.bootstrap(a,s,observation.copy(applications=observation.applications+observation.applications),1)};assertNull(s.owner());assertTrue(s.nodes.isEmpty())}
    @Test fun invalidObservationTimeRejected(){assertThrows(IllegalArgumentException::class.java){DigitalBrain.bootstrap(a,Store(),observation,-1)}}
    @Test fun authorizedResourceResolves(){val s=ready();val next=binding();val h=DigitalBrain.bindResource(s,a,next);assertEquals(next,DigitalBrain.resolveResource(s,a,h))}
    @Test fun relocationKeepsIdentityAndInvalidatesOldHandle(){val s=ready();val h=DigitalBrain.bindResource(s,a,binding());val next=binding(uri="test-storage://new/location",revision=2);val h2=DigitalBrain.bindResource(s,a,next);assertEquals(a,s.owner());assertEquals(next,DigitalBrain.resolveResource(s,a,h2));assertThrows(IllegalStateException::class.java){DigitalBrain.resolveResource(s,a,h)}}
    @Test fun resourceLocationCanContainSpacesAndUnicode(){val s=ready();val next=binding(uri="test-storage://owner/Folder with spaces/資料");val h=DigitalBrain.bindResource(s,a,next);assertEquals(next.resourceUri,DigitalBrain.resolveResource(s,a,h).resourceUri)}
    @Test fun unapprovedResourceNeverBecomesBound(){val s=ready();assertThrows(IllegalArgumentException::class.java){DigitalBrain.bindResource(s,a,binding().copy(ownerAuthorized=false))};assertTrue(s.bindings.isEmpty())}
    @Test fun bindingCannotCrossBodies(){val s=ready();assertThrows(IllegalArgumentException::class.java){DigitalBrain.bindResource(s,a,binding(b))};assertTrue(s.bindings.isEmpty())}
    @Test fun handleCannotCrossBodies(){val x=ready(a);val y=ready(b);val h=DigitalBrain.bindResource(x,a,binding(a));assertThrows(IllegalStateException::class.java){DigitalBrain.resolveResource(y,b,h)}}
    @Test fun changedLocationRequiresNewRevision(){val s=ready();DigitalBrain.bindResource(s,a,binding());assertThrows(IllegalArgumentException::class.java){DigitalBrain.bindResource(s,a,binding(uri="test-storage://other/location"))};assertEquals("test-storage://owner/location",s.bindings["workspace"]?.resourceUri)}
    @Test fun sameBindingIsIdempotent(){val s=ready();val first=DigitalBrain.bindResource(s,a,binding());assertEquals(first,DigitalBrain.bindResource(s,a,binding()));assertEquals(1,s.bindings.size)}
    @Test fun missingOrWithdrawnResourceFailsClosed(){val s=ready();assertThrows(IllegalStateException::class.java){DigitalBrain.resolveResource(s,a,ResourceHandle("missing",a.deviceId,1))};val h=DigitalBrain.bindResource(s,a,binding());s.bindings["workspace"]=binding().copy(ownerAuthorized=false);assertThrows(IllegalStateException::class.java){DigitalBrain.resolveResource(s,a,h)}}
}
'@
Plan 'tests/portable-brain-wiring.test.mjs' @'
/* REGION: LEEWAY.BRAIN.QUALIFICATION; TAG: PORTABLE_CORE_WIRING
WHO: LeeWay; WHAT: Check compile inputs and no reintroduction of rejected implementation.
WHEN: Source qualification; WHERE: existing repository; WHY: A new file without callers is not a repair.
HOW: Inspect active sources and Gradle linkage; behavioral proof is the separate Kotlin suite.
LICENSE: MIT */
import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
const read=p=>fs.readFileSync(new URL('../'+p,import.meta.url),'utf8');
const main='app/src/main/java/industries/leeway/pocket/';
test('portable core has no native platform or LLM imports',()=>{
 const core=read('brain-core/src/main/kotlin/industries/leeway/brain/DigitalBrain.kt');
 assert.doesNotMatch(core,/^import\s+(android|androidx|java\.|kotlinx\.coroutines|com\.)/m);
 assert.doesNotMatch(core,/phone-fold6|FoldDigitalBrain|System\.getenv|openai|gemini|ollama/i);
 assert.ok(core.includes('interface BrainStore'));
});
test('existing Android entrypoint actually invokes the portable core adapter',()=>{
 assert.ok(read('settings.gradle.kts').includes('include(":brain-core")'));
 assert.ok(read('app/build.gradle.kts').includes('implementation(project(":brain-core"))'));
 assert.ok(read(main+'MainActivity.kt').includes('AndroidDigitalBrainAdapter.bootstrap(this)'));
 assert.ok(read(main+'AndroidDigitalBrainAdapter.kt').includes('DigitalBrain.bootstrap('));
});
test('fixed-body implementation and Android-only master policy are absent',()=>{
 assert.equal(fs.existsSync(new URL('../'+main+'FoldDigitalBrain.kt',import.meta.url)),false);
 assert.equal(fs.existsSync(new URL('../contracts/golden-apk-release.v1.json',import.meta.url)),false);
 for(const file of ['MainActivity.kt','PocketVoiceActivity.kt','LeeWayAutomation.kt','LeeWayDeviceService.kt','LeeWayBodyDatabases.kt','AndroidDigitalBrainAdapter.kt'])assert.doesNotMatch(read(main+file),/phone-fold6|FoldDigitalBrain|Fold6/);
});
test('native identity is sourced from internal Devices and mappings stay in Brain',()=>{
 assert.ok(read(main+'AndroidDigitalBrainAdapter.kt').includes('devices.DeviceIdentity'));
 assert.ok(read(main+'LeeWayBodyDatabases.kt').includes('brain_resource_bindings'));
 assert.ok(read(main+'devices/DeviceIdentity.kt').includes('AndroidKeyStore'));
 assert.ok(read(main+'devices/DeviceIdentity.kt').includes('UUID.randomUUID()'));
});
'@

# Validate all planned text before any source mutation; rollback exists in memory only during this operation.
foreach($p in $changes.Keys){
 if($p.EndsWith('.json')){$null=$changes[$p]|ConvertFrom-Json}
 if($p.EndsWith('.ps1')){$tokens=$null;$errors=$null;$null=[Management.Automation.Language.Parser]::ParseInput($changes[$p],[ref]$tokens,[ref]$errors);if($errors.Count){throw ('GENERATED_POWERSHELL_INVALID:'+ $p)}}
}
$summary=[ordered]@{status='DRY_RUN';branch=$branch;sourceBase=$head;writePaths=@($changes.Keys);deletePaths=@($deletions);requiredMasterCases=$caseCount;phoneModified=$false;releasePromoted=$false}
if(!$Apply){$summary|ConvertTo-Json -Depth 8;return}
$before=[ordered]@{}
foreach($p in (@($changes.Keys)+@($deletions)|Select-Object -Unique)){
 $full=Join-Path $root $p
 $before[$p]=if(Test-Path -LiteralPath $full){[IO.File]::ReadAllBytes($full)}else{$null}
}
try {
 foreach($p in $changes.Keys){$full=Join-Path $root $p;$null=[IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($full));[IO.File]::WriteAllText($full,$changes[$p],$utf8)}
 foreach($p in $deletions){[IO.File]::Delete((Join-Path $root $p))}
} catch {
 foreach($p in $before.Keys){$full=Join-Path $root $p;if($null -eq $before[$p]){if(Test-Path $full){[IO.File]::Delete($full)}}else{[IO.File]::WriteAllBytes($full,$before[$p])}}
 throw
}
$run='portable-repair-'+[DateTime]::UtcNow.ToString('yyyyMMddTHHmmssfffZ')
$evidence=Join-Path $root ('qualification/'+$run)
$null=[IO.Directory]::CreateDirectory($evidence)
$hashes=@(foreach($p in $changes.Keys){[ordered]@{path=$p;sha256=(Get-FileHash -LiteralPath (Join-Path $root $p) -Algorithm SHA256).Hash}})
$summary.status='SOURCE_REPAIRED_TESTS_REQUIRED'
$summary|Add-Member -NotePropertyName sourceHashes -NotePropertyValue $hashes
$summary|Add-Member -NotePropertyName formulaExecution -NotePropertyValue 'NOT_EXECUTED'
$summary|Add-Member -NotePropertyName learningLedgerUpdated -NotePropertyValue $false
$summary|ConvertTo-Json -Depth 10|Set-Content -LiteralPath (Join-Path $evidence 'source-repair.json') -Encoding UTF8
$summary|ConvertTo-Json -Depth 10
Write-Output ('REPAIR_EVIDENCE='+$evidence)
