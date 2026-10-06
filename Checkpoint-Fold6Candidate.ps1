<#
REGION: POCKET.CHECKPOINT
TAG: FOLD6_QUALIFICATION_CHECKPOINT
WHO: Creator-authorized Agent Lee
WHAT: Preserve the compiled but incomplete candidate on an isolated Git branch.
WHEN: After source/build qualification; WHERE: existing Pocket checkout only.
WHY: Prevent local-only drift without promoting a partial build or publishing device data.
HOW: Verify origin/base, explicit path allowlist, reject binary/key artifacts, commit and push candidate branch only.
LICENSE: MIT
#>
$ErrorActionPreference='Stop'
Set-StrictMode -Version Latest
$r=$PSScriptRoot
$branch='qualification/fold6-single-apk-gates-20261005'
if((& git -C $r remote get-url origin|Out-String).Trim() -ne 'https://github.com/4citeB4U/LeeWay-Pocket-Agent.git'){throw 'WRONG_ORIGIN'}
if((& git -C $r rev-parse HEAD|Out-String).Trim() -ne 'a43ba6217216cd1ade6b058f39fd16cd31cdedd2'){throw 'BASE_CHANGED_REVIEW_REQUIRED'}
$staged=@(& git -C $r diff --cached --name-only)
if($staged.Count){throw 'EXISTING_STAGED_WORK_REVIEW_REQUIRED'}
$sourceManifest=Join-Path $r 'qualification\fold6-build-20261006T005247097Z\source-sha256.json'
foreach($file in (Get-Content $sourceManifest -Raw|ConvertFrom-Json)){
 if((Get-FileHash (Join-Path $r $file.path) -Algorithm SHA256).Hash -ne $file.sha256){throw ('SOURCE_CHANGED_AFTER_TEST:'+ $file.path)}
}
$paths=@('app/build.gradle.kts','app/src','tests/sphere-voice-boundary.test.mjs','Invoke-Fold6BuildQualification.ps1','Repair-Fold6Admission.ps1','Repair-Fold6UiFallback.ps1','Verify-SingleApk.ps1','docs/qualification/fold6-single-apk-20261005.json','Checkpoint-Fold6Candidate.ps1')
$files=@(foreach($p in $paths){$f=Get-Item (Join-Path $r $p);if($f.PSIsContainer){Get-ChildItem $f.FullName -Recurse -File}else{$f}})
foreach($f in $files){
 if($f.Extension -in @('.apk','.db','.sqlite','.jks','.keystore','.p12','.onnx','.bin') -or $f.Length -gt 262144){throw ('NON_SOURCE_ARTIFACT_REVIEW_REQUIRED:'+ $f.Name)}
 $text=[IO.File]::ReadAllText($f.FullName)
 if($text -match '-----BEGIN (RSA |EC |OPENSSH )?PRIVATE KEY-----|gh[pousr]_[A-Za-z0-9]{30,}|github_pat_[A-Za-z0-9_]{40,}') {throw ('POTENTIAL_CREDENTIAL_BLOCK:'+ $f.Name)}
}
& git -C $r checkout -b $branch
if($LASTEXITCODE -ne 0){throw 'CANDIDATE_BRANCH_NOT_CREATED'}
& git -C $r -c core.autocrlf=false add -A -- @paths
if($LASTEXITCODE -ne 0){throw 'STAGING_FAILED'}
$names=@(& git -C $r diff --cached --name-only)
if($names|Where-Object {$_ -match '^(qualification/)|\.(apk|db|sqlite|jks|keystore|p12|onnx|bin)$'}){throw 'STAGED_PATH_NOT_ALLOWED'}
& git -C $r -c user.name='LeeWay Qualification' -c user.email='qualification@local.leeway' commit -m 'Checkpoint incomplete Fold6 candidate with verified build and truthful admission gates'
if($LASTEXITCODE -ne 0){throw 'CHECKPOINT_COMMIT_FAILED'}
$commit=(& git -C $r rev-parse HEAD|Out-String).Trim()
& git -C $r push origin ('refs/heads/'+$branch+':refs/heads/'+$branch)
if($LASTEXITCODE -ne 0){throw 'CHECKPOINT_PUSH_FAILED'}
[ordered]@{branch=$branch;commit=$commit;status='CANDIDATE_SOURCE_PRESERVED';promoted=$false;installed=$false;files=$names}|ConvertTo-Json -Depth 5
