<#
REGION: LEEWAY.POCKET.RELEASE_QUALIFICATION
TAG: GOLDEN_GATE_SOURCE_CHECKPOINT
WHO: Creator-authorized Agent Lee
WHAT: Preserve only the reviewed gate, contract, tests and sanitized qualification receipt.
WHEN: After gate tests and native hash readback; WHERE: isolated Pocket branch.
WHY: Protect progress without promoting an incomplete APK or publishing customer data.
HOW: Verify branch, base and tested hashes; explicit staging allowlist; commit and push branch only.
LICENSE: MIT
#>
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
$branch='qualification/golden-customer-release-20261005'
if((& git -C $root remote get-url origin|Out-String).Trim() -ne 'https://github.com/4citeB4U/LeeWay-Pocket-Agent.git'){throw 'WRONG_ORIGIN'}
if((& git -C $root branch --show-current|Out-String).Trim() -ne $branch){throw 'WRONG_BRANCH'}
if((& git -C $root rev-parse HEAD|Out-String).Trim() -ne '6ec2dc9c4c54deb09c3190e1c0e560a9b6164113'){throw 'SOURCE_BASE_CHANGED'}
if(@(& git -C $root diff --cached --name-only).Count){throw 'EXISTING_STAGED_WORK_REVIEW_REQUIRED'}
$receiptPath=Join-Path $root 'docs\qualification\golden-release-gate-20261005.json'
$receipt=Get-Content $receiptPath -Raw|ConvertFrom-Json
if($receipt.status -ne 'VERIFIED_RELEASE_GATE_REGRESSIONS_NOT_PRODUCT_ACCEPTANCE' -or $receipt.unitTestsFailed -ne 0 -or $receipt.candidateReleaseState -ne 'BLOCKED'){throw 'QUALIFICATION_BOUNDARY_INVALID'}
foreach($entry in $receipt.sourceHashes){if((Get-FileHash (Join-Path $root $entry.path) -Algorithm SHA256).Hash -ne $entry.sha256){throw 'SOURCE_CHANGED_AFTER_TEST'}}
$paths=@('Verify-SingleApk.ps1','contracts/golden-apk-release.v1.json','scripts/golden-release-gate.mjs','scripts/Apply-GoldenReleaseGate.ps1','scripts/Invoke-GoldenGateQualification.ps1','tests/golden-release-gate.test.mjs','docs/qualification/golden-release-gate-20261005.json')
foreach($p in $paths){$f=Get-Item (Join-Path $root $p);if($f.Length -gt 131072){throw 'UNEXPECTED_PAYLOAD_SIZE'};$text=[IO.File]::ReadAllText($f.FullName);if($text -match '-----BEGIN (RSA |EC |OPENSSH )?PRIVATE KEY-----|gh[pousr]_[A-Za-z0-9]{30,}|github_pat_[A-Za-z0-9_]{40,}'){throw 'POTENTIAL_CREDENTIAL_BLOCK'}}
& git -C $root -c core.autocrlf=false add -- @paths
if($LASTEXITCODE -ne 0){throw 'STAGING_FAILED'}
& git -C $root -c user.name='LeeWay Qualification' -c user.email='qualification@local.leeway' commit -m 'Require golden customer APK proof for owner-retained Brain, freshness and all mandatory components'
if($LASTEXITCODE -ne 0){throw 'COMMIT_FAILED'}
$commit=(& git -C $root rev-parse HEAD|Out-String).Trim()
& git -C $root push origin ('refs/heads/'+$branch+':refs/heads/'+$branch)
if($LASTEXITCODE -ne 0){throw 'PUSH_FAILED_LOCAL_COMMIT_PRESERVED'}
[pscustomobject]@{branch=$branch;commit=$commit;mainModified=$false;apkPublished=$false;installed=$false}|ConvertTo-Json
