<# REGION: LEEWAY.DEPLOYMENT.WINDOWS; TAG: INSTALLED_AGENT_LAUNCHER
WHO: Device owner. WHAT: Open the actual installed Agent Lee UI, including local Brain routes.
WHEN: Existing desktop shortcut. WHERE: Windows native adapter; paths are installation observations.
WHY: Old launcher required a nonexistent health.status=ok. HOW: Existing binding and exact carrier identity.
LICENSE: MIT #>
$ErrorActionPreference='Stop'
$binding=Get-Content -LiteralPath (Join-Path $PSScriptRoot 'host-commander.binding.v1.json') -Raw|ConvertFrom-Json
$ui=[Uri]$binding.agentUiUri
if(!$ui.IsLoopback -or $ui.Scheme -ne 'http'){throw 'OWNER_LOCAL_UI_BINDING_REQUIRED'}
$origin=$ui.GetLeftPart([UriPartial]::Authority)
try{$health=Invoke-RestMethod ($origin+'/health') -TimeoutSec 8}catch{
 $bootstrap=Join-Path $PSScriptRoot 'runtime\machine-consciousness\Start-LeeWay-Machine-Consciousness.ps1'
 if(!(Test-Path $bootstrap)){throw 'EXISTING_RUNTIME_BOOTSTRAP_MISSING'}
 & $bootstrap -NoUI|Out-Null
 $health=Invoke-RestMethod ($origin+'/health') -TimeoutSec 12
}
if($health.identity -ne 'LEEWAY_MACHINE_CONSCIOUSNESS'){throw 'CARRIER_IDENTITY_MISMATCH'}
$browser=$null
foreach($name in @('chrome.exe','msedge.exe')){
 $command=Get-Command $name -ErrorAction SilentlyContinue
 if($command){$browser=$command.Source;break}
 foreach($hive in @('HKCU:','HKLM:')){
  $key=$hive+'\SOFTWARE\Microsoft\Windows\CurrentVersion\App Paths\'+$name
  if(Test-Path $key){$candidate=(Get-Item $key).GetValue('');if($candidate -and (Test-Path $candidate)){$browser=$candidate;break}}
 }
 if($browser){break}
}
if(!$browser){throw 'NATIVE_BROWSER_BINDING_UNAVAILABLE'}
Start-Process -FilePath $browser -ArgumentList @('--app='+$ui.AbsoluteUri,'--window-size=1000,800','--autoplay-policy=no-user-gesture-required')
