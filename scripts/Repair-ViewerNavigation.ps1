<#
REGION: LEEWAY.BRAIN.VIEWER.REPAIR
TAG: SEARCH_PAGE_AND_BACK_BEHAVIOR
WHO: Creator-authorized engineering; WHAT: Keep searched objects on their actual parent page and fix Back history.
WHEN: Browser qualification; WHERE: existing native viewer adapter and portable read contract.
WHY: A visible inspector alone must not hide that the selected object is off-page.
HOW: Parameterized sibling rank, portable bounds and existing renderer handoff; no data mutation.
LICENSE: MIT
#>
[CmdletBinding()]
param([switch]$Apply)
Set-StrictMode -Version Latest
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
if((& git -C $root rev-parse HEAD|Out-String).Trim() -ne '11bc1bcc7aef4de646a7ff125f90aeb7b1c38066'){throw 'SOURCE_HEAD_CHANGED'}
function Read-Source([string]$p){[IO.File]::ReadAllText((Join-Path $root $p)).Replace("`r`n","`n")}
function Once([string]$s,[string]$old,[string]$new){if(([regex]::Matches($s,[regex]::Escape($old))).Count -ne 1){throw ('EXACT_BOUNDARY_MISSING:'+ $old.Substring(0,[Math]::Min(70,$old.Length)))};$s.Replace($old,$new)}
$changes=[ordered]@{}
$p='brain-core/src/main/kotlin/industries/leeway/brain/BrainViewer.kt';$s=Read-Source $p
$s=Once $s '    fun search(query:String,limit:Int):List<BrainViewNode>' '    fun childIndex(parentId:String,childId:String):Long
    fun search(query:String,limit:Int):List<BrainViewNode>'
$s=Once $s '    fun ancestors(id:String):List<BrainViewNode> {' @'
    fun pageOffset(id:String):Int {
        val child=node(id);val parent=child.parentId?:return 0
        val position=store.childIndex(parent,id)
        check(position>=0 && position<node(parent).childCount && position<=1000000){"VIEWER_CHILD_POSITION_INVALID"}
        return (position/PAGE_LIMIT*PAGE_LIMIT).toInt()
    }
    fun ancestors(id:String):List<BrainViewNode> {
'@
$changes[$p]=$s
$p='app/src/main/java/industries/leeway/pocket/AndroidBrainViewer.kt';$s=Read-Source $p
$s=Once $s '.put("ancestors",JSONArray(viewer.ancestors(id).map{encode(it)}))' '.put("pageOffset",viewer.pageOffset(id)).put("ancestors",JSONArray(viewer.ancestors(id).map{encode(it)}))'
$s=Once $s 'ORDER BY n.title COLLATE NOCASE,n.id LIMIT ? OFFSET ?' 'ORDER BY COALESCE(n.title,'''') COLLATE NOCASE,n.id LIMIT ? OFFSET ?'
$s=Once $s '        override fun search(query:String,limit:Int)=' @'
        override fun childIndex(parentId:String,childId:String):Long = db.rawQuery("SELECT COUNT(*) FROM nodes c JOIN nodes n ON n.id=? AND n.parent_id=? WHERE c.parent_id=n.parent_id AND COALESCE(c.status,'')!='tombstoned' AND (COALESCE(c.title,'') COLLATE NOCASE < COALESCE(n.title,'') COLLATE NOCASE OR (COALESCE(c.title,'') COLLATE NOCASE = COALESCE(n.title,'') COLLATE NOCASE AND c.id<n.id))",arrayOf(childId,parentId)).use{c->check(c.moveToFirst());c.getLong(0)}
        override fun search(query:String,limit:Int)=
'@
$changes[$p]=$s
$p='brain-core/src/test/kotlin/industries/leeway/brain/BrainViewerTest.kt';$s=Read-Source $p
$s=Once $s '        override fun node(id:String)=rows[id]' '        var mismatchedNode=false
        override fun node(id:String)=if(mismatchedNode)rows["$root:user"] else rows[id]'
$s=Once $s '        override fun search(query:String,limit:Int)=' '        override fun childIndex(parentId:String,childId:String)=rows.values.filter{it.parentId==parentId}.indexOfFirst{it.id==childId}.toLong()
        override fun search(query:String,limit:Int)='
$s=Once $s '    @Test fun rootComesFromTheSameBrainIdentity()' @'
    @Test fun returnedNodeMustMatchRequestedIdentity(){val s=Store();s.mismatchedNode=true;assertThrows(IllegalStateException::class.java){BrainViewer(identity,s).root()}}
    @Test fun exactSiblingRankSelectsTheCorrectPage(){val s=Store();val parent="$root:page";s.rows[parent]=row(parent,root,257);repeat(257){i->s.rows["$parent:$i"]=row("$parent:$i",parent)};assertEquals(256,BrainViewer(identity,s).pageOffset("$parent:256"));assertEquals(0,BrainViewer(identity,s).pageOffset(root))}
    @Test fun rootComesFromTheSameBrainIdentity()
'@
$changes[$p]=$s
$p='app/src/main/assets/digital-brain/local-brain-binding.js';$s=Read-Source $p
$s=Once $s 'if(parent)await load(parent);renderer.select(id);' 'if(parent){const offset=result.pageOffset;if(!Number.isInteger(offset)||offset<0||offset%128!==0)throw Error(''VIEWER_SEARCH_PAGE_INVALID'');const page=await load(parent,offset);if(!page?.nodes.some(n=>n.id===id))throw Error(''VIEWER_SEARCH_RESULT_NOT_IN_PAGE'');}renderer.select(id);'
$changes[$p]=$s
$p='app/src/main/assets/digital-brain/original-viewer-native-glue.js';$s=Read-Source $p
$s=Once $s 'goHome=function(){if(localBinding)safeView(localBinding.home());};' 'goHome=function(){if(localBinding){history=[];historyTraversal=true;safeView(localBinding.home().finally(()=>historyTraversal=false));}};'
$s=Once $s 'window.__leewayLocalBrain=localBinding;' 'window.__leewayLocalBrain=localBinding;
document.getElementById(''homeBtn'').onclick=goHome;document.getElementById(''backBtn'').onclick=goBack;'
$changes[$p]=$s
if(!$Apply){[pscustomobject]@{status='DRY_RUN';paths=@($changes.Keys);phoneModified=$false}|ConvertTo-Json;return}
$enc=New-Object Text.UTF8Encoding($false);foreach($path in $changes.Keys){[IO.File]::WriteAllText((Join-Path $root $path),$changes[$path],$enc)}
'NAVIGATION_REPAIRED_REGENERATION_AND_TEST_REQUIRED'
