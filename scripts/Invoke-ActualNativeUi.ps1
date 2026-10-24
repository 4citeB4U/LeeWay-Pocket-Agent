<# REGION: LEEWAY.DEPLOYMENT; TAG: ACTUAL_NATIVE_UI_REPLACEMENT
WHO: Creator-authorized Agent Lee. WHAT: Start existing carrier with repaired page and native Device Bridge host.
WHEN: After owner backup/source parsing. WHERE: supplied device binding, no guessed process target.
WHY: A debug candidate left on disk cannot change the launcher. HOW: Verify -> restart exact carrier -> native host -> evidence.
LICENSE: MIT #>
[CmdletBinding()]
param([Parameter(Mandatory=$true)][string]$BodyRoot,[Parameter(Mandatory=$true)][string]$HostScript,[Parameter(Mandatory=$true)][string]$EvidenceDirectory,[switch]$Apply)
Set-StrictMode -Version Latest;$ErrorActionPreference='Stop'
$runtime=Join-Path $BodyRoot 'runtime';$carrier=Join-Path $runtime 'machine-consciousness\carrier-server.mjs'
if((& git -C $runtime remote get-url origin|Out-String).Trim() -ne 'https://github.com/4citeB4U/Leeway-Runtime-Fabric.git'){throw 'RUNTIME_ORIGIN_MISMATCH'}
node --check $carrier;if($LASTEXITCODE -ne 0){throw 'CARRIER_PARSE_FAILED'}
python -m py_compile $HostScript;if($LASTEXITCODE -ne 0){throw 'NATIVE_HOST_PARSE_FAILED'}
$binding=Join-Path $BodyRoot 'host-commander.binding.v1.json';$b=Get-Content $binding -Raw|ConvertFrom-Json;$url=[Uri]$b.agentUiUri
if(!$url.IsLoopback){throw 'OWNER_LOCAL_UI_BINDING_REQUIRED'}
$listener=Get-NetTCPConnection -LocalPort $url.Port -State Listen|Select-Object -First 1
$process=Get-CimInstance Win32_Process -Filter ('ProcessId='+$listener.OwningProcess)
if($process.Name -ne 'node.exe' -or $process.CommandLine -notmatch 'carrier-server.mjs'){throw 'LIVE_CARRIER_OWNER_CHANGED'}
$plan=[ordered]@{state='DRY_RUN';priorCarrierPid=$process.ProcessId;carrierSha256=(Get-FileHash $carrier -Algorithm SHA256).Hash;nativeHostSha256=(Get-FileHash $HostScript -Algorithm SHA256).Hash;voiceSelectionChanged=$false;nativeHostUnderDevices=$true;fullGoldenRelease=$false}
if(!$Apply){$plan|ConvertTo-Json;return}
[void][IO.Directory]::CreateDirectory($EvidenceDirectory)
Copy-Item $binding (Join-Path $EvidenceDirectory 'device-binding.before.json')
Copy-Item (Join-Path $BodyRoot 'Start-Agent-Lee.ps1') (Join-Path $EvidenceDirectory 'desktop-launcher.before.ps1')
Stop-Process -Id $process.ProcessId
$running=Start-Process (Get-Command node).Source -ArgumentList 'machine-consciousness/carrier-server.mjs' -WorkingDirectory $runtime -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $EvidenceDirectory 'carrier.stdout.log') -RedirectStandardError (Join-Path $EvidenceDirectory 'carrier.stderr.log')
Start-Sleep -Seconds 2
$health=Invoke-RestMethod ($url.GetLeftPart([UriPartial]::Authority)+'/health') -TimeoutSec 10
if($health.identity -ne 'LEEWAY_MACHINE_CONSCIOUSNESS'){throw 'ACTUAL_CARRIER_IDENTITY_FAILED'}
$native=Start-Process (Get-Command python).Source -ArgumentList @('-u',('"'+$HostScript+'"'),'--binding',('"'+$binding+'"'),'--evidence',('"'+$EvidenceDirectory+'"')) -WorkingDirectory $BodyRoot -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $EvidenceDirectory 'native.stdout.log') -RedirectStandardError (Join-Path $EvidenceDirectory 'native.stderr.log')
$plan['state']='NATIVE_HOST_STARTED_ACTUAL_COMPOSITOR_TEST_REQUIRED';$plan['carrierPid']=$running.Id;$plan['nativePid']=$native.Id
$plan|ConvertTo-Json -Depth 6|Set-Content (Join-Path $EvidenceDirectory 'native-start.json') -Encoding UTF8
$plan|ConvertTo-Json -Depth 6
