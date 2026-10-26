$ErrorActionPreference='Stop'
$r='E:\LeeWay-Work\agent-lee-fold6-unified'
$p=Join-Path $r 'app\src\main\java\industries\leeway\pocket\OwnerSurfaceWindow.kt'
$g=Join-Path $r 'app\build.gradle.kts'
$stage='E:\LeeWay-Work\c3-vision-lane-a-qualification\qualification'
if((Get-FileHash $p).Hash -ne '7105EBBFD0AC1304EB36690C4626F86582A0C276FD5B9B88D430C68C90607501'){throw 'WINDOW_SOURCE_DRIFT'}
$b=Join-Path $stage ('floating-window-v81-backup-'+[DateTime]::UtcNow.ToString('yyyyMMddTHHmmssZ'))
New-Item -ItemType Directory $b|Out-Null
Copy-Item $p (Join-Path $b 'OwnerSurfaceWindow.kt')
Copy-Item $g (Join-Path $b 'build.gradle.kts')
$s=[IO.File]::ReadAllText($p)
$old='setBackgroundColor(Color.rgb(6, 13, 24))'
if(([regex]::Matches($s,[regex]::Escape($old))).Count -ne 1){throw 'SHELL_BACKGROUND_DRIFT'}
$s=$s.Replace($old,'setBackgroundColor(if(title=="Agent Lee Vision") Color.TRANSPARENT else Color.rgb(6, 13, 24))')
$old='background = GradientDrawable().apply { setColor(Color.rgb(15, 29, 47)); setStroke(dp(1), Color.rgb(76, 131, 160)) }'
if(-not $s.Contains($old)){throw 'HEADER_DRIFT'}
$s=$s.Replace($old,'background = GradientDrawable().apply { setColor(if(title=="Agent Lee Vision") Color.argb(72, 8, 22, 40) else Color.rgb(15, 29, 47)); setStroke(if(title=="Agent Lee Vision") 0 else dp(1), Color.rgb(76, 131, 160)) }')
[IO.File]::WriteAllText($p,$s,[Text.UTF8Encoding]::new($false))
$t=[IO.File]::ReadAllText($g)
if(-not $t.Contains('versionCode = 81')){throw 'VERSION_DRIFT'}
$t=$t.Replace('versionCode = 81','versionCode = 82').Replace('versionName = "1.0.0-vision-transparent-rc54"','versionName = "1.0.0-vision-floating-shell-rc55"')
[IO.File]::WriteAllText($g,$t,[Text.UTF8Encoding]::new($false))
[pscustomobject]@{patched='VISION_ONLY_WINDOW_BACKGROUND';backup=$b;windowSha256=(Get-FileHash $p).Hash;versionCode=82}|ConvertTo-Json
