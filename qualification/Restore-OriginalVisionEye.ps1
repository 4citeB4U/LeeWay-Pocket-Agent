$ErrorActionPreference='Stop'
$phone='E:\LeeWay-Work\agent-lee-fold6-unified\app\src\main\assets\vision\index.html'
$pc='E:\LeeWay-Work\pc-consciousness-live\runtime\machine-consciousness\pocket-voice.html'
$build='E:\LeeWay-Work\agent-lee-fold6-unified\app\build.gradle.kts'
$backup='E:\LeeWay-Work\c3-vision-lane-a-qualification\qualification\restore-original-3d-eye-'+[DateTime]::UtcNow.ToString('yyyyMMddTHHmmssZ')
New-Item -ItemType Directory $backup|Out-Null
foreach($f in @($phone,$pc,$build)){Copy-Item $f (Join-Path $backup (Split-Path $f -Leaf))}
$h=[IO.File]::ReadAllText($phone)
if(([regex]::Matches($h,'#widget .shell-stage\{opacity:\.23;pointer-events:none\}')).Count -ne 1){throw 'EYE_LAYER_DRIFT'}
$h=$h.Replace('#widget .shell-stage{opacity:.23;pointer-events:none}', '#widget .shell-stage{opacity:1!important;pointer-events:none}'+[Environment]::NewLine+'#widget .shell.main{opacity:1!important;display:block!important}'+[Environment]::NewLine+'#widget .shell.front{display:block!important}'+[Environment]::NewLine+'#widget .shell.back{display:block!important}')
[IO.File]::WriteAllText($phone,$h,[Text.UTF8Encoding]::new($false))
$p=[IO.File]::ReadAllText($pc)
$old="panel.append(back,viewer);panel.classList.add('open')"
$new="back.style.cssText='position:fixed;right:12px;top:12px;z-index:1000;background:rgba(7,23,41,.55);border:0;border-radius:50%;color:#fff';panel.append(back,viewer);panel.classList.add('open')"
if(-not $p.Contains($old)){throw 'PC_WINDOW_DISPATCH_DRIFT'}
$p=$p.Replace($old,$new)
[IO.File]::WriteAllText($pc,$p,[Text.UTF8Encoding]::new($false))
$g=[IO.File]::ReadAllText($build)
if($g.Contains('versionCode = 82')){$g=$g.Replace('versionCode = 82','versionCode = 83').Replace('versionName = "1.0.0-vision-floating-shell-rc55"','versionName = "1.0.0-vision-3d-eye-restored-rc56"');[IO.File]::WriteAllText($build,$g,[Text.UTF8Encoding]::new($false))}else{throw 'VERSION_OWNER_DRIFT'}
[pscustomobject]@{result='ORIGINAL_3D_EYE_RESTORED_SOURCE';backup=$backup;phoneHash=(Get-FileHash $phone).Hash;pcHash=(Get-FileHash $pc).Hash;versionCode=83}|ConvertTo-Json
