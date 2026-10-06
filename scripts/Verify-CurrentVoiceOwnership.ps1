<#
REGION: LEEWAY.VOICE.QUALIFICATION
TAG: RECHECK_SHARED_SPEAKER_AUTHORITY_WITHOUT_SOURCE_MUTATION
WHO: Creator-authorized Agent Lee. WHAT: Reuse current Voice Fabric tests and real selected renderer.
WHEN: Overlay/Voice ownership correction. WHERE: explicit owner-local carrier and existing Voice repository.
WHY: Device identity is not speaker identity; a working overlay is not proof of audible speech.
HOW: Read binding, run existing tests, synthesize with attempted client override, verify waveform/binding.
LICENSE: MIT
#>
[CmdletBinding()]
param([Parameter(Mandatory=$true)][string]$VoiceRoot,[Parameter(Mandatory=$true)][string]$CarrierUri,[Parameter(Mandatory=$true)][string]$OutputDirectory)
Set-StrictMode -Version Latest
$ErrorActionPreference='Stop'
$uri=[Uri]$CarrierUri
if(!$uri.IsLoopback -or $uri.Scheme -ne 'http'){throw 'OWNER_LOCAL_CARRIER_REQUIRED'}
if((& git -C $VoiceRoot remote get-url origin|Out-String).Trim() -ne 'https://github.com/4citeB4U/LeeWay-Voice-Fabric.git'){throw 'VOICE_ORIGIN_MISMATCH'}
if(Test-Path -LiteralPath $OutputDirectory){throw 'FRESH_EVIDENCE_DIRECTORY_REQUIRED'}
[void][IO.Directory]::CreateDirectory($OutputDirectory)
$bindingFile=Join-Path $VoiceRoot 'runtime/employee-voice-bindings.v1.json'
$before=(Get-FileHash $bindingFile -Algorithm SHA256).Hash
$receipt=[ordered]@{schemaVersion='leeway.current-voice-ownership.v1';status='RUNNING';voiceSourceCommit=(& git -C $VoiceRoot rev-parse HEAD|Out-String).Trim();fixtureProviders=$false;phoneConnected=$false;phonePlaybackTested=$false;crossDeviceSelectionSyncTested=$false;humanAudibilityConfirmed=$false;formulaExecuted=$false;voiceSelectionChanged=$false}
try {
 Push-Location $VoiceRoot
 try {
  $ErrorActionPreference='Continue'
  & node --test --test-reporter=tap tests/voice-authority.test.cjs *> (Join-Path $OutputDirectory 'voice-authority-tests.tap')
  $code=$LASTEXITCODE;$ErrorActionPreference='Stop'
  if($code -ne 0){throw 'CURRENT_VOICE_AUTHORITY_TESTS_FAILED'}
 }finally{Pop-Location}
 $settings=Invoke-RestMethod ($CarrierUri.TrimEnd('/')+'/settings') -TimeoutSec 10
 $binding=$settings.voiceBinding
 if($binding.authority -ne 'LEEWAY_VOICE_FABRIC' -or $binding.deviceMayOverride -ne $false -or $binding.systemVoiceFallback -ne $false){throw 'SHARED_VOICE_AUTHORITY_NOT_ENFORCED'}
 $receipt['binding']=$binding
 $text='Creator, this is LeeWay Voice. The device provides the speaker, but it does not choose my voice.'
 $payload=@{text=$text;voicePackageId='android-installed-english';provider='android-tts'}|ConvertTo-Json -Compress
 $r=Invoke-RestMethod ($CarrierUri.TrimEnd('/')+'/render-voice') -Method Post -ContentType 'application/json' -Body $payload -TimeoutSec 120
 if($r.voicePackageId -ne $binding.voicePackageId -or $r.provider -ne $binding.provider -or $r.voiceId -ne $binding.voiceId -or $r.selectionRevision -ne $binding.selectionRevision -or $r.voiceAuthority -ne 'LEEWAY_VOICE_FABRIC'){throw 'RENDERER_IDENTITY_MISMATCH'}
 $audio=[Convert]::FromBase64String($r.audioContent)
 if($audio.Length -lt 46 -or [Text.Encoding]::ASCII.GetString($audio,0,4) -ne 'RIFF' -or [Text.Encoding]::ASCII.GetString($audio,8,4) -ne 'WAVE' -or [BitConverter]::ToUInt16($audio,20) -ne 1 -or [BitConverter]::ToUInt16($audio,34) -ne 16){throw 'PCM_AUDIO_INVALID'}
 $peak=0;for($i=44;$i+1 -lt $audio.Length;$i+=2){$peak=[Math]::Max($peak,[Math]::Abs([int][BitConverter]::ToInt16($audio,$i)))}
 if($peak -le 0){throw 'GENERATED_AUDIO_IS_SILENT'}
 $wav=Join-Path $OutputDirectory 'selected-voice.wav';[IO.File]::WriteAllBytes($wav,$audio)
 $receipt['synthesis']=[ordered]@{voicePackageId=$r.voicePackageId;provider=$r.provider;audioBytes=$audio.Length;sha256=(Get-FileHash $wav -Algorithm SHA256).Hash;sampleRate=[BitConverter]::ToUInt32($audio,24);peakAbsolutePcm16=$peak;deviceOverrideRequested='android-installed-english';deviceOverrideAdmitted=$false;playbackPerformed=$false}
 $after=(Invoke-RestMethod ($CarrierUri.TrimEnd('/')+'/settings') -TimeoutSec 10).voiceBinding
 if((Get-FileHash $bindingFile -Algorithm SHA256).Hash -ne $before -or $after.selectionRevision -ne $binding.selectionRevision){throw 'VOICE_BINDING_CHANGED_DURING_QUALIFICATION'}
 $receipt['bindingFileSha256']=$before
 $receipt.status='SHARED_SELECTION_AND_REAL_SYNTHESIS_VERIFIED_NOT_PHONE_OR_AUDIBILITY'
}catch{$receipt.status='FAILED';$receipt['error']=$_.Exception.Message}
$receipt|ConvertTo-Json -Depth 10|Set-Content (Join-Path $OutputDirectory 'receipt.json') -Encoding UTF8
$receipt|ConvertTo-Json -Depth 10
if($receipt.status -eq 'FAILED'){exit 1}
