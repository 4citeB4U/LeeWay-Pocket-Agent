<#
REGION: LEEWAY.POCKET.QUALIFICATION
TAG: FOLD6.ROUND_BOX.BUILD_GATE
5WH: WHAT=Compile/test the exact Round Box addition against canonical Pocket v45; WHY=Fail closed before device mutation;
WHO=LeeWay Industries/owner; WHERE=existing Pocket Agent staging worktree; WHEN=explicit owner-authorized integration;
HOW=check source SHA, Gradle offline build, unit tests, captured log and receipt.
LICENSE: MIT
#>
Set-StrictMode -Version Latest
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
$head=(& git -C $root rev-parse HEAD).Trim()
if($head -ne 'f5956a85d0d347f166c0fbb187f999af9ad30cd2'){throw 'POCKET_V45_AUTHORITY_MISMATCH'}
$workRoot=Split-Path (Split-Path $root -Parent) -Parent
$gradle=Join-Path $workRoot 'tool-cache\gradle-8.9\bin\gradle.bat'
$sdk=Join-Path $env:LOCALAPPDATA 'Android\Sdk'
if(!(Test-Path $gradle)){throw 'GRADLE_89_MISSING'}
if(!(Test-Path (Join-Path $sdk 'platforms\android-35\android.jar'))){throw 'ANDROID_35_SDK_MISSING'}
$env:ANDROID_HOME=$sdk
$env:ANDROID_SDK_ROOT=$sdk
$run='roundbox-v45-'+[DateTime]::UtcNow.ToString('yyyyMMddTHHmmssZ')
$dest=Join-Path $root ('qualification\'+$run)
$null=New-Item -ItemType Directory -Force -Path $dest
$paths=@(
'app\src\main\assets\round-box-leeway.html',
'app\src\main\java\industries\leeway\pocket\FloatingRoundBoxWindow.kt',
'app\src\main\java\industries\leeway\pocket\PocketOverlayService.kt',
'app\src\main\java\industries\leeway\pocket\FloatingSphereWindow.kt',
'app\src\main\java\industries\leeway\pocket\MainActivity.kt'
)
$hashes=@($paths | ForEach-Object { $p=Join-Path $root $_; [ordered]@{path=$_;sha256=(Get-FileHash -LiteralPath $p -Algorithm SHA256).Hash;bytes=(Get-Item $p).Length} })
$hashes | ConvertTo-Json -Depth 5 | Set-Content (Join-Path $dest 'sources.json') -Encoding UTF8
$oldApk=Join-Path (Split-Path $root -Parent) 'phone-pocket-recovery\pocket-v45-before-roundbox.apk'
$oldHash=(Get-FileHash $oldApk -Algorithm SHA256).Hash
$log=Join-Path $dest 'gradle.log'
$started=[DateTime]::UtcNow.ToString('o')
Push-Location $root
try{
  $ErrorActionPreference='Continue'
  & $gradle ':app:assembleDebug' ':app:testDebugUnitTest' '--offline' '--no-daemon' '--console=plain' *> $log
  $exitCode=$LASTEXITCODE
} finally{Pop-Location;$ErrorActionPreference='Stop'}
$apk=Join-Path $root 'app\build\outputs\apk\debug\app-debug.apk'
$apkHash=if(Test-Path $apk){(Get-FileHash $apk -Algorithm SHA256).Hash}else{$null}
$changed=@($hashes | Where-Object{(Get-FileHash -LiteralPath (Join-Path $root $_.path) -Algorithm SHA256).Hash -ne $_.sha256})
$receipt=[ordered]@{
 schema='leeway.pocket.roundbox.build.v1';baseCommit=$head;runId=$run;startedAt=$started;finishedAt=[DateTime]::UtcNow.ToString('o');
 gradleExitCode=$exitCode;sourceChangedDuringBuild=$changed.Count;
 status=$(if($exitCode -eq 0 -and $apkHash -and $changed.Count -eq 0){'BUILD_AND_TEST_PASS'}else{'FAILED'});
 oldInstalledApkSha256=$oldHash;candidateApkSha256=$apkHash;candidateApk=$apk;
 logSha256=(Get-FileHash $log -Algorithm SHA256).Hash;deviceInstall='NOT_EXECUTED';deviceVisualGate='NOT_VERIFIED';
 formulaExecution='NOT_EXECUTED';nativeVeritas='NOT_RUN';learningLedgerUpdated=$false
}
$receipt | ConvertTo-Json -Depth 8 | Set-Content (Join-Path $dest 'receipt.json') -Encoding UTF8
Write-Output ('RECEIPT='+ (Join-Path $dest 'receipt.json'))
Write-Output ('BUILD_STATUS='+ $receipt.status)
Write-Output ('EXIT='+$exitCode)
Get-Content $log -Tail 55
if($exitCode -ne 0 -or $changed.Count -gt 0){exit 1}
