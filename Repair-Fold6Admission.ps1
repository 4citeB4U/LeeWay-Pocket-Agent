<#
REGION: LEEWAY.POCKET.REPAIR
TAG: FOLD6-VOICE-AND-ADMISSION-REPAIR
WHO: Creator-authorized Agent Lee
WHAT: Repair the inspected voice identity, native transparency and false readiness boundaries.
WHEN: Single-APK qualification; WHERE: existing Pocket candidate checkout only.
WHY: Do not install a compiling but disconnected candidate or silently change Agent Lee's voice.
HOW: Match inspected hashes, preserve backup lineage, make exact edits, copy verified voice source and receipt.
LICENSE: MIT
#>
[CmdletBinding()]
param()
Set-StrictMode -Version Latest
$ErrorActionPreference='Stop'
$root=$PSScriptRoot
if((& git -C $root remote get-url origin | Out-String).Trim() -ne 'https://github.com/4citeB4U/LeeWay-Pocket-Agent.git'){throw 'POCKET_AUTHORITY_MISMATCH'}
$prefix='app\src\main\java\industries\leeway\pocket\'
$expected=[ordered]@{
 ($prefix+'PocketVoiceHost.kt')='FFA0192D1D49524022CF178F0B3B2B791F7A0E8369761F1A6D188C5AB47C467A'
 ($prefix+'UnifiedAgentLeeRuntime.kt')='9FF058E18F87C8C18CC791A608E3AC6555B3F1781DB26EDB91AED8FA3A541A48'
 ($prefix+'PocketVoiceActivity.kt')='B64263299820BDD57729502F38EA5CC977C07DD1BA46C956C04635CC12367E73'
 'app\src\main\res\values\styles.xml'='059F42D6C6504B715043F96835E4631CE788D724B567AA9B96F02EC01498BADB'
 'Verify-SingleApk.ps1'='1FDAF680E3BEB7E173D2DC5A18B251AAE75E0DA88F6C64ED34DFC1F21E248E1E'
}
foreach($rel in $expected.Keys){if((Get-FileHash -LiteralPath (Join-Path $root $rel) -Algorithm SHA256).Hash -ne $expected[$rel]){throw ('SOURCE_CHANGED_REVIEW_REQUIRED:'+ $rel)}}
$utf8=New-Object System.Text.UTF8Encoding($false)
function Replace-Once([string]$text,[string]$old,[string]$new){
 $text=$text.Replace("`r`n","`n");$old=$old.Replace("`r`n","`n");$new=$new.Replace("`r`n","`n")
 if(([regex]::Matches($text,[regex]::Escape($old))).Count -ne 1){throw ('REPAIR_BOUNDARY_AMBIGUOUS:'+ $old.Substring(0,[Math]::Min($old.Length,80)))}
 return $text.Replace($old,$new)
}
$voicePath=Join-Path $root ($prefix+'PocketVoiceHost.kt')
$voice=[IO.File]::ReadAllText($voicePath)
$old=@'
        if(selectionPrefs?.getBoolean("fabric_english_default_v1",false)!=true){
            selectionPrefs?.edit()?.putString("selected_id","android-installed-english")?.putBoolean("fabric_english_default_v1",true)?.apply()
        }
        requestedVoiceId=selectionPrefs?.getString("selected_id","android-installed-english") ?: "android-installed-english"
'@
$new=@'
        val binding = try {
            AgentVoiceBinding.fromSources(
                JSONObject(context.assets.open("voice/employee-voice-bindings.v1.json").bufferedReader().use { it.readText() }),
                JSONObject(context.assets.open("voice/catalog.v1.json").bufferedReader().use { it.readText() }),
                allowQualification = BuildConfig.DEBUG
            )
        } catch (error: Exception) {
            readiness.unavailable()
            record("VOICE_BINDING_BLOCKED", error.message ?: "VOICE_SELECTION_REQUIRED")
            listener.onError(lastError)
            return
        }
        activeBinding=binding
        requestedVoiceId=binding.voicePackageId
        if(binding.qualificationOnly) listener.onState("TEMPORARY_SHARED_VOICE_FOR_QUALIFICATION")
'@
$voice=Replace-Once $voice $old $new
$voice=Replace-Once $voice '    private var requestedVoiceId="agent-lee-voice-one"' '    private var requestedVoiceId=""
    private var activeBinding:AgentVoiceBinding?=null'
