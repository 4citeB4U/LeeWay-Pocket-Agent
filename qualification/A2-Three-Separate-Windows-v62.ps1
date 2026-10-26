$ErrorActionPreference='Stop'
$r='E:\LeeWay-Work\agent-lee-fold6-unified'
$base=Join-Path $r 'app\src\main'
$manifest=Join-Path $base 'AndroidManifest.xml'
$native=Join-Path $base 'java\industries\leeway\pocket\FloatingSphereWindow.kt'
$html=Join-Path $base 'assets\agent_lee_sphere_transparent.html'
$js=Join-Path $base 'assets\leeway-three-reference-screens.js'
$v=Join-Path $r 'app\build.gradle.kts'
foreach($p in @($manifest,$native,$html,$js,$v)){Copy-Item $p ($p+'.a2-separate-screens-before.bak') -Force}
$s=[IO.File]::ReadAllText($manifest)
$anchor='<activity android:name=".DigitalBrainActivity"'
if(!$s.Contains($anchor)){throw 'MANIFEST_ANCHOR_CHANGED'}
$s=$s.Replace($anchor,'<activity android:name=".ThreeSurfaceActivity" android:exported="false" android:theme="@style/AppTheme.TransparentVoice" />'+[Environment]::NewLine+'          '+$anchor)
[IO.File]::WriteAllText($manifest,$s,(New-Object System.Text.UTF8Encoding($false)))
$s=[IO.File]::ReadAllText($native)
$anchor='        @JavascriptInterface fun openDigitalBrain()'
if(!$s.Contains($anchor)){throw 'NATIVE_BRIDGE_CHANGED'}
$methods=@'
        private fun openThree(key:String){main.post{service.startActivity(Intent(service,ThreeSurfaceActivity::class.java).putExtra("surface",key).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))}}
        @JavascriptInterface fun openAbout(){openThree("about")}
        @JavascriptInterface fun openEvidence(){openThree("evidence")}
        @JavascriptInterface fun openAuthority(){openThree("authority")}
'@
$s=$s.Replace($anchor,$methods+$anchor)
[IO.File]::WriteAllText($native,$s,(New-Object System.Text.UTF8Encoding($false)))
$s=[IO.File]::ReadAllText($html)
$anchor="const phoneMethods={'digital-brain':"
if(!$s.Contains($anchor)){throw 'MENU_DISPATCH_CHANGED'}
$s=$s.Replace($anchor,"const phoneMethods={about:'openAbout',evidence:'openEvidence',authority:'openAuthority','digital-brain':")
[IO.File]::WriteAllText($html,$s,(New-Object System.Text.UTF8Encoding($false)))
$s=[IO.File]::ReadAllText($js)
$before="close.onclick=()=>{document.getElementById('surface-panel').classList.remove('open','leeway-three-open');window.__leewayFloating?.host('panel',false)}"
$after="close.onclick=()=>{if(window.LeeWayAndroid?.closeSurface){window.LeeWayAndroid.closeSurface();return;}document.getElementById('surface-panel').classList.remove('open','leeway-three-open');window.__leewayFloating?.host('panel',false)}"
if(!$s.Contains($before)){throw 'CLOSE_ANCHOR_CHANGED'}
$s=$s.Replace($before,$after)
[IO.File]::WriteAllText($js,$s,(New-Object System.Text.UTF8Encoding($false)))
$s=[IO.File]::ReadAllText($v)
if(!$s.Contains('versionCode = 61')){throw 'VERSION_CONFLICT'}
[IO.File]::WriteAllText($v,$s.Replace('versionCode = 61','versionCode = 62').Replace('1.0.0-three-reference-screens-rc34','1.0.0-three-independent-screens-rc35'),(New-Object System.Text.UTF8Encoding($false)))
Get-FileHash $manifest,$native,$html,$js,$v -Algorithm SHA256 | ForEach-Object{$_.Path+' '+$_.Hash}
