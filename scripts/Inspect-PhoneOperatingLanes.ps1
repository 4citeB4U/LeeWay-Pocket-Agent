<# REGION: LEEWAY.DEVICES.QUALIFICATION; TAG: PHONE_TRANSPORT_AND_LOCAL_UI_OBSERVATION
WHO: Creator-authorized Agent Lee. WHAT: Observe the connected phone and existing wireless/native operating paths.
WHEN: Owner reconnected the phone; WHERE: Explicit discovered serial, not a compiled device identity.
WHY: USB transport, app-local operation and authenticated wireless reachability are separate claims.
HOW: Bounded read-only retries, one shell snapshot, screenshot and factual receipt; no settings changed.
LICENSE: MIT #>
[CmdletBinding()]
param([Parameter(Mandatory=$true)][string]$Serial,[Parameter(Mandatory=$true)][string]$EvidenceDirectory)
Set-StrictMode -Version Latest;$ErrorActionPreference='Stop'
$adb=(Get-Command adb -ErrorAction Stop).Source
[void][IO.Directory]::CreateDirectory($EvidenceDirectory)
$observations=@();$success=$false
$cmd=@'
echo ===INSTALLED===; dumpsys package industries.leeway.pocket | grep -E 'versionCode=|versionName=|lastUpdateTime='; echo ===OVERLAY_PERMISSION===; appops get industries.leeway.pocket SYSTEM_ALERT_WINDOW; echo ===WIRELESS_ADB===; settings get global adb_wifi_enabled; getprop service.adb.tls.port; echo ===NETWORK===; cmd wifi status; echo ===LISTENERS===; ss -ltn; echo ===SERVICES===; dumpsys activity services industries.leeway.pocket | grep -E 'ServiceRecord|isForeground|startRequested'; echo ===POCKET_PROCESS===; pidof industries.leeway.pocket; echo ===OVERLAY_STATE===; run-as industries.leeway.pocket cat shared_prefs/leeway-pocket-overlay.xml; echo ===SPHERE_STATE===; run-as industries.leeway.pocket cat shared_prefs/leeway-floating-sphere.xml; echo ===CURRENT_ACTIVITY===; dumpsys activity activities | grep -m 1 topResumedActivity; echo ===COMPLETE===
'@
for($attempt=1;$attempt -le 12;$attempt++){
 $ErrorActionPreference='Continue'; $state=& $adb -s $Serial get-state 2>&1
 $code=$LASTEXITCODE
 if($code -eq 0 -and ($state|Out-String).Trim() -eq 'device'){
  $snapshot=& $adb -s $Serial shell $cmd 2>&1
  $exit=$LASTEXITCODE
  $ErrorActionPreference='Stop'; $text=$snapshot|Out-String
  $observations+=@{attempt=$attempt;at=[DateTime]::UtcNow.ToString('o');exit=$exit;complete=$text.Contains('===COMPLETE===')}
  if($exit -eq 0 -and $text.Contains('===COMPLETE===')){$success=$true;$text|Set-Content (Join-Path $EvidenceDirectory 'phone-state.txt') -Encoding UTF8;break}
 }else{$observations+=@{attempt=$attempt;at=[DateTime]::UtcNow.ToString('o');exit=$code;state=($state|Out-String).Trim()}}
 Start-Sleep -Milliseconds 1200
}
$receipt=[ordered]@{schemaVersion='leeway.phone-operating-lanes.v1';status=if($success){'READ_ONLY_PHONE_SNAPSHOT_OBSERVED'}else{'PHONE_TRANSPORT_UNSTABLE_OR_ABSENT'};observations=$observations;usbRequiredByProduct=$false;wirelessQualification='NOT_YET_EXECUTED';phoneSettingsChanged=$false;voiceAudibility='NOT_TESTED';formulaExecution='NOT_EXECUTED';learningLedgerUpdated=$false}
if($success){
 $receipt['readoutSha256']=(Get-FileHash (Join-Path $EvidenceDirectory 'phone-state.txt') -Algorithm SHA256).Hash
 $info=New-Object Diagnostics.ProcessStartInfo;$info.FileName=$adb;$info.Arguments='-s '+$Serial+' exec-out screencap -p';$info.UseShellExecute=$false;$info.CreateNoWindow=$true;$info.RedirectStandardOutput=$true;$info.RedirectStandardError=$true
 $proc=New-Object Diagnostics.Process;$proc.StartInfo=$info;[void]$proc.Start();$file=[IO.File]::Create((Join-Path $EvidenceDirectory 'phone.png'));try{$proc.StandardOutput.BaseStream.CopyTo($file)}finally{$file.Dispose()};$proc.WaitForExit()
 if($proc.ExitCode -eq 0 -and (Get-Item (Join-Path $EvidenceDirectory 'phone.png')).Length -gt 100){$receipt['screenshotSha256']=(Get-FileHash (Join-Path $EvidenceDirectory 'phone.png') -Algorithm SHA256).Hash}else{$receipt['screenshotState']='CAPTURE_FAILED'}
 Write-Output $text
}
$receipt|ConvertTo-Json -Depth 8|Set-Content (Join-Path $EvidenceDirectory 'receipt.json') -Encoding UTF8
$receipt|ConvertTo-Json -Depth 8
if(!$success){exit 2}
