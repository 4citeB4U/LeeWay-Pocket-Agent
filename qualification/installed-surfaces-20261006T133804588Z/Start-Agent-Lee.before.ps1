$ErrorActionPreference='Stop'
$boot='E:\LeeWay-Intellectual-Estate\scripts\boot\Start-LeeWay-BootFabric.ps1'
if(Test-Path $boot){& $boot | Out-Null}
$deadline=(Get-Date).AddSeconds(45)
do{try{$h=Invoke-RestMethod 'http://127.0.0.1:8890/health' -TimeoutSec 2;if($h.status -eq 'ok'){break}}catch{};Start-Sleep -Milliseconds 500}while((Get-Date)-lt $deadline)
if(-not $h -or $h.status -ne 'ok'){throw 'Agent Lee consciousness did not become healthy.'}
$chrome='C:\Program Files\Google\Chrome\Application\chrome.exe'
if(-not(Test-Path $chrome)){$chrome='C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe'}
Start-Process $chrome -ArgumentList '--app=http://127.0.0.1:8890/ui','--start-maximized','--autoplay-policy=no-user-gesture-required'
