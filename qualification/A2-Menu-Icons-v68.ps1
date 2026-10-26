$ErrorActionPreference='Stop'
$r='E:\LeeWay-Work\agent-lee-fold6-unified'
$p=Join-Path $r 'app\src\main\assets\agent_lee_sphere_transparent.html'
$v=Join-Path $r 'app\build.gradle.kts'
Copy-Item $p ($p+'.a2-logo-icon-before.bak') -Force
$s=[IO.File]::ReadAllText($p)
if($s.Contains('LEEWAY_A2_REAL_MENU_ICONS')){throw 'ALREADY_APPLIED'}
$patch=@'
<style id="LEEWAY_A2_REAL_MENU_ICONS">
/* SVG icon tiles replace corrupt pseudo-character logos, with existing handlers intact. */
#drawer .element-card.setting-card::before{display:none!important;content:none!important}
#drawer .element-card.setting-card{grid-template-columns:70px minmax(0,1fr) auto!important}
#drawer .element-card .lw-menu-icon{display:grid!important;place-items:center!important;width:68px!important;height:72px!important;min-width:68px!important;background:linear-gradient(150deg,#0c345c,#051a38)!important;border:2px solid #35c5ff!important;border-radius:16px!important;box-shadow:inset 0 0 15px #009cde55,0 0 12px #00b6ff55!important}
#drawer .element-card .lw-menu-icon svg{width:44px;height:44px;display:block;filter:drop-shadow(0 0 5px #0bd2ff)}
@media(max-width:400px){#drawer .element-card.setting-card{grid-template-columns:52px minmax(0,1fr) auto!important}#drawer .element-card .lw-menu-icon{width:50px!important;height:54px!important;min-width:50px!important;border-radius:12px!important}#drawer .element-card .lw-menu-icon svg{width:34px;height:34px}}
</style>
'@
if(!$s.Contains('</head>') -or !$s.Contains('</body>')){throw 'INVALID_HTML_STRUCTURE'}
$s=$s.Replace('</head>',$patch+[Environment]::NewLine+'</head>')
$s=$s.Replace('</body>','<script src="leeway-menu-icons.js"></script>'+[Environment]::NewLine+'</body>')
[IO.File]::WriteAllText($p,$s,(New-Object System.Text.UTF8Encoding($false)))
$a=[IO.File]::ReadAllText($v)
if(!$a.Contains('versionCode = 67')){throw 'VERSION_CONFLICT'}
Copy-Item $v ($v+'.a2-logo-icon-before.bak') -Force
[IO.File]::WriteAllText($v,$a.Replace('versionCode = 67','versionCode = 68').Replace('1.0.0-hamburger-reference-rc40','1.0.0-hamburger-svg-icons-rc41'),(New-Object System.Text.UTF8Encoding($false)))
Write-Output ('HTML_SHA='+(Get-FileHash $p -Algorithm SHA256).Hash)
Write-Output ('JS_SHA='+(Get-FileHash (Join-Path $r 'app\src\main\assets\leeway-menu-icons.js') -Algorithm SHA256).Hash)
