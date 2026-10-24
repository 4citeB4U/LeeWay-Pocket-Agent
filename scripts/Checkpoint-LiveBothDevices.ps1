<#
REGION: LEEWAY.LIVE.QUALIFICATION
TAG: INSTALLED_NATIVE_OVERLAYS_AND_PHONE_SPEAKER
WHO: Creator-authorized Agent Lee. WHAT: Record actual installed checks, not just candidate build results.
WHEN: After USB and native-window execution. WHERE: existing evidence paths, no private database publication.
WHY: Installed, visible, interactive, audible-to-owner and Golden are different claims.
HOW: Exact APK/native hashes, native output route, preserved IDs and scoped receipt. LICENSE: MIT
#>
[CmdletBinding()]
param([Parameter(Mandatory=$true)][string]$RunRoot,[Parameter(Mandatory=$true)][string]$BodyRoot,[Parameter(Mandatory=$true)][string]$NativeHost,[Parameter(Mandatory=$true)][string]$BuildReceipt)
Set-StrictMode -Version Latest;$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
if((& git -C $root branch --show-current|Out-String).Trim() -ne 'repair/floating-agent-interaction'){throw 'REPAIR_BRANCH_CHANGED'}
$q=Get-Content $BuildReceipt -Raw|ConvertFrom-Json
$phone=Get-Content (Join-Path $RunRoot 'phone-live\phone-live-receipt.json') -Raw|ConvertFrom-Json
$pc=Get-Content (Join-Path $RunRoot 'pc-native-converged\pc-native-live-receipt.json') -Raw|ConvertFrom-Json
$owner=Get-Content (Join-Path $RunRoot 'phone-owner-preservation.json') -Raw|ConvertFrom-Json
$hostState=Get-Content (Join-Path $BodyRoot 'receipts\native-agent-ui\host-receipt.json') -Raw|ConvertFrom-Json
$tab=Get-Content (Join-Path $BodyRoot 'receipts\native-agent-ui\external-tab\overlay-live.json') -Raw|ConvertFrom-Json
$binding=Get-Content (Join-Path $BodyRoot 'host-commander.binding.v1.json') -Raw|ConvertFrom-Json
if($phone.status -ne 'VERIFIED_INSTALLED_PHONE_SCOPED_UI_AND_OUTPUT' -or @($phone.errors).Count){throw 'ACTUAL_PHONE_ACCEPTANCE_REQUIRED'}
if($pc.status -ne 'VERIFIED_NATIVE_WINDOW_AND_INTERACTION' -or @($pc.errors).Count){throw 'ACTUAL_PC_ACCEPTANCE_REQUIRED'}
if($q.status -ne 'PASS_SCOPED_SOFTWARE_QUALIFICATION_ONLY' -or !$q.sourceStable){throw 'BUILD_QUALIFICATION_REQUIRED'}
$apk=Join-Path $root 'app\build\outputs\apk\debug\app-debug.apk'
if((Get-FileHash $apk -Algorithm SHA256).Hash -ne $q.apkSha256 -or $owner.installedApkSha256.ToUpperInvariant() -ne $q.apkSha256){throw 'INSTALLED_APK_HASH_MISMATCH'}
if(!$owner.sameOwnerIdentity -or $owner.missingPriorNodeIds -ne 0){throw 'OWNER_DATA_PRESERVATION_FAILED'}
if(!$tab.visible -or $tab.state -ne 'NATIVE_WPF_TAB_RUNNING'){throw 'EXTERNAL_PC_TAB_NOT_RUNNING'}
if((Get-FileHash $NativeHost -Algorithm SHA256).Hash -ne $binding.nativeAgentUi.sha256){throw 'NATIVE_LAUNCHER_HASH_BINDING_MISMATCH'}
$playing=@($phone.checks|Where-Object{$_.PSObject.Properties.Name -contains 'playing'})[0].playing
$completed=@($phone.checks|Where-Object{$_.PSObject.Properties.Name -contains 'completed'})[0].completed
if($playing.actualOutputType -ne 2 -or !$playing.nativeIsPlaying -or $completed.state -ne 'NATIVE_PLAYBACK_COMPLETED'){throw 'ACTUAL_PHONE_SPEAKER_PROOF_REQUIRED'}
$now=[DateTime]::UtcNow.ToString('yyyyMMddTHHmmssfffZ');$dest=Join-Path $root ('docs\qualification\live-both-'+$now)
[void][IO.Directory]::CreateDirectory($dest)
foreach($pair in @(@('phone-live\phone-live-receipt.json','phone-live-receipt.json'),@('pc-native-converged\pc-native-live-receipt.json','pc-native-live-receipt.json'),@('phone-owner-preservation.json','owner-preservation.json'),@('phone-deployment.json','phone-install.json'),@('phone-voice-stage.json','voice-stage.json'),@('voice-reused-client-tests.tap','voice-reused-client-tests.tap'))){Copy-Item -LiteralPath (Join-Path $RunRoot $pair[0]) -Destination (Join-Path $dest $pair[1])}
Copy-Item -LiteralPath $BuildReceipt -Destination (Join-Path $dest 'build-qualification.json')
$receipt=[ordered]@{
 schemaVersion='leeway.installed-both-device-repair.v1'
 status='VERIFIED_SCOPED_INSTALLED_OVERLAYS_AND_PHONE_AUDIO_NOT_GOLDEN'
 capturedAt=[DateTime]::UtcNow.ToString('o')
 phoneInstalledVersionCode=30
 phoneInstalledVersionName='1.0.0-floating-live-rc3'
 phoneInstalledApkSha256=$q.apkSha256
 phoneInstalledApkBytes=$q.apkBytes
 phoneLiveChecks=@($phone.checks).Count
 pcNativeLiveChecks=@($pc.checks).Count
 pcInputTestMethod=$pc.inputMethod
 phoneUiFixture=$false
 pcBrowserFixture=$false
 oldPhoneVersion=28
 oldPcBrowserLauncherReplaced=$true
 pcLauncherSha256=(Get-FileHash (Join-Path $BodyRoot 'Start-Agent-Lee.ps1') -Algorithm SHA256).Hash
 pcNativeHostSha256=(Get-FileHash $NativeHost -Algorithm SHA256).Hash
 pcFloatingHostPid=$hostState.pid
 pcExternalTabPid=$tab.pid
 pcExternalTabVisible=$tab.visible
 pcCompactGeometry=$hostState.geometry
 phoneOriginalNodes=$owner.priorNodes
 phonePreservedNodes=$owner.currentNodes
 phonePriorNodesLost=$owner.missingPriorNodeIds
 phoneOwnerIdentityPreserved=$owner.sameOwnerIdentity
 phoneDatabaseIntegrity=$owner.databaseIntegrity
 phoneBuiltInSpeakerPlayed=$true
 voicePackageId=$playing.voicePackageId
 voiceProvider=$playing.provider
 voiceSelectionRevision=$playing.selectionRevision
 waveformSha256=$playing.audioSha256
 actualAudioOutputName=$playing.actualOutputName
 actualAudioOutputType=$playing.actualOutputType
 playbackDurationMs=$playing.durationMs
 playedPositionMs=$completed.playedPositionMs
 volumeChanged=$false
 systemTtsUsed=$false
 humanAudibilityConfirmed=$false
 noUninstallOrDataClear=$true
 appRestartTested=$true
 physicalRebootTested=$false
 microphoneConversationQualification=$false
 phoneCanonicalConversationExecutor='NOT_BOUND'
 voiceSelectionFederation='NOT_QUALIFIED'
 canonicalContinuumAndLdwmdRuntime='NOT_QUALIFIED_FOR_FULL_PRODUCT'
 storageIncident='E_VOLUME_TEMPORARILY_UNAVAILABLE_THEN_SAME_REPOSITORIES_RECOVERED; ROOT_CAUSE_NOT_ESTABLISHED'
 initialNativePointerFailure='RECORDED; CORRECTED_WITH_OWN_SHAPED_NATIVE_INPUT_PLANE_AND_RETESTED'
 privateDatabaseBackupsPublished=$false
 screenshotsRemainLocalWithRecordedHashes=$true
 fullGoldenRelease=$false
 formulaExecuted=$false
 learningLedgerUpdated=$false
}
$receipt|ConvertTo-Json -Depth 10|Set-Content -LiteralPath (Join-Path $dest 'installed-both-receipt.json') -Encoding UTF8
$receipt|ConvertTo-Json -Depth 10
Write-Output ('CANONICAL_EVIDENCE_DIRECTORY='+$dest)