$voice=Replace-Once $voice '                        requestedVoiceId=choices[index].id;requestSelection();dialog.dismiss()' '                        dialog.dismiss()
                        if(choices[index].id==requestedVoiceId) requestSelection()
                        else android.app.AlertDialog.Builder(activity).setTitle("One Agent Lee voice")
                            .setMessage("Voice identity is shared with the PC. A device-local selection cannot replace the shared LeeWay Voice binding.")
                            .setPositiveButton("Close",null).show()'
$admit=@'
    private fun admitRenderer(payload:JSONObject):Boolean {
        val binding=activeBinding ?: return false
        return try {
            binding.requireRenderer(payload.optString("voicePackageId"),payload.optString("provider"))
            true
        } catch(error:IllegalArgumentException) {
            selectionConfirmed=false;readiness.unavailable()
            record("VOICE_IDENTITY_REJECTED",error.message ?: "VOICE_IDENTITY_MISMATCH")
            session.owner?.onError(lastError)
            false
        }
    }

'@
$voice=Replace-Once $voice '    private fun dispatch() {' ($admit+'    private fun dispatch() {')
$voice=Replace-Once $voice '            selectedVoiceId=requestedVoiceId;selectedVoiceName=VoiceProgress.safe(payload.optString("name",selectedVoiceId))' '            if(!admitRenderer(payload))return@deliver
            selectedVoiceId=requestedVoiceId;selectedVoiceName=VoiceProgress.safe(payload.optString("name",selectedVoiceId))'
$voice=Replace-Once $voice '            if(!selectionConfirmed||payload?.optString("voicePackageId")!=requestedVoiceId)return@deliver' '            if(!selectionConfirmed||payload?.optString("voicePackageId")!=requestedVoiceId)return@deliver
            if(payload==null||!admitRenderer(payload))return@deliver'
$stylePath=Join-Path $root 'app\src\main\res\values\styles.xml'
$style=[IO.File]::ReadAllText($stylePath)
$style=Replace-Once $style '        <item name="android:statusBarColor">#000000</item>' '        <item name="android:windowIsTranslucent">true</item>
        <item name="android:windowBackground">@android:color/transparent</item>
        <item name="android:windowIsFloating">false</item>
        <item name="android:backgroundDimEnabled">false</item>
        <item name="android:statusBarColor">@android:color/transparent</item>'
$style=Replace-Once $style '        <item name="android:navigationBarColor">#000000</item>' '        <item name="android:navigationBarColor">@android:color/transparent</item>'
$runtime=@'
/*
REGION: POCKET.RUNTIME.ADAPTER
TAG: LEEWAY_CANONICAL_CONVERSATION_BINDING
WHO: Agent Lee / Creator-authorized Android body
WHAT: Reserve the existing adapter boundary for canonical conversation execution.
WHEN: During integration; WHERE: Pocket Android candidate.
WHY: A canned status sentence is not a connected consciousness or a conversational answer.
HOW: Fail explicitly until the canonical executor is linked; do not create a parallel reasoner.
LICENSE: MIT
*/
package industries.leeway.pocket

import android.content.Context

class UnifiedAgentLeeRuntime(private val context:Context) {
    fun respond(request:String):String {
        require(request.isNotBlank()) { "CONVERSATION_REQUEST_REQUIRED" }
        throw IllegalStateException("CANONICAL_CONVERSATION_PROVIDER_NOT_BOUND")
    }
}
'@
$activityPath=Join-Path $root ($prefix+'PocketVoiceActivity.kt')
$activity=[IO.File]::ReadAllText($activityPath)
$oldHandle='    private fun handle(q:String){appendContinuum("user",q);transcript.text="You: "+q;val reply=UnifiedAgentLeeRuntime(this).respond(q);appendContinuum("agent-lee",reply);transcript.text="Agent Lee: "+reply;status.text="Speaking";PocketSpeech.speak(voiceListener,reply)}'
$newHandle=@'
    private fun handle(q:String){
        appendContinuum("user",q)
        transcript.text="You: "+q
        val reply=try { UnifiedAgentLeeRuntime(this).respond(q) }
        catch(error:IllegalStateException){
            status.text="Conversation connection blocked"
            transcript.text="The canonical Agent Lee conversation provider is not bound in this candidate. No answer or execution is being claimed."
            return
        }
        appendContinuum("agent-lee",reply)
        transcript.text="Agent Lee: "+reply
        status.text="Speaking"
        PocketSpeech.speak(voiceListener,reply)
    }
