<#
REGION: POCKET.UI.REPAIR
TAG: FOLD6-TRUTHFUL-VOICE-UI
WHO: Agent Lee / Creator-authorized build continuation
WHAT: Remove simulated microphone behavior without changing approved sphere geometry.
WHEN: Candidate qualification; WHERE: existing Pocket source.
WHY: Missing microphone/runtime must not look like successful listening or speech.
HOW: Exact replacements with before/after hashes and rollback copies.
LICENSE: MIT
#>
$ErrorActionPreference='Stop'
$r=$PSScriptRoot
$ui=Join-Path $r 'app\src\main\assets\agent_lee_sphere_transparent.html'
$v=Join-Path $r 'app\src\main\java\industries\leeway\pocket\PocketVoiceHost.kt'
$s=[IO.File]::ReadAllText($ui).Replace("`r`n","`n")
$voice=[IO.File]::ReadAllText($v)
$old=@'
    function fallbackListeningSimulation() {
      isListening = true;
      statusBadge.classList.add('active');
      statusText.textContent = 'Resonating...';
      let t = 0;
      const simInterval = setInterval(() => {
        t += 0.15;
        voiceAmplitude = Math.abs(Math.sin(t * 3)) * 0.5 + Math.random() * 0.2;
      }, 40);
      setTimeout(() => {
        clearInterval(simInterval);
        stopListening();
        agentLeeSpeak("Logic. Experience. Human Purpose.");
      }, 3000);
    }
'@
$new=@'
    function voiceUnavailable(reason = 'MICROPHONE_PROVIDER_UNBOUND') {
      stopListening();
      isSpeaking = false;
      voiceAmplitude = 0;
      statusBadge.style.display = 'flex';
      statusText.textContent = 'Agent Lee connection unavailable: ' + reason;
      window.dispatchEvent(new CustomEvent('leeway:voice-error', { detail: { reason } }));
    }
'@
if(([regex]::Matches($s,[regex]::Escape($old))).Count -ne 1){throw 'UI_SOURCE_CHANGED_REVIEW_REQUIRED'}
$s=$s.Replace($old,$new).Replace('fallbackListeningSimulation();','voiceUnavailable();')
$s=$s.Replace("      const w = window.open('', '_self');`n",'')
$oldStart="    async function startListening() {`n      try {"
$newStart="    async function startListening() {`n      if (!('webkitSpeechRecognition' in window || 'SpeechRecognition' in window)) {`n        voiceUnavailable('SPEECH_RECOGNITION_UNBOUND');`n        return;`n      }`n      try {"
if(!$s.Contains($oldStart)){throw 'LISTENING_BOUNDARY_CHANGED'}
$s=$s.Replace($oldStart,$newStart)
$oldWarn='            if(payload==null||!admitRenderer(payload))return@deliver'
if(!$voice.Contains($oldWarn)){throw 'VOICE_SOURCE_CHANGED_REVIEW_REQUIRED'}
$voice=$voice.Replace($oldWarn,'            if(!admitRenderer(payload))return@deliver')
$dir=Join-Path $r ('qualification\ui-repair-'+[DateTime]::UtcNow.ToString('yyyyMMddTHHmmssfffZ'))
$null=New-Item -ItemType Directory -Path $dir
Copy-Item -LiteralPath $ui -Destination (Join-Path $dir 'sphere-before.html')
Copy-Item -LiteralPath $v -Destination (Join-Path $dir 'voice-host-before.kt')
$before=@(Get-FileHash -LiteralPath $ui,$v -Algorithm SHA256)
$utf8=New-Object Text.UTF8Encoding($false)
[IO.File]::WriteAllText($ui,$s,$utf8)
[IO.File]::WriteAllText($v,$voice,$utf8)
[ordered]@{status='SOURCE_REPAIRED_TESTS_PENDING';before=$before;after=@(Get-FileHash -LiteralPath $ui,$v -Algorithm SHA256);installed=$false}|ConvertTo-Json -Depth 5|Set-Content (Join-Path $dir 'receipt.json') -Encoding UTF8
Write-Output ('UI_REPAIR_RECEIPT='+$dir)
