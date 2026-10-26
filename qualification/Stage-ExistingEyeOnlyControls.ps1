$ErrorActionPreference='Stop'
$root='E:\LeeWay-Work\agent-lee-fold6-unified'
$phone=Join-Path $root 'app\src\main\assets\vision\index.html'
$hosted='E:\LeeWay-Work\vision-lane-a-source-20261010\index.html'
$gradle=Join-Path $root 'app\build.gradle.kts'
$expected=@{
 $phone='700BB26ADB057256EF9DBC0C038990AB94387CEFA15306D67E1B539889D7729B'
 $hosted='A8D23F3024AC96AE00F95D7D9ABBF3ACF980DCCEE560F7C6D813F31A2AA64826'
}
foreach($f in $expected.Keys){if((Get-FileHash $f).Hash -ne $expected[$f]){throw ('SOURCE_DRIFT '+$f)}}
$g=[IO.File]::ReadAllText($gradle)
if(-not $g.Contains('versionCode = 84')){throw 'ANDROID_VERSION_DRIFT'}
$backup='E:\LeeWay-Work\c3-vision-lane-a-qualification\qualification\eye-only-controls-before-'+[DateTime]::UtcNow.ToString('yyyyMMddTHHmmssZ')
New-Item -ItemType Directory $backup|Out-Null
foreach($f in @($phone,$hosted,$gradle)){ $label=if($f -eq $phone){'phone-vision.html'}elseif($f -eq $hosted){'hosted-vision.html'}else{'build.gradle.kts'};Copy-Item $f (Join-Path $backup $label)}
$style=@'
<style id="leeway-existing-eye-only-default">
/* REGION: LEEWAY.VISION.UI; TAG: EYE_ONLY_DOCK_STATE; LICENSE: MIT */
/* Restore original 3D Eye; collapse only existing command docks, not optics or cameras. */
body.leeway-eye-only #optLeftDock,
body.leeway-eye-only #optRightDock,
body.leeway-eye-only #optLowerDock{opacity:0!important;visibility:hidden!important;pointer-events:none!important;transition:opacity .18s ease!important}
body.leeway-eye-only #padLauncher{display:block!important;visibility:visible!important;pointer-events:auto!important;opacity:.33!important;background:rgba(2,18,32,.22)!important;bottom:8px!important;min-height:28px!important;font-size:11px!important;border-radius:20px!important}
body.leeway-eye-only #padLauncher:hover,body.leeway-eye-only #padLauncher:focus-visible{opacity:1!important}
</style>
'@
$behavior=@'
<script id="leeway-existing-eye-only-behavior">
/* REGION: LEEWAY.VISION.UI; TAG: REUSE_APPROVED_DOCK_TOGGLES; LICENSE: MIT */
(()=>{
 const attach=()=>{
  const launcher=document.getElementById('padLauncher'),minimize=document.getElementById('minimizePad');
  if(!launcher||!minimize||launcher.dataset.leewayEyeOnly==='v1')return;
  launcher.dataset.leewayEyeOnly='v1';
  document.body.classList.add('leeway-eye-only');
  launcher.hidden=false;
  launcher.textContent='CONTROLS';
  launcher.addEventListener('click',()=>document.body.classList.remove('leeway-eye-only'));
  minimize.addEventListener('click',()=>document.body.classList.add('leeway-eye-only'));
 };
 if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',()=>setTimeout(attach,0),{once:true});
 else setTimeout(attach,0);
})();
</script>
'@
foreach($f in @($phone,$hosted)){
 $s=[IO.File]::ReadAllText($f)
 if($s.Contains('leeway-existing-eye-only-default')){throw ('DUPLICATE_STATE '+$f)}
 if(([regex]::Matches($s,'</head>')).Count -ne 1 -or ([regex]::Matches($s,'</body>')).Count -ne 1){throw ('AMBIGUOUS_DOCUMENT '+$f)}
 $s=$s.Replace('</head>',$style+[Environment]::NewLine+'</head>').Replace('</body>',$behavior+[Environment]::NewLine+'</body>')
 [IO.File]::WriteAllText($f,$s,[Text.UTF8Encoding]::new($false))
}
$g=$g.Replace('versionCode = 84','versionCode = 85').Replace('versionName = "1.0.0-vision-eye-restored-rc57"','versionName = "1.0.0-original-eye-widget-rc58"')
[IO.File]::WriteAllText($gradle,$g,[Text.UTF8Encoding]::new($false))
[pscustomobject]@{status='APPROVED_EYE_ONLY_MODE_STAGED';backup=$backup;phoneSha256=(Get-FileHash $phone).Hash;hostedSha256=(Get-FileHash $hosted).Hash;version=85}|ConvertTo-Json
