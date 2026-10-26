$ErrorActionPreference='Stop'
$stage='E:\LeeWay-Work\c3-vision-lane-a-qualification\qualification'
$build=Get-Content 'E:\LeeWay-Work\agent-lee-fold6-unified\qualification\fold6-build-20261010T105519218Z\build-receipt.json' -Raw | ConvertFrom-Json
$expected='839279D305A2CA7AC7042972F465ED18022B4C511025670C3FA9F590759C7B18'
$actual=(Get-FileHash (Join-Path $stage 'fold6-installed-v79-confirmed.apk')).Hash
if($actual -ne $expected -or $build.apk.sha256 -ne $expected -or $build.gradleExitCode -ne 0){throw 'EVIDENCE_MISMATCH'}
$versionText=(& adb shell dumpsys package industries.leeway.pocket | Select-String 'versionCode=|versionName=' | Select-Object -First 2 | ForEach-Object {$_.ToString().Trim()}) -join '; '
if($versionText -notmatch 'versionCode=79' -or $versionText -notmatch 'voice-vision-foreground-rc52'){throw 'INSTALLED_VERSION_MISMATCH'}
$receipt=[ordered]@{
 schemaVersion='leeway.c3.vision.physical-observation.v1'
 timestampUtc=[DateTime]::UtcNow.ToString('o')
 state='INSTALLED_APK_HASH_VERIFIED_WITH_OPEN_CAMERA_GATE'
 device='SM-F956U'
 package='industries.leeway.pocket'
 installedVersion=$versionText
 installedApkSha256=$actual
 sourceManifestSha256=$build.sourceManifestSha256
 gradleExitCode=$build.gradleExitCode
 buildAndUnitTestsPassed=($build.status -eq 'BUILD_AND_UNIT_TESTS_PASSED')
 sourceChangedDuringBuild=$build.sourceChangedDuringBuild
 observedGoldenMenuVision=$true
 observedInPackageVisionActivityLaunch=$true
 observedSecureVisionPageLoaded=$true
 observedLocalCameraCapture='FAILED_NotAllowedError_PermissionDenied'
 observedVideoWidth=0
 observedVideoHeight=0
 observedPageVisibility='hidden_after_launcher_resumed'
 pcCrossDeviceVideo='NOT_VERIFIED'
 fourCameraLive='NOT_VERIFIED'
 fiftyPositionDeployed='NOT_VERIFIED'
 independentVeritas='NOT_EXECUTED'
 formulaExecution='NOT_EXECUTED'
 learningLedgerUpdated=$false
 priorRollbackApk=(Join-Path $stage 'fold6-installed-v78-voice-rollback.apk')
 notes='Only machine-observed facts. WebView camera permission failed with Activity hidden; no camera frame proof. Source/OS policy remains fail closed.'
}
$target=Join-Path $stage 'c3-vision-v79-physical-observation-receipt.json'
$receipt|ConvertTo-Json -Depth 6|Set-Content -LiteralPath $target -Encoding UTF8
[pscustomobject]@{receipt=$target;receiptSha256=(Get-FileHash $target).Hash;installedApkSha256=$actual;status=$receipt.state}|ConvertTo-Json