'@
$activity=Replace-Once $activity $oldHandle $newHandle
$gatePath=Join-Path $root 'Verify-SingleApk.ps1'
$gate=[IO.File]::ReadAllText($gatePath)
$gate=Replace-Once $gate '$ErrorActionPreference=''Stop''' "param([switch]`$SourceOnly)`n`$ErrorActionPreference='Stop'"
$gate=Replace-Once $gate "'LEEWAY_SINGLE_APK_COMPLETENESS_PASS'" "'LEEWAY_SINGLE_APK_SOURCE_STRUCTURE_PASS_NOT_RUNTIME_ACCEPTANCE'`nif(-not `$SourceOnly){throw 'FULL_APK_RUNTIME_ACCEPTANCE_NOT_PROVEN'}"
$sourceVoice=Join-Path (Split-Path $root -Parent) 'pc-consciousness-live\voice'
$catalogSource=Join-Path $sourceVoice 'voices\catalog.v1.json'
if((Get-FileHash $catalogSource -Algorithm SHA256).Hash -ne '2E56FD4390757B7AD76C44F0CA42D03D5EBF6F9C6507A9A2FBCA5EAFF7F5F298'){throw 'VOICE_CATALOG_SOURCE_CHANGED'}
$bindingSource=Join-Path $sourceVoice 'runtime\employee-voice-bindings.v1.json'
$binding=Get-Content $bindingSource -Raw|ConvertFrom-Json
if($binding.bindings.'agent-lee'.state -ne 'TEMPORARY_VERIFIED_PROVIDER' -or $binding.bindings.'agent-lee'.voicePackageId -ne 'kokoro-am_michael'){throw 'VOICE_BINDING_REVIEW_REQUIRED'}
# All replacements have been validated in memory before source mutation.
[IO.File]::WriteAllText($voicePath,$voice,$utf8)
[IO.File]::WriteAllText($stylePath,$style,$utf8)
[IO.File]::WriteAllText((Join-Path $root ($prefix+'UnifiedAgentLeeRuntime.kt')),$runtime,$utf8)
[IO.File]::WriteAllText($activityPath,$activity,$utf8)
[IO.File]::WriteAllText($gatePath,$gate,$utf8)
$assetDir=Join-Path $root 'app\src\main\assets\voice'
$null=New-Item -ItemType Directory -Force -Path $assetDir
Copy-Item -LiteralPath $catalogSource -Destination (Join-Path $assetDir 'catalog.v1.json')
Copy-Item -LiteralPath $bindingSource -Destination (Join-Path $assetDir 'employee-voice-bindings.v1.json')
$lock=[ordered]@{schemaVersion='leeway.voice.source-projection.v1';authority='4citeB4U/LeeWay-Voice-Fabric';body='phone-fold6';sourceBody='pc-primary';mode='QUALIFICATION_ONLY';creatorFinalVoiceSelection=$false;catalogSha256=(Get-FileHash $catalogSource -Algorithm SHA256).Hash;bindingSha256=(Get-FileHash $bindingSource -Algorithm SHA256).Hash;acousticEquivalence='NOT_TESTED';providerRuntimePackaged=$false}
$lock|ConvertTo-Json|Set-Content (Join-Path $assetDir 'source-lock.json') -Encoding UTF8
$after=@(foreach($rel in $expected.Keys){[ordered]@{path=$rel;beforeSha256=$expected[$rel];afterSha256=(Get-FileHash (Join-Path $root $rel) -Algorithm SHA256).Hash}})
$receipt=Join-Path $root ('qualification\source-repair-'+[DateTime]::UtcNow.ToString('yyyyMMddTHHmmssfffZ')+'.json')
[ordered]@{status='SOURCE_REPAIRED_TESTS_PENDING';changes=$after;voiceSource=$lock;installed=$false;formulaExecution='NOT_EXECUTED';learningLedgerUpdated=$false}|ConvertTo-Json -Depth 8|Set-Content $receipt -Encoding UTF8
Write-Output ('REPAIR_RECEIPT='+$receipt)
