$ErrorActionPreference='Stop'
$r='E:\LeeWay-Work\agent-lee-fold6-unified'
$assets=Join-Path $r 'app\src\main\assets'
$targets=@(
 @{file='agent_lee_sphere_transparent.html';needle='</head>';css='.close-btn,#close-drawer{background:#b91c1c!important;color:#fff!important;border:2px solid #fca5a5!important}.close-btn:hover,#close-drawer:hover{background:#991b1b!important}'},
 @{file='digital-brain\brain-live-overrides.css';needle='';css='#backBtn,#leewayReturnAgent,#closeLegend,#closeSettings,#closeHud,.hudClose,.legendClose{background:#b91c1c!important;color:#fff!important;border:2px solid #fca5a5!important}#backBtn:hover,#leewayReturnAgent:hover,.hudClose:hover{background:#991b1b!important}'},
 @{file='agent-vt\continuum\index.html';needle='</head>';css='#back,#close-record{background:#b91c1c!important;color:#fff!important;border:2px solid #fca5a5!important}'}
)
foreach($entry in $targets){
 $p=Join-Path $assets $entry.file
 if(!(Test-Path $p)){throw "MISSING_TARGET $p"}
 $back=$p+'.a2-red-navigation-before-20261009.bak'
 if(!(Test-Path $back)){Copy-Item $p $back}
 $s=[IO.File]::ReadAllText($p)
 if($s.Contains('LEEWAY_A2_RED_NAV_CONTROL')){throw "ALREADY_PATCHED $p"}
 if($entry.needle){
  if(!$s.Contains($entry.needle)){throw "ANCHOR_MISSING $p"}
  $insertion='<style id="LEEWAY_A2_RED_NAV_CONTROL">'+$entry.css+'</style>'
  $s=$s.Replace($entry.needle,$insertion+[Environment]::NewLine+$entry.needle)
 }else{
  $s+=[Environment]::NewLine+'/* LEEWAY_A2_RED_NAV_CONTROL */'+[Environment]::NewLine+$entry.css+[Environment]::NewLine
 }
 [IO.File]::WriteAllText($p,$s,(New-Object System.Text.UTF8Encoding($false)))
 Write-Output ($entry.file+' '+(Get-FileHash $p -Algorithm SHA256).Hash)
}
$v=Join-Path $r 'app\build.gradle.kts'
$s=[IO.File]::ReadAllText($v)
if(!$s.Contains('versionCode = 55')){throw 'VERSION_CONFLICT'}
Copy-Item $v ($v+'.a2-red-navigation-before-20261009.bak') -Force
[IO.File]::WriteAllText($v,$s.Replace('versionCode = 55','versionCode = 56').Replace('1.0.0-ui-red-close-rc28','1.0.0-ui-red-navigation-rc29'),(New-Object System.Text.UTF8Encoding($false)))
Write-Output ('VERSION '+(Get-FileHash $v -Algorithm SHA256).Hash)
