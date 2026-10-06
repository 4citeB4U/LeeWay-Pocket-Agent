<#
REGION: LEEWAY.BRAIN.VIEWER.REPAIR
TAG: NARROW_VIEWPORT_AND_READ_BOUNDARY_CORRECTIONS
WHO: Creator-authorized Agent Lee; WHAT: Repair observed mobile navigation and retained host-specific chrome.
WHEN: After actual browser failure; WHERE: existing source candidate only.
WHY: An inspector must not cover the second tap; recovery-source hardware labels are not customer hardware.
HOW: Exact source replacements, original renderer preserved, regeneration through the saved original-viewer script.
LICENSE: MIT
#>
[CmdletBinding()]
param([switch]$Apply)
Set-StrictMode -Version Latest
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
if((& git -C $root rev-parse HEAD|Out-String).Trim() -ne '11bc1bcc7aef4de646a7ff125f90aeb7b1c38066'){throw 'SOURCE_BASE_CHANGED'}
$enc=New-Object Text.UTF8Encoding($false)
function Read-Source([string]$p){[IO.File]::ReadAllText((Join-Path $root $p)).Replace("`r`n","`n")}
function Once([string]$s,[string]$old,[string]$new){if(([regex]::Matches($s,[regex]::Escape($old))).Count -ne 1){throw ('EXACT_BOUNDARY_MISSING:'+ $old.Substring(0,[Math]::Min(65,$old.Length)))};$s.Replace($old,$new)}
$changes=[ordered]@{}
$p='brain-core/src/main/kotlin/industries/leeway/brain/BrainViewer.kt'
$s=Read-Source $p
$s=Once $s 'fun node(id:String):BrainViewNode {authority();scoped(id);return validate(store.node(id)?:error("VIEWER_NODE_NOT_FOUND"))}' 'fun node(id:String):BrainViewNode {authority();scoped(id);val result=validate(store.node(id)?:error("VIEWER_NODE_NOT_FOUND"));check(result.id==id){"VIEWER_RETURNED_NODE_MISMATCH"};return result}'
$changes[$p]=$s
$p='app/src/main/assets/digital-brain/original-viewer-native-glue.js'
$s=Read-Source $p
$s=Once $s 'let localBinding=null;' 'let localBinding=null;
let historyTraversal=false;'
$s=Once $s 'goBack=function(){const id=history.pop();if(id&&localBinding)safeView(localBinding.load(id));};' 'goBack=function(){const id=history.pop();if(id&&localBinding){historyTraversal=true;safeView(localBinding.load(id).finally(()=>historyTraversal=false));}};'
$s=Once $s 'if(scope&&scope!==id&&!history.includes(scope))history.push(scope);' 'if(!historyTraversal&&scope&&scope!==id)history.push(scope);'
$s=Once $s 'const originalHud=renderHud;' @'
// Keep original card geometry. Narrow touch layouts enter expandable nodes on one tap.
nodesLayer.addEventListener('click',event=>{
  const node=event.target.closest?.('.node');
  if(innerWidth>760||!node)return;
  const id=node.dataset.id;
  if(id&&canEnterUniverse(id)){event.preventDefault();event.stopImmediatePropagation();safeView(enterNode(id));}
},true);
previewSectionForNode=function(id){
  const n=N[id];if(!n||n.type!=='file')return '';
  return '<section class="hudSection"><div class="hudSectionTitle">FILE METADATA RECORD</div><p>'+esc(n.label)+'</p><p class="sourcePath">'+esc(n.source_path||'Source location not supplied in this record')+'</p><p>Content preview is not bound. No file contents or content hash are claimed.</p></section>';
};
const hardwareButton=document.getElementById('wdDiagBtn');if(hardwareButton)hardwareButton.onclick=()=>safeView(localBinding.load(VIEW_ROOT+':system:hardware'));
const originalHud=renderHud;
'@
$changes[$p]=$s
$p='scripts/Repair-OriginalBrainViewer.ps1'
$s=Read-Source $p
$s=Once $s '[string]$SourceHtml,[switch]$Apply)' '[string]$SourceHtml,[switch]$Apply,[switch]$RefreshViewerOnly)'
$s=Once $s '$head=@''' @'
$prefix=[regex]::Replace($prefix,'(?s)<button id="wdDiagBtn"[^>]*>.*?</button>','<button id="wdDiagBtn" title="Inspect this device hardware records">Device diagnostics</button>')
$head=@'
'@
$s=Once $s 'window.__leewayOriginalBrain3D={geometrySource:' @'
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
window.__leewayOriginalBrain3D={geometrySource:
'@
$s=Once $s '$main=Once $main ' 'if(!$RefreshViewerOnly){$main=Once $main '
$s=Once $s ("`n"+'$manifestPath=') ("`n}`n"+'$manifestPath=')
$s=Once $s '$manifest=Once $manifest ' 'if(!$RefreshViewerOnly){$manifest=Once $manifest '
$s=Once $s ("`n"+'$spherePath=') ("`n}`n"+'$spherePath=')
$s=Once $s '$sphere=Span $sphere ' 'if(!$RefreshViewerOnly){$sphere=Span $sphere '
$s=Once $s "'@`n`$changes=[ordered]@" "'@`n}`n`$changes=[ordered]@"
$changes[$p]=$s
if(!$Apply){[pscustomobject]@{status='DRY_RUN';paths=@($changes.Keys);originalShapeChanged=$false;phoneModified=$false}|ConvertTo-Json;return}
foreach($path in $changes.Keys){[IO.File]::WriteAllText((Join-Path $root $path),$changes[$path],$enc)}
$tokens=$null;$errors=$null;[void][Management.Automation.Language.Parser]::ParseFile((Join-Path $root 'scripts/Repair-OriginalBrainViewer.ps1'),[ref]$tokens,[ref]$errors)
if($errors.Count){$errors|ForEach-Object{$_.Message};throw 'REGENERATION_SCRIPT_PARSE_FAILED'}
'BEHAVIOR_REPAIRED_REGENERATION_AND_TESTS_REQUIRED'
