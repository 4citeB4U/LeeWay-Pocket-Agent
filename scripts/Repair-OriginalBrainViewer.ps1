<#
REGION: LEEWAY.BRAIN.VIEWER.REPAIR
TAG: REUSE_ORIGINAL_RECURSIVE_RENDERER
WHO: Creator-authorized LeeWay engineering.
WHAT: Connect the recovered original renderer to this installation's read-only Brain.
WHEN: After portable ingestion; WHERE: verified existing Pocket candidate, not a production promotion.
WHY: The original visual system must not be replaced by JSON or ship embedded Creator records.
HOW: Pinned source input, exact extraction boundaries, no original-source mutation; dry-run/apply/hash.
LICENSE: MIT
#>
[CmdletBinding()]
param([Parameter(Mandatory=$true)][string]$SourceHtml,[switch]$Apply,[switch]$RefreshViewerOnly)
Set-StrictMode -Version Latest
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
if((& git -C $root remote get-url origin|Out-String).Trim() -ne 'https://github.com/4citeB4U/LeeWay-Pocket-Agent.git'){throw 'SOURCE_ORIGIN_MISMATCH'}
$base=(& git -C $root rev-parse HEAD|Out-String).Trim()
if($base -ne '11bc1bcc7aef4de646a7ff125f90aeb7b1c38066'){throw 'SOURCE_BASE_CHANGED_REVIEW_REQUIRED'}
$expected='0130D8ECB65F28F4D615D08D8498227DE1CD841B545BEDD81154508B753E46F9'
if((Get-FileHash -LiteralPath $SourceHtml -Algorithm SHA256).Hash -ne $expected){throw 'RECOVERED_VIEWER_HASH_CHANGED'}
$utf8=New-Object Text.UTF8Encoding($false)
function Read-Source([string]$p){[IO.File]::ReadAllText((Join-Path $root $p)).Replace("`r`n","`n")}
function Once([string]$text,[string]$old,[string]$new){if(([regex]::Matches($text,[regex]::Escape($old))).Count -ne 1){throw ('REPLACEMENT_NOT_UNIQUE:'+ $old.Substring(0,[Math]::Min(70,$old.Length)))};$text.Replace($old,$new)}
function Span([string]$text,[string]$start,[string]$end,[string]$replacement){$a=$text.IndexOf($start);$b=$text.IndexOf($end,$a+$start.Length);if($a -lt 0 -or $b -le $a){throw ('ORIGINAL_BOUNDARY_MISSING:'+ $start)};$text.Substring(0,$a)+$replacement+$text.Substring($b)}
$source=[IO.File]::ReadAllText($SourceHtml).Replace("`r`n","`n")
$scripts=[regex]::Matches($source,'(?s)<script\b([^>]*)>(.*?)</script>')
$main=@($scripts|Where-Object{$_.Groups[2].Value.Contains('const DATA=')})
if($main.Count -ne 1){throw 'ORIGINAL_MAIN_SCRIPT_AMBIGUOUS'}
$core=$main[0].Groups[2].Value
$prefix=$source.Substring(0,$main[0].Index)
$dataset=[regex]::Matches($core,'(?m)^const DATA=.+;\s*$')
if($dataset.Count -ne 1){throw 'EMBEDDED_DATA_BOUNDARY_AMBIGUOUS'}
$core=$core.Replace($dataset[0].Value,'const DATA={nodes:Object.create(null),children:Object.create(null),rels:Object.create(null)};'+"`n")
$core=Span $core '// ---- v4: first-class Harness Fabric + Data/Database Fabric ----' 'const stage=document.getElementById("stage")' ''
$core=[regex]::Replace($core,'(?m)^const FALLBACK_D_TOP_LEVEL=.+;\s*$','')
$core=Span $core 'function leewayRelativePathFromBinding(' 'function isWholeBrainSuppressed(' @'
// Physical paths are observations. Navigation consumes opaque local Brain IDs only.
function filesystemPathForNode(id){return N[id]?.source_path||null;}
function isFilesystemNavigationNode(id){return N[id]?.type==='directory'||N[id]?.type==='file';}
async function expandFilesystemNode(){throw Error('LOCAL_BRAIN_BINDING_NOT_READY');}
async function hydratePhysicalFilePreview(){return false;}
const LEEWAY_FS_UNIVERSE_WINDOW=128;
window.LEEWAY_FS_UNIVERSE_WINDOW=128;
window.__leewayFilesystemWindows=Object.create(null);

