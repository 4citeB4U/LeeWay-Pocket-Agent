$ErrorActionPreference='Stop'
$r='E:\LeeWay-Work\agent-lee-fold6-unified'
$p=Join-Path $r 'app\src\main\assets\agent_lee_sphere_transparent.html'
$v=Join-Path $r 'app\build.gradle.kts'
Copy-Item $p ($p+'.a2-text-before.bak') -Force
$s=[IO.File]::ReadAllText($p)
$s=[regex]::Replace($s,'<div class="drawer-subtitle">.*?</div>','<div class="drawer-subtitle">Elemental Intelligence - Logic and Purpose</div>',1)
$s=[regex]::Replace($s,'(<button class="element-card setting-card" data-setting="workstation">.*?<span class="element-motto">).*?(</span>)','$1Virtual Tablet - Notebook - Notepad - Sandbox$2',1)
$s=[regex]::Replace($s,'(<button class="element-card setting-card" data-setting="models">.*?<span class="element-name">).*?(</span>)','$1Models (optional)$2',1)
$style='<style id="LEEWAY_A2_CLEAN_MENU_TEXT">#drawer .drawer-title,#drawer .drawer-subtitle,#drawer .element-name,#drawer .element-motto,#drawer .element-action{font-style:normal!important;font-family:system-ui,Segoe UI,Arial,sans-serif!important;letter-spacing:normal!important;text-transform:none!important}</style>'
if(!$s.Contains('</head>')){throw 'HEAD_MISSING'}
$s=$s.Replace('</head>',$style+[Environment]::NewLine+'</head>')
[IO.File]::WriteAllText($p,$s,(New-Object System.Text.UTF8Encoding($false)))
$a=[IO.File]::ReadAllText($v);if(!$a.Contains('versionCode = 68')){throw 'VERSION_CONFLICT'}
Copy-Item $v ($v+'.a2-text-before.bak') -Force
[IO.File]::WriteAllText($v,$a.Replace('versionCode = 68','versionCode = 69').Replace('1.0.0-hamburger-svg-icons-rc41','1.0.0-hamburger-text-rc42'),(New-Object System.Text.UTF8Encoding($false)))
Write-Output ('HTML_SHA='+(Get-FileHash $p -Algorithm SHA256).Hash)
