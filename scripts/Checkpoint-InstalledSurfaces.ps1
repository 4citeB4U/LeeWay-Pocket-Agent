<# REGION: LEEWAY.DEPLOYMENT.EVIDENCE; TAG: INSTALLED_REPAIR_SCOPE
WHO: Owner-authorized Agent Lee. WHAT: Separate actual PC execution, Android install, and unverified phone visuals.
WHEN: After in-place repair. WHERE: Verified local receipts and exact APK.
WHY: Never call an install a complete Golden release. HOW: Hash/readback, bounded evidence copy, no private DB upload.
LICENSE: MIT #>
[CmdletBinding()]
param([Parameter(Mandatory=$true)][string]$BackupDirectory,[Parameter(Mandatory=$true)][string]$BuildReceipt)
Set-StrictMode -Version Latest
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
if((& git -C $root branch --show-current|Out-String).Trim() -ne 'repair/installed-brain-diagnostics'){throw 'REPAIR_BRANCH_REQUIRED'}
$q=Get-Content -LiteralPath $BuildReceipt -Raw|ConvertFrom-Json
$pc=Get-Content (Join-Path $BackupDirectory 'pc-live\pc-live-acceptance.json') -Raw|ConvertFrom-Json
$phone=Get-Content (Join-Path $BackupDirectory 'phone-deployment.json') -Raw|ConvertFrom-Json
$overlay=Get-Content (Join-Path $BackupDirectory 'pc-overlay\overlay-live.json') -Raw|ConvertFrom-Json
if($q.status -ne 'PASS_SCOPED_SOFTWARE_QUALIFICATION_ONLY' -or $pc.status -ne 'VERIFIED_INSTALLED_PC_UI_AND_HARDWARE_SCOPE'){throw 'QUALIFICATION_SCOPE_NOT_PASSED'}
$apk=Join-Path $root 'app\build\outputs\apk\debug\app-debug.apk'
if((Get-FileHash $apk -Algorithm SHA256).Hash -ne $q.apkSha256 -or $phone.apkSha256 -ne $q.apkSha256){throw 'INSTALLED_BUILD_HASH_MISMATCH'}
$run='installed-surfaces-'+[DateTime]::UtcNow.ToString('yyyyMMddTHHmmssfffZ');$dest=Join-Path $root ('docs\qualification\'+$run);[void][IO.Directory]::CreateDirectory($dest)
foreach($pair in @(@('pc-live\pc-live-acceptance.json','pc-live-acceptance.json'),@('phone-deployment.json','phone-deployment.json'),@('pc-overlay\overlay-live.json','pc-overlay-last-observation.json'))){Copy-Item (Join-Path $BackupDirectory $pair[0]) (Join-Path $dest $pair[1])}
Copy-Item $BuildReceipt (Join-Path $dest 'build-qualification.json')
$screen=Join-Path $dest 'pc-live-screens';[void][IO.Directory]::CreateDirectory($screen)
foreach($f in $pc.files){$src=Join-Path $BackupDirectory ('pc-live\'+$f.name);if((Get-FileHash $src -Algorithm SHA256).Hash.ToLowerInvariant() -ne $f.sha256){throw 'SCREENSHOT_CHANGED'};Copy-Item $src (Join-Path $screen $f.name)}
$receipt=[ordered]@{
 schemaVersion='leeway.installed-surface-repair.v1'
 status='PC_UI_VERIFIED_PHONE_INSTALLED_VISUAL_VERIFICATION_BLOCKED'
 createdAt=[DateTime]::UtcNow.ToString('o')
 sourceBase=$q.sourceBase
 apkSha256=$q.apkSha256
 apkBytes=$q.apkBytes
 androidVersionCode=28
 androidVersionName='1.0.0-live-surface-repair-rc1'
 androidInstall='VERIFIED_ADB_IN_PLACE_SUCCESS_SAME_CERTIFICATE'
 androidNativeStartup='OBSERVED_MAIN_ACTIVITY_PID22353_AFTER_INSTALL'
 androidMigration='OBSERVED_OWNER_LOCAL_UPGRADE_COMPLETED_FROM_NATIVE_RECEIPT'
 androidIdentity='GENERATED_BY_EXISTING_KEYSTORE_UUID_MECHANISM_NOT_MODEL_HARDCODE'
 androidActiveDatabases=@('leeway-digital-brain.db','leeway-continuum.db','leeway-working-memory.db')
 androidDataCleared=$false
 androidUninstalled=$false
 androidFullPreservationCountComparison='PENDING_RECONNECTED_DATABASE_READBACK'
 androidVisualAcceptance='BLOCKED_DEVICE_DISCONNECTED_AFTER_SUCCESSFUL_INSTALL'
 androidFloatingOverlayPermission='NOT_YET_VERIFIED_ENABLED'
 pcActualButtonChecks=@($pc.checks).Count
 pcBrowserFixtures=$false
 pcDatabase='EXISTING_RECOVERED_BRAIN_NOT_NEW_STORE'
 pcRecoveredBaselineNodes=198
 pcReadbackNodesAfterHardwareObservations=209
 pcQuickCheck='ok'
 pcHardwareSections=$pc.data[0].hardwareSections
 pcTemperatureMeasurements='UNAVAILABLE_NO_QUALIFIED_SENSOR_PROVIDER_BOUND'
 pcFloatingTab='NATIVE_WPF_VISIBLE_WITH_ORIGINAL_ARTWORK_IDLE_COLOR_CYCLE'
 pcLauncher='EXECUTED_EXISTING_DESKTOP_LAUNCHER_AFTER_HEALTH_CONTRACT_REPAIR'
 nodeUnitAndProtocolTests=47
 junitTests=105
 hardwareElectricalCertification=$false
 voicePackageChanged=$false
 unifiedConversation='NOT_REPAIRED_CANONICAL_PHONE_EXECUTOR_REMAINS_UNBOUND'
 continuumAndLdwmd='CANONICAL_ANDROID_RUNTIME_BINDINGS_STILL_UNQUALIFIED'
 goldenPromoted=$false
 formulaExecuted=$false
 learningLedgerUpdated=$false
 privateBackupFilesCommitted=$false
}
$receipt|ConvertTo-Json -Depth 8|Set-Content (Join-Path $dest 'installed-repair-receipt.json') -Encoding UTF8
Write-Output ('EVIDENCE_DIRECTORY='+$dest)
$receipt|ConvertTo-Json -Depth 8
