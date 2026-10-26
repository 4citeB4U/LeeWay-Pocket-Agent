$ErrorActionPreference = 'Stop'
$source = 'E:\LeeWay-Work\agent-lee-fold6-unified\app\src\main\assets\agent_lee_sphere_transparent.html'
$destination = 'E:\LeeWay-Work\c3-vision-lane-a-qualification\qualification\golden-vision-selection-candidate.html'
$original = [IO.File]::ReadAllText($source)
$sourceHash = (Get-FileHash $source -Algorithm SHA256).Hash
if ($original -notmatch 'data-setting="about"' -or $original -notmatch "const known=new Set") { throw 'Canonical menu contract not found' }
if ($original -match 'data-setting="vision"') { throw 'Vision is already present; stop to avoid duplication' }
$entry = '<button class="element-card setting-card" data-setting="vision"><span><span class="element-name">Vision</span><span class="element-motto">Authorized camera viewing inside Agent Lee</span></span><span class="element-action">OPEN</span></button>'
$marker = '<button class="element-card setting-card" data-setting="about">'
if ($original.Split([string[]]@($marker),[StringSplitOptions]::None).Length -ne 2) { throw 'Ambiguous menu insertion marker' }
$patched = $original.Replace($marker, $entry + [Environment]::NewLine + $marker)
$oldKnown = "'evidence','about']);"
$newKnown = "'evidence','about','vision']);"
if (-not $patched.Contains($oldKnown)) { throw 'Cannot find menu allowlist' }
$patched = $patched.Replace($oldKnown,$newKnown)
$oldDispatch = "if(key==='voice'&&window.LeeWayAndroid?.openVoiceStudio)"
$insert = @'
  if(key==='vision'){
    const panel=document.getElementById('surface-panel');
    if(!panel)return;
    panel.replaceChildren();
    const close=document.createElement('button');
    close.textContent='Back to Agent Lee';
    close.onclick=()=>{panel.classList.remove('open');panel.replaceChildren();window.__leewayFloating?.host?.('panel',false);};
    const heading=document.createElement('h2');
    heading.textContent='Agent Lee Vision';
    const state=document.createElement('p');
    state.textContent='Camera access requires separate owner consent. Remote cameras are not assumed connected.';
    const viewer=document.createElement('iframe');
    viewer.title='LeeWay Vision camera viewer';
    viewer.src='https://leeway-agent-lee-vision.vercel.app/';
    viewer.style.cssText='width:100%;height:65vh;border:1px solid #87b6d2;border-radius:12px';
    viewer.allow='camera; microphone; fullscreen';
    panel.append(close,heading,state,viewer);
    panel.classList.add('open');
    await window.__leewayFloating?.host?.('panel',true);
    return;
  }
'@
if (-not $patched.Contains($oldDispatch)) { throw ('Cannot find surface handler: ' + $oldDispatch) }
$patched = $patched.Replace($oldDispatch,$insert + [Environment]::NewLine + $oldDispatch)
[IO.File]::WriteAllText($destination,$patched,[Text.UTF8Encoding]::new($false))
$keys = [regex]::Matches($patched,'data-setting="([^"]+)"') | ForEach-Object {$_.Groups[1].Value} | Sort-Object -Unique
$expected = @('digital-brain','continuum','diagnostics','voice','workstation','models','skills','authority','evidence','about','vision') | Sort-Object
if ((Compare-Object $keys $expected)) { throw 'Selection set regression' }
if (([regex]::Matches($patched,'data-setting="vision"')).Count -ne 1) { throw 'Vision menu duplicate' }
[pscustomobject]@{status='CANDIDATE_SOURCE_CHECK_PASS';sourceSha256=$sourceHash;candidateSha256=(Get-FileHash $destination -Algorithm SHA256).Hash;candidate=$destination;menuItems=$keys.Count;productionSourceUnchanged=((Get-FileHash $source -Algorithm SHA256).Hash -eq $sourceHash)} | ConvertTo-Json
