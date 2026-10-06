<#
REGION: LEEWAY.DEPLOYMENT.REPAIR
TAG: IN_PLACE_SURFACE_REPAIR_NOT_GOLDEN_PROMOTION
WHO: Creator-authorized Agent Lee. WHAT: Apply the inspected UI/diagnostics repair to actual installed targets.
WHEN: Only after backup, build and signature checks. WHERE: explicit existing carrier and selected Android owner.
WHY: Source-only progress does not fix the application. HOW: Bounded restart/in-place install, never uninstall/clear.
LICENSE: MIT
#>
[CmdletBinding()]
param([Parameter(Mandatory=$true)][ValidateSet('Pc','Phone')][string]$Target,[Parameter(Mandatory=$true)][string]$BackupDirectory,[string]$PcRuntime,[string]$PhoneSerial,[string]$BuildReceipt,[switch]$Apply)
Set-StrictMode -Version Latest
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
if((& git -C $root branch --show-current|Out-String).Trim() -ne 'repair/installed-brain-diagnostics'){throw 'REPAIR_BRANCH_REQUIRED'}
$pre=Get-Content (Join-Path $BackupDirectory 'preflight.json') -Raw|ConvertFrom-Json
if($Target -eq 'Pc'){
 if(!$PcRuntime){throw 'PC_RUNTIME_BINDING_REQUIRED'}
 $proof=Join-Path $BackupDirectory 'pc-before\pc-repair.json'
 $r=Get-Content $proof -Raw|ConvertFrom-Json
 foreach($prop in $r.after.PSObject.Properties){if((Get-FileHash -LiteralPath $prop.Name -Algorithm SHA256).Hash.ToLowerInvariant() -ne $prop.Value){throw 'PC_REPAIR_SOURCE_CHANGED'}}
 $owner=Get-NetTCPConnection -LocalPort 8890 -State Listen|Select-Object -First 1
 $process=Get-CimInstance Win32_Process -Filter ('ProcessId='+$owner.OwningProcess)
 if($process.Name -ne 'node.exe' -or $process.CommandLine -notmatch 'machine-consciousness[\\/]carrier-server.mjs'){throw 'LIVE_CARRIER_OWNER_MISMATCH'}
 $plan=[ordered]@{target='EXISTING_PC_CARRIER';pid=$process.ProcessId;source=(Join-Path $PcRuntime 'machine-consciousness\carrier-server.mjs');newService=$false;goldenPromoted=$false}
 if(!$Apply){$plan|ConvertTo-Json;return}
 Stop-Process -Id $process.ProcessId
 $p=Start-Process -FilePath (Get-Command node).Source -ArgumentList 'machine-consciousness/carrier-server.mjs' -WorkingDirectory $PcRuntime -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $BackupDirectory 'pc-carrier.stdout.log') -RedirectStandardError (Join-Path $BackupDirectory 'pc-carrier.stderr.log')
 Start-Sleep -Seconds 2
 if($p.HasExited){throw 'REPAIRED_CARRIER_START_FAILED_RESTORE_BACKUP'}
 $reply=Invoke-RestMethod 'http://127.0.0.1:8890/brain/query' -Method Post -ContentType 'application/json' -Body '{"operation":"root","arguments":{}}' -TimeoutSec 20
 if(!$reply.ok){throw 'LIVE_PC_BRAIN_QUERY_FAILED'}
 $plan['newPid']=$p.Id;$plan['rootId']=$reply.result.node.id;$plan['source']=$reply.source;$plan['status']='PC_LIVE_BRAIN_BOUND_UI_TEST_PENDING'
 $plan|ConvertTo-Json -Depth 6|Set-Content (Join-Path $BackupDirectory 'pc-deployment.json') -Encoding UTF8
 $plan|ConvertTo-Json -Depth 6
}else{
 if(!$PhoneSerial -or !$BuildReceipt){throw 'PHONE_TARGET_AND_QUALIFIED_BUILD_REQUIRED'}
 $q=Get-Content $BuildReceipt -Raw|ConvertFrom-Json
 if($q.status -ne 'PASS_SCOPED_SOFTWARE_QUALIFICATION_ONLY' -or !$q.sourceStable){throw 'SCOPED_BUILD_QUALIFICATION_REQUIRED'}
 $apk=Join-Path $root 'app\build\outputs\apk\debug\app-debug.apk'
 if((Get-FileHash $apk -Algorithm SHA256).Hash -ne $q.apkSha256){throw 'APK_NOT_THE_QUALIFIED_BUILD'}
 $old=Join-Path $BackupDirectory 'installed.apk'
 if((Get-FileHash $old -Algorithm SHA256).Hash.ToLowerInvariant() -ne $pre.installedApkSha256){throw 'ROLLBACK_APK_CHANGED'}
 if((Get-FileHash (Join-Path $BackupDirectory 'private-state.tar') -Algorithm SHA256).Hash.ToLowerInvariant() -ne $pre.privateBackupSha256){throw 'OWNER_STATE_BACKUP_CHANGED'}
 $sdk=Join-Path $env:LOCALAPPDATA 'Android\Sdk';$signer=Get-ChildItem (Join-Path $sdk 'build-tools') -Recurse -File -Filter apksigner.bat|Sort-Object FullName -Descending|Select-Object -First 1 -ExpandProperty FullName
 if(!$signer){throw 'APK_SIGNER_VERIFIER_UNAVAILABLE'}
 $oldResult=& $signer verify --print-certs $old;if($LASTEXITCODE -ne 0){throw 'INSTALLED_SIGNATURE_INVALID'}
 $newResult=& $signer verify --print-certs $apk;if($LASTEXITCODE -ne 0){throw 'REPAIR_SIGNATURE_INVALID'}
 $oldCert=($oldResult|Select-String '^Signer #1 certificate SHA-256 digest:').Line
 $newCert=($newResult|Select-String '^Signer #1 certificate SHA-256 digest:').Line
 if(!$oldCert -or $oldCert -ne $newCert){throw 'IN_PLACE_SIGNING_IDENTITY_MISMATCH'}
 $adb=(Get-Command adb).Source
 $actual=& $adb -s $PhoneSerial shell dumpsys package industries.leeway.pocket
 if(!($actual|Select-String 'versionCode=26 ')){throw 'INSTALLED_VERSION_CHANGED_REVIEW_REQUIRED'}
 $plan=[ordered]@{target='EXISTING_ANDROID_APPLICATION';package='industries.leeway.pocket';apkSha256=$q.apkSha256;certificate=$newCert;ownerStateBackedUp=$true;uninstall=$false;clearData=$false;goldenPromoted=$false;scope='OWNER_REQUESTED_UI_AND_DIAGNOSTICS_REPAIR_NOT_COMPLETE_GOLDEN_PACKAGE'}
 if(!$Apply){$plan|ConvertTo-Json;return}
 & $adb -s $PhoneSerial install --user 0 -r $apk
 if($LASTEXITCODE -ne 0){throw 'IN_PLACE_INSTALL_FAILED_NO_UNINSTALL_ATTEMPTED'}
 & $adb -s $PhoneSerial shell am start --user 0 -n industries.leeway.pocket/.MainActivity -a android.intent.action.MAIN -c android.intent.category.LAUNCHER
 if($LASTEXITCODE -ne 0){throw 'PHONE_LAUNCH_FAILED'}
 $plan['status']='IN_PLACE_REPAIR_INSTALLED_LIVE_ACCEPTANCE_PENDING'
 $plan|ConvertTo-Json -Depth 6|Set-Content (Join-Path $BackupDirectory 'phone-deployment.json') -Encoding UTF8
 $plan|ConvertTo-Json -Depth 6
}
