$ErrorActionPreference='Stop'
$r='E:\LeeWay-Work\agent-lee-fold6-unified'
$p=Join-Path $r 'app\src\main\assets\agent_lee_sphere_transparent.html'
$v=Join-Path $r 'app\build.gradle.kts'
Copy-Item -LiteralPath $p -Destination ($p+'.a2-before-chevron-v70.bak') -Force
$s=[IO.File]::ReadAllText($p)
if($s.Contains('LEEWAY_A2_OPEN_CHEVRON_V70')){throw 'ALREADY_PATCHED'}
$css=@'
<style id="LEEWAY_A2_OPEN_CHEVRON_V70">
/* Correct the malformed generated pseudo-glyph after OPEN; draw a CSS chevron. */
#drawer .element-card .element-action{display:inline-flex!important;align-items:center!important;gap:9px!important;font-style:normal!important;white-space:nowrap!important}
#drawer .element-card .element-action::after{content:""!important;display:inline-block!important;width:11px!important;height:11px!important;border-right:3px solid #4fd5ff!important;border-top:3px solid #4fd5ff!important;transform:rotate(45deg)!important;flex-shrink:0!important;font-size:0!important}
</style>
'@
if(!$s.Contains('</head>')){throw 'HEAD_NOT_FOUND'}
$s=$s.Replace('</head>',$css+[Environment]::NewLine+'</head>')
[IO.File]::WriteAllText($p,$s,(New-Object System.Text.UTF8Encoding($false)))
$a=[IO.File]::ReadAllText($v)
if(!$a.Contains('versionCode = 69')){throw 'VERSION_CONFLICT'}
Copy-Item $v ($v+'.a2-before-v70.bak') -Force
[IO.File]::WriteAllText($v,$a.Replace('versionCode = 69','versionCode = 70').Replace('1.0.0-hamburger-text-rc42','1.0.0-hamburger-chevron-rc43'),(New-Object System.Text.UTF8Encoding($false)))
Write-Output ('HTML_SHA256='+(Get-FileHash $p -Algorithm SHA256).Hash)
