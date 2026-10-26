$ErrorActionPreference='Stop'
$r='E:\LeeWay-Work\agent-lee-fold6-unified'
$pc='E:\LeeWay-Work\pc-consciousness-live\runtime\machine-consciousness\pocket-voice.html'
$vision=Join-Path $r 'app\src\main\assets\vision\index.html'
$activity=Join-Path $r 'app\src\main\java\industries\leeway\pocket\VisionActivity.kt'
$build=Join-Path $r 'app\build.gradle.kts'
$stage='E:\LeeWay-Work\c3-vision-lane-a-qualification\qualification'
$files=@($pc,$vision,$activity,$build)
$backup=Join-Path $stage ('floating-vision-backup-'+[DateTime]::UtcNow.ToString('yyyyMMddTHHmmssZ'))
New-Item -ItemType Directory $backup|Out-Null
foreach($f in $files){Copy-Item $f (Join-Path $backup ((Split-Path $f -Leaf)+'.bak'))}
$css=@'
<style id="leeway-golden-vision-float-v1">
/* REGION: LEEWAY.VISION.UI; TAG: TRANSPARENT_GOLDEN_FLOAT; LICENSE: MIT */
html,body,body.pad-open,main.stage{background:transparent!important;background-color:transparent!important}
.stage{isolation:isolate!important}
#widget{filter:none!important}
#widget .poster{display:none!important}
#widget .shell-stage{opacity:.23;pointer-events:none}
#widget .portal{background:rgba(0,0,0,.28)!important}
#widget .portal video{object-fit:contain}
#controls{background:transparent!important;box-shadow:none!important}
@media(max-width:700px){#widget{width:min(98vw,90vh)!important}}
</style>
'@
$v=[IO.File]::ReadAllText($vision)
if($v.Contains('leeway-golden-vision-float-v1')){throw 'ALREADY_PATCHED'}
$v=$v.Replace('</head>',$css+[Environment]::NewLine+'</head>')
[IO.File]::WriteAllText($vision,$v,[Text.UTF8Encoding]::new($false))
$a=[IO.File]::ReadAllText($activity)
if(-not $a.Contains('setBackgroundColor(Color.BLACK)')){throw 'ANDROID_BACKGROUND_DRIFT'}
$a=$a.Replace('setBackgroundColor(Color.BLACK)','setBackgroundColor(Color.TRANSPARENT)')
[IO.File]::WriteAllText($activity,$a,[Text.UTF8Encoding]::new($false))
$p=[IO.File]::ReadAllText($pc)
$old="viewer.style.cssText='width:100%;height:65vh;border:1px solid #87b6d2;border-radius:12px'"
$new="viewer.style.cssText='width:100%;height:77vh;border:0;background:transparent;border-radius:0'"
if(-not $p.Contains($old)){throw 'PC_VIEWER_DRIFT'}
$p=$p.Replace($old,$new)
$pcstyle=@'
<style id="leeway-pc-vision-transparent-v1">
body:has(#surface-panel.open iframe[title="Agent Lee Vision"]) #surface-panel{background:transparent!important;border:0!important;box-shadow:none!important;backdrop-filter:none!important}
body:has(#surface-panel.open iframe[title="Agent Lee Vision"]) #surface-panel iframe{background:transparent!important;border:0!important}
</style>
'@
$p=$p.Replace('</head>',$pcstyle+[Environment]::NewLine+'</head>')
[IO.File]::WriteAllText($pc,$p,[Text.UTF8Encoding]::new($false))
$g=[IO.File]::ReadAllText($build)
if(-not $g.Contains('versionCode = 80')){throw 'ANDROID_VERSION_DRIFT'}
$g=$g.Replace('versionCode = 80','versionCode = 81').Replace('versionName = "1.0.0-voice-cancel-recovery-rc53"','versionName = "1.0.0-vision-transparent-rc54"')
[IO.File]::WriteAllText($build,$g,[Text.UTF8Encoding]::new($false))
[pscustomobject]@{state='SCOPED_FLOATING_PRESENTATION_PATCHED';backup=$backup;pcHash=(Get-FileHash $pc).Hash;visionHash=(Get-FileHash $vision).Hash;activityHash=(Get-FileHash $activity).Hash}|ConvertTo-Json
