$ErrorActionPreference='Stop'
$repo='E:\LeeWay-Work\vision-lane-a-source-20261010'
$file=Join-Path $repo 'index.html'
$sha=(Get-FileHash $file).Hash
if($sha -ne '0F3307456BA1F16E7270EB8DD84E38E3B3356130B3D89845E18DC4507A6C1F4D'){throw 'CANONICAL_VISION_HASH_DRIFT'}
$backup='E:\LeeWay-Work\c3-vision-lane-a-qualification\qualification\vision-before-3d-eye-restore.html'
Copy-Item $file $backup
$s=[IO.File]::ReadAllText($file)
$old='#widget .shell-stage{opacity:.23;pointer-events:none}'
if(([regex]::Matches($s,[regex]::Escape($old))).Count -ne 1){throw 'SOURCE_STAGE_NOT_UNIQUE'}
$new='#widget .shell-stage{opacity:1!important;pointer-events:none}'+[Environment]::NewLine+'#widget .shell.main{opacity:1!important;display:block!important}'+[Environment]::NewLine+'#widget .shell.front{display:block!important}'+[Environment]::NewLine+'#widget .shell.back{display:block!important}'
[IO.File]::WriteAllText($file,$s.Replace($old,$new),[Text.UTF8Encoding]::new($false))
[pscustomobject]@{status='EXISTING_3D_EYE_RESTORED';previous=$sha;current=(Get-FileHash $file).Hash;backup=$backup}|ConvertTo-Json
