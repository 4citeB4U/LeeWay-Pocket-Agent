$ErrorActionPreference='Stop'
$root='E:\LeeWay-Work\agent-lee-fold6-unified'
$stage='E:\LeeWay-Work\c3-vision-lane-a-qualification\qualification'
$html=Join-Path $root 'app\src\main\assets\agent_lee_sphere_transparent.html'
$gradle=Join-Path $root 'app\build.gradle.kts'
$candidate=Join-Path $stage 'golden-vision-selection-candidate.html'
$expected='74998A003DADECC41F161A4FD3B01EFE3AB081F777CDEB4C8C11B930330BA585'
if((Get-FileHash $html).Hash -ne $expected){throw 'SOURCE_DRIFT'}
if((Get-FileHash $candidate).Hash -ne '79D46FDAE815DB8AA3A4357DBE7CD2EE1FEB6590ABCBBBB860A90947A281B3EF'){throw 'CANDIDATE_DRIFT'}
$raw=[IO.File]::ReadAllText($gradle)
if(-not $raw.Contains('versionCode = 75') -or -not $raw.Contains('versionName = "1.0.0-touch-request-gate-rc48"')){throw 'VERSION_DRIFT'}
$backup=Join-Path $stage ('v75-scoped-backup-'+[DateTime]::UtcNow.ToString('yyyyMMddTHHmmssZ'))
New-Item -ItemType Directory $backup | Out-Null
Copy-Item $html (Join-Path $backup 'agent_lee_sphere_transparent.html')
Copy-Item $gradle (Join-Path $backup 'build.gradle.kts')
Copy-Item $candidate $html
$updated=$raw.Replace('versionCode = 75','versionCode = 76').Replace('versionName = "1.0.0-touch-request-gate-rc48"','versionName = "1.0.0-vision-in-package-rc49"')
[IO.File]::WriteAllText($gradle,$updated,[Text.UTF8Encoding]::new($false))
[pscustomobject]@{action='SCOPED_PATCH_APPLIED';backup=$backup;htmlHash=(Get-FileHash $html).Hash;gradleHash=(Get-FileHash $gradle).Hash;version=76} | ConvertTo-Json