'@
$core=Span $core 'function buildGalaxy(){' 'function leewaySquareRingSlots(' ('function buildGalaxy(){return;}'+"`n")
$cut=$core.IndexOf('// ---- PRODUCTION LIVE GRAPH BINDING ----');if($cut -lt 0){throw 'ORIGINAL_LIVE_BINDING_BOUNDARY_MISSING'};$core=$core.Substring(0,$cut)
$core=Once $core 'let scope="__GALAXY__",history=[]' 'let scope="__GALAXY__",history=[]'
# Structural links remain; invented adjacent-file transfers are not sensor evidence.
$core=[regex]::Replace($core,'(?s)  if\(scopeNode.domain==="fs" && scopeNode.type==="directory"\)\{\s*for\(let i=1;i<kids.length;i\+\+\)links.push\(\[kids\[i-1\],kids\[i\],"transfer","STAGED_FILE_FLOW"\]\);\s*\}','')
$nucleus=@($scripts|Where-Object{$_.Groups[1].Value -match 'id="absoluteNucleusRuntime"'})
$brain=@($scripts|Where-Object{$_.Groups[1].Value -match 'id="brainUniverseRuntime"'})
if($nucleus.Count -ne 1 -or $brain.Count -ne 1){throw 'ORIGINAL_RENDERER_SCRIPT_AMBIGUOUS'}
$geometry=$brain[0].Groups[2].Value
$import=@($scripts|Where-Object{$_.Groups[1].Value -match 'type="importmap"'})
if($import.Count -ne 1){throw 'IMPORT_MAP_AMBIGUOUS'}
$prefix=$prefix.Replace($import[0].Value,'<script type="importmap">{"imports":{"three":"./vendor/three.module.js","three/addons/controls/OrbitControls.js":"./vendor/OrbitControls.js"}}</script>')
$img=[regex]::Match($prefix,'(?s)<img\b[^>]*id="nucleusFallbackImage"[^>]*>')
if(!$img.Success){throw 'ORIGINAL_NUCLEUS_IMAGE_MISSING'}
$imageData=[regex]::Match($img.Value,'src="data:image/png;base64,([^"]+)"')
if(!$imageData.Success){throw 'ORIGINAL_NUCLEUS_ENCODING_UNSUPPORTED'}
$imageBytes=[Convert]::FromBase64String($imageData.Groups[1].Value)
$sha=[Security.Cryptography.SHA256]::Create();$imageHash=([BitConverter]::ToString($sha.ComputeHash($imageBytes))).Replace('-','');$sha.Dispose()
if($imageHash -ne 'D81DF8E5C08B7782D36692559EC4A1C896ED260C4E7494D657D52CA6427D337F'){throw 'ORIGINAL_NUCLEUS_IMAGE_HASH_MISMATCH'}
$prefix=$prefix.Replace($imageData.Value,'src="nucleus.png"')
$prefix=[regex]::Replace($prefix,'(?s)<button id="wdDiagBtn"[^>]*>.*?</button>','<button id="wdDiagBtn" title="Inspect this device hardware records">Device diagnostics</button>')
$head=@'
<meta http-equiv="Content-Security-Policy" content="default-src 'none'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; connect-src 'none'; font-src 'self'; media-src 'none'; object-src 'none'; frame-src 'none'; base-uri 'none'; form-action 'none'">
<style>
/* Native viewport adaptation only; original geometry, node cards, graph and renderer remain. */
html,body,#absoluteNucleusBackground{background:transparent!important}
#nucleusFallbackImage,#nucleusWaveCanvas{opacity:.24}
#top{background:rgba(2,5,10,.62)!important}
#localControls{position:fixed;z-index:12000;left:8px;right:8px;bottom:58px;display:flex;gap:6px;align-items:center;flex-wrap:wrap;background:rgba(3,9,17,.62);padding:6px;border-radius:12px;backdrop-filter:blur(12px)}
#localSearch{flex:1;min-width:130px}#localSearchResults{position:absolute;bottom:100%;left:0;right:0;display:flex;flex-direction:column;max-height:240px;overflow:auto}
#localPageStatus{font-size:11px}#localEnter{border-color:#50e6ff}#hud dl{overflow-wrap:anywhere}#hud dt{color:#8aa5b9}#hud dd{margin:3px 0 9px}
@media(max-width:760px){#hud{width:calc(100% - 24px);max-height:50vh;top:110px;right:12px}#stage{bottom:142px}.filesystemMatrixGrid{grid-template-columns:repeat(2,minmax(0,1fr))!important}}
</style>
'@
$prefix=Once $prefix '</head>' ($head+"`n</head>")
$controls=@'
<div id="localControls" aria-label="Local Digital Brain navigation">
<button id="localClose">Agent Lee</button><button id="localEnter">Enter Brain</button><button id="localPrev" disabled>Previous</button><button id="localNext" disabled>Next</button><span id="localPageStatus">Binding local Brain</span><button id="localRefresh">Refresh</button>
<input id="localSearch" placeholder="Search this device's Brain" aria-label="Search this device's Brain"><div id="localSearchResults"></div>
</div>
'@
$glue=Read-Source 'app/src/main/assets/digital-brain/original-viewer-native-glue.js'
$entry=@'
window.addEventListener('leeway-enter-local-brain',()=>{
  if(!window.__leewayLocalBrainReady)return;
  const direction=brainCamera.position.clone().sub(orbit.target).normalize();
  brainCamera.position.copy(orbit.target).add(direction.multiplyScalar(5.75));orbit.update();applyDepth();
});
// Fit the original sculpted mesh to the viewport; do not change vertex geometry.
function fitLocalBrainViewport(){
  if(!window.__leewayBrainIntroActive)return;
  const bounds=new THREE.Box3().setFromObject(root),size=bounds.getSize(new THREE.Vector3());
  const tangent=Math.tan(THREE.MathUtils.degToRad(brainCamera.fov)*.5);
  const distance=Math.max(18.8,Math.max(size.y/(2*tangent),size.x/(2*tangent*Math.max(.1,brainCamera.aspect)))*1.18);
  const direction=brainCamera.position.clone().sub(orbit.target).normalize();
  brainCamera.position.copy(orbit.target).add(direction.multiplyScalar(distance));orbit.update();applyDepth();
}
window.addEventListener('resize',fitLocalBrainViewport);fitLocalBrainViewport();
window.__leewayOriginalBrain3D={geometrySource:'RECOVERED_ORIGINAL_HEMISPHERE_GEOMETRY',renderer:'THREE.WebGLRenderer',ready:true};
'@
$html=$prefix+"`n"+$controls+"`n<script src=`"local-brain-binding.js`"></script>`n<script>`n"+$core+"`n"+$glue+"`n</script>`n"+$nucleus[0].Value+"`n<script type=`"module`" id=`"brainUniverseRuntime`">`n"+$geometry+"`n"+$entry+"`n</script>`n</body></html>"
if($html -match 'phone-fold6|FoldDigitalBrain|FALLBACK_D_TOP_LEVEL|D:\\\\Leeway|lw22-demo-runtime'){throw 'LEGACY_MACHINE_OR_DEMO_PAYLOAD_REMAINS'}
$mainPath='app/src/main/java/industries/leeway/pocket/MainActivity.kt'
$main=Read-Source $mainPath
if(!$RefreshViewerOnly){$main=Once $main '        @JavascriptInterface fun digitalBrain():String' '        @JavascriptInterface fun openDigitalBrain(){runOnUiThread{startActivity(Intent(this@MainActivity,DigitalBrainActivity::class.java))}}
        @JavascriptInterface fun digitalBrain():String'
}
$manifestPath='app/src/main/AndroidManifest.xml'
$manifest=Read-Source $manifestPath
if(!$RefreshViewerOnly){$manifest=Once $manifest '        <service' '        <activity android:name=".DigitalBrainActivity" android:exported="false" android:theme="@style/AppTheme.TransparentVoice" />
        <service'
}
$spherePath='app/src/main/assets/agent_lee_sphere_transparent.html'
$sphere=Read-Source $spherePath
if(!$RefreshViewerOnly){$sphere=Span $sphere '    function openDigitalBrain() {' '    function openWorkstation()' @'
    function openDigitalBrain() {
      closeDrawer();
      if(window.LeeWayAndroid && typeof window.LeeWayAndroid.openDigitalBrain==='function'){
        window.LeeWayAndroid.openDigitalBrain();return;
      }
      voiceUnavailable('DIGITAL_BRAIN_NATIVE_VIEWER_UNBOUND');
    }
'@
}
$changes=[ordered]@{'app/src/main/assets/digital-brain/brain.html'=$html;$mainPath=$main;$manifestPath=$manifest;$spherePath=$sphere}
$plan=[ordered]@{status='DRY_RUN';sourceBase=$base;recoveredOriginalSha256=$expected;originalNucleusSha256=$imageHash;originalHtmlBytes=(Get-Item $SourceHtml).Length;derivedHtmlBytes=[Text.Encoding]::UTF8.GetByteCount($html);retainedRenderer='ORIGINAL_GRAPH_LAYOUT_CARDS_HUD_AND_3D_GEOMETRY';dataSource='NATIVE_OWNER_LOCAL_BRAIN';embeddedOriginalNodeCount=268;embeddedOriginalRecordsShipped=0;recoveredOldLockMatchesCurrentFile=$false;scope='READ_ONLY_VIEWER_CANDIDATE_NOT_GOLDEN_UI_PROMOTION';changedPaths=@($changes.Keys);installed=$false}
if(!$Apply){$plan|ConvertTo-Json -Depth 8;return}
foreach($p in $changes.Keys){[IO.File]::WriteAllText((Join-Path $root $p),$changes[$p],$utf8)}
[IO.File]::WriteAllBytes((Join-Path $root 'app/src/main/assets/digital-brain/nucleus.png'),$imageBytes)
$plan.status='DERIVED_VIEWER_CREATED_TESTS_REQUIRED'
$plan['files']=@(foreach($p in $changes.Keys){@{path=$p;sha256=(Get-FileHash (Join-Path $root $p) -Algorithm SHA256).Hash}})
$geometryHash=([BitConverter]::ToString([Security.Cryptography.SHA256]::Create().ComputeHash([Text.Encoding]::UTF8.GetBytes($geometry)))).Replace('-','')
$plan['original3DScriptSha256']=$geometryHash
$plan|ConvertTo-Json -Depth 8|Set-Content -LiteralPath (Join-Path $root 'contracts/original-brain-viewer-projection.v1.json') -Encoding UTF8
$plan|ConvertTo-Json -Depth 8
