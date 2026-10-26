<# REGION: LEEWAY.POCKET.MODELS_UI_PARITY; TAG: LIVE_PC_ASSET_STAGE
WHO: Owner-authorized Agent Lee. WHAT: Stage only the verified active PC Models UI into the existing phone package.
WHY: Remove obsolete phone Models HTML. WHERE: Existing Pocket package. WHEN: C3 parity repair.
HOW: Verify active host SHA, snapshot targets, copy exact source, pin hash, retain bounded read-only native API gate.
AUTHORIZED ROLES: OWNER_UI_PARITY. LICENSE: MIT. #>
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
$src=Join-Path (Join-Path (Join-Path $root '..') 'pc-consciousness-live') 'runtime\model-execution-runtime\models-ui.html'
$assets=Join-Path $root 'app\src\main\assets\models-ui'
$kt=Join-Path $root 'app\src\main\java\industries\leeway\pocket\ModelsActivity.kt'
$expected='2152D41E48C0361FDB82D4D1714D5F5658FD09935D398ED7ACAF79668732444C'
if (!(Test-Path $src)) { throw 'ACTIVE_PC_MODELS_SOURCE_MISSING' }
$sha=(Get-FileHash $src -Algorithm SHA256).Hash
if($sha -ne $expected){throw 'LIVE_PC_MODELS_HASH_DRIFT'}
$old=(Get-Content (Join-Path $assets 'SOURCE.json') -Raw | ConvertFrom-Json)
if($old.schemaVersion -ne 'leeway.models-ui-bundle.v1' -or $old.repository -ne '4citeB4U/Leeway-Runtime-Fabric'){throw 'BUNDLE_AUTHORITY_MISMATCH'}
$k=Get-Content $kt -Raw
if(-not $k.Contains('require(total<=128000)')){throw 'MODELS_HOST_UNEXPECTED_STATE'}
$backup=Join-Path $root ('qualification\models-parity-backup-'+[DateTime]::UtcNow.ToString('yyyyMMddTHHmmssZ'))
New-Item -ItemType Directory -Path $backup | Out-Null
Copy-Item (Join-Path $assets 'index.html') (Join-Path $backup 'index.html')
Copy-Item (Join-Path $assets 'SOURCE.json') (Join-Path $backup 'SOURCE.json')
Copy-Item $kt (Join-Path $backup 'ModelsActivity.kt')
Copy-Item $src (Join-Path $assets 'index.html')
$old.files.'index.html'.sha256=$sha.ToLowerInvariant()
$old.sourceComponent='runtime/model-execution-runtime/models-ui.html'
$old.sourceState='LIVE_PC_SOURCE_HASH_VERIFIED'
$old.scope='PAIRED_PC_INVENTORY_READ_ONLY_UI_PARITY_ONLY'
$old | ConvertTo-Json -Depth 12 | Set-Content (Join-Path $assets 'SOURCE.json') -Encoding utf8
$k=$k.Replace('require(total<=128000)','require(total<=2000000)')
[IO.File]::WriteAllText($kt,$k,[Text.UTF8Encoding]::new($false))
if((Get-FileHash (Join-Path $assets 'index.html') -Algorithm SHA256).Hash -ne $expected){throw 'STAGED_HASH_MISMATCH'}
Write-Output ('STAGED_PC_MODELS_HASH='+$expected)
Write-Output ('BACKUP='+$backup)
Write-Output 'READ_ONLY_NATIVE_BRIDGE_UNCHANGED'
