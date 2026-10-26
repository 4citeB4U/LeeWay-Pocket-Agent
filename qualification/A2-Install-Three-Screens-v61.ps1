$ErrorActionPreference='Stop'
$r='E:\LeeWay-Work\agent-lee-fold6-unified'
$p=Join-Path $r 'app\src\main\assets\agent_lee_sphere_transparent.html'
$v=Join-Path $r 'app\build.gradle.kts'
Copy-Item $p ($p+'.a2-three-screens-before.bak') -Force
$s=[IO.File]::ReadAllText($p)
$anchor="panel.replaceChildren();const close=document.createElement('button');"
if(!$s.Contains($anchor)){throw 'SURFACE_ROUTE_CHANGED'}
$old="panel.replaceChildren();const close=document.createElement('button');"
$s=$s.Replace($old,$old)
$inject="panel.classList.remove('leeway-three-open');"
$s=$s.Replace($anchor,$inject+$anchor)
$anchor="const article=document.createElement('article');panel.appendChild(article);panel.classList.add('open');await window.__leewayFloating.host('panel',true);"
if(!$s.Contains($anchor)){throw 'SURFACE_RENDER_ANCHOR_CHANGED'}
$s=$s.Replace($anchor,$anchor+[Environment]::NewLine+" if(['about','authority','evidence'].includes(key)&&window.__leewayThreeScreens){await window.__leewayThreeScreens(key,article);return;}")
if(!$s.Contains('</head>') -or !$s.Contains('</body>')){throw 'HTML_STRUCTURE_MISSING'}
$s=$s.Replace('</head>','<link rel="stylesheet" href="leeway-three-reference-screens.css">'+[Environment]::NewLine+'</head>')
$s=$s.Replace('</body>','<script src="leeway-three-reference-screens.js"></script>'+[Environment]::NewLine+'</body>')
[IO.File]::WriteAllText($p,$s,(New-Object System.Text.UTF8Encoding($false)))
$a=[IO.File]::ReadAllText($v)
if(!$a.Contains('versionCode = 60')){throw 'VERSION_CONFLICT'}
Copy-Item $v ($v+'.a2-three-screens-before.bak') -Force
[IO.File]::WriteAllText($v,$a.Replace('versionCode = 60','versionCode = 61').Replace('1.0.0-brain-native-drag-rc33','1.0.0-three-reference-screens-rc34'),(New-Object System.Text.UTF8Encoding($false)))
Get-FileHash $p,$v -Algorithm SHA256 | ForEach-Object{$_.Path+' '+$_.Hash}
