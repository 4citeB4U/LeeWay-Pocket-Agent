$ErrorActionPreference='Stop'
$root='E:\LeeWay-Work\vision-lane-a-source-20261010'
$file=Join-Path $root 'index.html'
$expected='F14B6B420C3C0CA2FE7043379DF60134C87E46A597E4D5C221BCEFC3AF42F2C4'
if((Get-FileHash $file).Hash -ne $expected){throw 'CANONICAL_SOURCE_DRIFT'}
$branch='repair/c3-vision-transparent-floating-parity-20261010'
git -C $root switch -c $branch
if($LASTEXITCODE -ne 0){throw 'BRANCH_FAILED'}
$backup=Join-Path 'E:\LeeWay-Work\c3-vision-lane-a-qualification\qualification' 'vision-hosted-before-floating.html'
Copy-Item $file $backup
$s=[IO.File]::ReadAllText($file)
if($s.Contains('leeway-golden-vision-float-v1')){throw 'DUPLICATE_THEME'}
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
if(([regex]::Matches($s,'</head>')).Count -ne 1){throw 'HTML_HEAD_AMBIGUOUS'}
[IO.File]::WriteAllText($file,$s.Replace('</head>',$css+[Environment]::NewLine+'</head>'),[Text.UTF8Encoding]::new($false))
$checks=[ordered]@{bodyTransparent=([IO.File]::ReadAllText($file).Contains('background:transparent!important'));logoPreserved=([IO.File]::ReadAllText($file).Contains('id="logoCarousel"'));cameraControlPreserved=([IO.File]::ReadAllText($file).Contains('id="cameraButton"'));stopControlPreserved=([IO.File]::ReadAllText($file).Contains('STOP ALL CAMERAS'));originalSha256=$expected;candidateSha256=(Get-FileHash $file).Hash;backup=$backup}
$checks|ConvertTo-Json
if(-not $checks.logoPreserved -or -not $checks.cameraControlPreserved){throw 'SOURCE_REGRESSION'}
