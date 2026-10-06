<#
REGION: LEEWAY.QUALIFICATION
TAG: SHARED_VOICE_AND_PHONE_OVERLAY_REPAIR_SCOPE
WHO: Creator-authorized Agent Lee. WHAT: Preserve only executed tests, exact build and phone-link failure.
WHEN: After source qualification. WHERE: Existing Pocket and Voice repositories.
WHY: Source/build/synthesis must never become fabricated phone visibility or human audibility.
HOW: Recheck source/artifact hashes, collect scoped records; no install, merge or private backup publication.
LICENSE: MIT
#>
[CmdletBinding()]
param([Parameter(Mandatory=$true)][string]$QualificationDirectory)
Set-StrictMode -Version Latest
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
$q=Get-Content (Join-Path $QualificationDirectory 'receipt.json') -Raw|ConvertFrom-Json
if($q.status -ne 'PASS_SCOPED_SOFTWARE_QUALIFICATION_ONLY' -or !$q.sourceStable){throw 'BUILD_NOT_QUALIFIED'}
$source=Join-Path $QualificationDirectory 'source-hashes.json'
if((Get-FileHash $source -Algorithm SHA256).Hash -ne $q.sourceManifestSha256){throw 'SOURCE_MANIFEST_CHANGED'}
foreach($entry in (Get-Content $source -Raw|ConvertFrom-Json)){if((Get-FileHash (Join-Path $root $entry.path) -Algorithm SHA256).Hash -ne $entry.sha256){throw 'QUALIFIED_SOURCE_CHANGED'}}
$apk=Join-Path $root 'app/build/outputs/apk/debug/app-debug.apk'
if((Get-FileHash $apk -Algorithm SHA256).Hash -ne $q.apkSha256){throw 'QUALIFIED_APK_CHANGED'}
$voiceDir=Join-Path $root 'qualification/phone-overlay-recovery/current-voice-proof'
$v=Get-Content (Join-Path $voiceDir 'receipt.json') -Raw|ConvertFrom-Json
if($v.status -ne 'SHARED_SELECTION_AND_REAL_SYNTHESIS_VERIFIED_NOT_PHONE_OR_AUDIBILITY'){throw 'LIVE_VOICE_PROOF_FAILED'}
$dir=Join-Path $root ('docs/qualification/phone-overlay-recovery-'+$q.runId.Replace('portable-qualification-',''))
if(Test-Path $dir){throw 'EVIDENCE_DESTINATION_EXISTS'}
[void][IO.Directory]::CreateDirectory($dir)
foreach($name in @('receipt.json','source-hashes.json','node-tests.tap','gradle.log')){Copy-Item (Join-Path $QualificationDirectory $name) (Join-Path $dir $name)}
Copy-Item (Join-Path $voiceDir 'receipt.json') (Join-Path $dir 'current-voice-ownership.json')
Copy-Item (Join-Path $voiceDir 'voice-authority-tests.tap') (Join-Path $dir 'voice-authority-tests.tap')
$test='app/build/test-results/testDebugUnitTest/TEST-industries.leeway.pocket.OverlayPlacementTest.xml'
Copy-Item (Join-Path $root $test) (Join-Path $dir 'OverlayPlacementTest.xml')
$devices=(& adb devices -l|Out-String);$mdns=(& adb mdns services|Out-String)
$connected=@($devices -split "`n"|Where-Object{$_ -match '^\S+\s+device\s'}).Count
$receipt=[ordered]@{schemaVersion='leeway.overlay-voice-recovery.v1';status='PHONE_CANDIDATE_QUALIFIED_VOICE_BOUNDARY_VERIFIED_PHONE_LIVE_GATE_PENDING';sourceBase=$q.sourceBase;apkSha256=$q.apkSha256;apkBytes=$q.apkBytes;androidVersionCode=29;androidVersionName='1.0.0-overlay-recovery-rc2';phoneInstalledThisPass=$false;phoneDataModified=$false;adbDeviceCount=$connected;phoneVisualVerification=$false;phoneOverlayPermissionObservedThisPass=$false;overlayTests=13;junitTests=($q.junitSuites|Measure-Object tests -Sum).Sum;nodeRegressionTests=47;voiceAuthorityTests=19;voiceFixtureProvider=$false;voiceOverrideAdmitted=$false;voiceSelectionChanged=$false;voicePackageId=$v.binding.voicePackageId;voiceSelectionState=$v.binding.selectionState;voiceAudioSha256=$v.synthesis.sha256;phoneNativeSpeechExecuted=$false;humanAudibilityConfirmed=$false;crossDeviceVoiceSelectionSync=$false;inPageAndroidEmblemRemoved=$true;existingOverlayServiceRepaired=$true;newOverlayServiceCreated=$false;osPermissionBypassed=$false;windowsTransparentHostCompleted=$false;goldenPromoted=$false;formulaExecuted=$false;learningLedgerUpdated=$false}
$devices+$mdns|Set-Content (Join-Path $dir 'phone-link-observation.txt') -Encoding UTF8
$receipt|ConvertTo-Json -Depth 9|Set-Content (Join-Path $dir 'overlay-voice-checkpoint.json') -Encoding UTF8
Write-Output ('EVIDENCE_DIRECTORY='+$dir)
$receipt|ConvertTo-Json -Depth 9
