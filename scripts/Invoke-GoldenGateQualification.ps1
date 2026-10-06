<#
REGION: LEEWAY.POCKET.RELEASE_QUALIFICATION
TAG: GOLDEN_GATE_QUALIFICATION_RECEIPT
WHO: Creator-authorized Agent Lee
WHAT: Execute gate regression tests and reject the actual incomplete APK with missing proof.
WHEN: Golden customer-package requirement; WHERE: existing candidate checkout and local evidence.
WHY: Record what was actually tested without implying customer capability or device qualification.
HOW: Source hashes, Node tests, exit-code checks, exact APK hash, negative assessment and receipt.
LICENSE: MIT
#>
[CmdletBinding()]
param([Parameter(Mandatory=$true)][string]$ApkPath,[Parameter(Mandatory=$true)][string]$ArtifactSourceCommit)
Set-StrictMode -Version Latest
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
if((& git -C $root remote get-url origin|Out-String).Trim() -ne 'https://github.com/4citeB4U/LeeWay-Pocket-Agent.git'){throw 'POCKET_ORIGIN_REQUIRED'}
$runId='golden-gate-'+[DateTime]::UtcNow.ToString('yyyyMMddTHHmmssfffZ')
$dir=Join-Path $root ('qualification\'+$runId)
$null=New-Item -ItemType Directory -Path $dir
$paths=@('contracts\golden-apk-release.v1.json','scripts\golden-release-gate.mjs','tests\golden-release-gate.test.mjs','tests\sphere-voice-boundary.test.mjs','Verify-SingleApk.ps1')
$before=@(foreach($p in $paths){[pscustomobject]@{path=$p;sha256=(Get-FileHash -LiteralPath (Join-Path $root $p) -Algorithm SHA256).Hash}})
$policy=Get-Content -LiteralPath (Join-Path $root $paths[0]) -Raw|ConvertFrom-Json
$count=0;foreach($p in $policy.requiredGroups.PSObject.Properties){$count+=@($p.Value).Count}
$testsLog=Join-Path $dir 'gate-tests.tap'
$assessment=Join-Path $dir 'candidate-assessment.json'
Push-Location $root
try {
 & node --test --test-reporter=tap tests/golden-release-gate.test.mjs tests/sphere-voice-boundary.test.mjs *> $testsLog
 $testExit=$LASTEXITCODE
 if($testExit -ne 0){Get-Content $testsLog -Tail 100;throw 'GOLDEN_GATE_REGRESSIONS_FAILED'}
 $gateLog=Join-Path $dir 'candidate-assessment.log'
 & node scripts/golden-release-gate.mjs --apk $ApkPath --source-commit $ArtifactSourceCommit --profiles android-reference-candidate --out $assessment *> $gateLog
 $gateExit=$LASTEXITCODE
 if($gateExit -ne 2){throw 'INCOMPLETE_GOLDEN_CANDIDATE_MUST_BE_REJECTED'}
 $result=Get-Content $assessment -Raw|ConvertFrom-Json
 if($result.status -ne 'BLOCKED' -or $result.verifiedEvidenceCount -ne 0 -or $result.requiredCaseCount -ne $count){throw 'CANDIDATE_ASSESSMENT_INCONSISTENT'}
 if(@($result.blockers|Where-Object {$_.code -eq 'MISSING_ACCEPTANCE_EVIDENCE'}).Count -ne $count){throw 'MANDATORY_CASES_NOT_ALL_REPORTED'}
 foreach($entry in $before){if((Get-FileHash (Join-Path $root $entry.path) -Algorithm SHA256).Hash -ne $entry.sha256){throw 'QUALIFICATION_SOURCE_CHANGED'}}
 $tap=Get-Content $testsLog -Raw
 $passed=[regex]::Match($tap,'(?m)^# pass (\d+)').Groups[1].Value
 $failed=[regex]::Match($tap,'(?m)^# fail (\d+)').Groups[1].Value
 if(!$passed -or !$failed -or $failed -ne '0'){throw 'TAP_SUMMARY_NOT_VERIFIED'}
 $receipt=[ordered]@{schemaVersion='leeway.golden-gate-qualification.v1';runId=$runId;sourceBase=(& git -C $root rev-parse HEAD|Out-String).Trim();artifactSourceCommit=$ArtifactSourceCommit;status='VERIFIED_RELEASE_GATE_REGRESSIONS_NOT_PRODUCT_ACCEPTANCE';unitTestsPassed=[int]$passed;unitTestsFailed=[int]$failed;testExitCode=$testExit;negativeCandidateExitCode=$gateExit;requiredGroups=@($policy.requiredGroups.PSObject.Properties).Count;requiredAcceptanceCasesPerProfile=$count;candidateVerifiedAcceptanceCases=0;candidateReleaseState=$result.status;candidateProfile='PROVISIONAL_PLAN_LABEL_NOT_SUPPORTED_PLATFORM_CERTIFICATION';apkSha256=(Get-FileHash $ApkPath -Algorithm SHA256).Hash;apkBytes=(Get-Item $ApkPath).Length;testsLogSha256=(Get-FileHash $testsLog -Algorithm SHA256).Hash;assessmentSha256=(Get-FileHash $assessment -Algorithm SHA256).Hash;sourceHashes=$before;phoneModified=$false;apkRebuilt=$false;releasePromoted=$false;formulaExecution='NOT_EXECUTED';learningLedgerUpdated=$false}
 $receiptPath=Join-Path $dir 'receipt.json'
 $receipt|ConvertTo-Json -Depth 8|Set-Content -LiteralPath $receiptPath -Encoding UTF8
 $receipt|ConvertTo-Json -Depth 8
 Write-Output ('RECEIPT_PATH='+$receiptPath)
 Write-Output ('RECEIPT_SHA256='+(Get-FileHash $receiptPath -Algorithm SHA256).Hash)
} finally {Pop-Location}
