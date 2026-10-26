$ErrorActionPreference='Stop'
$r='E:\LeeWay-Work\agent-lee-fold6-unified'
$base=Join-Path $r 'app\src\main\java\industries\leeway\pocket'
$round=Join-Path $base 'RoundBoxWindow.kt'
$three=Join-Path $base 'ThreeSurfaceActivity.kt'
$v=Join-Path $r 'app\build.gradle.kts'
foreach($p in @($round,$three,$v)){Copy-Item $p ($p+'.a2-webview-security-before.bak') -Force}
foreach($p in @($round,$three)){
 $s=[IO.File]::ReadAllText($p)
 if(!$s.Contains('import android.webkit.WebView')){throw "MISSING_WEBVIEW_IMPORT $p"}
 $s=$s.Replace('import android.webkit.WebView'+[Environment]::NewLine,'import android.webkit.WebView'+[Environment]::NewLine+'import android.webkit.WebViewClient'+[Environment]::NewLine+'import android.webkit.WebResourceRequest'+[Environment]::NewLine+'import android.webkit.WebSettings'+[Environment]::NewLine)
 $old='settings.allowFileAccess=false'
 if(!$s.Contains($old)){throw "FILE_SETTING_CHANGED $p"}
 $s=$s.Replace($old,$old+[Environment]::NewLine+'            settings.allowContentAccess=false'+[Environment]::NewLine+'            settings.allowFileAccessFromFileURLs=false'+[Environment]::NewLine+'            settings.allowUniversalAccessFromFileURLs=false'+[Environment]::NewLine+'            settings.mixedContentMode=WebSettings.MIXED_CONTENT_NEVER_ALLOW')
 if($p -eq $round){
 $anchor='addJavascriptInterface(Bridge(),"LeeWayAndroid")'
 $client='webViewClient=object:WebViewClient(){override fun shouldOverrideUrlLoading(view:WebView,request:WebResourceRequest):Boolean{return request.isForMainFrame && request.url.toString()!="file:///android_asset/round-box-leeway.html"}}'
 }else{
 $anchor='addJavascriptInterface(OwnerBridge(),"LeeWayAndroid")'
 $client='webViewClient=object:WebViewClient(){override fun shouldOverrideUrlLoading(view:WebView,request:WebResourceRequest):Boolean{return request.isForMainFrame && !(request.url.scheme=="file" && request.url.path=="/android_asset/leeway-three-host.html" && request.url.getQueryParameter("surface") in setOf("about","evidence","authority"))}}'
 }
 if(!$s.Contains($anchor)){throw "BRIDGE_ANCHOR_CHANGED $p"}
 $s=$s.Replace($anchor,$client+[Environment]::NewLine+'            '+$anchor)
 [IO.File]::WriteAllText($p,$s,(New-Object System.Text.UTF8Encoding($false)))
 Write-Output ((Split-Path $p -Leaf)+' HASH='+(Get-FileHash $p -Algorithm SHA256).Hash)
}
$a=[IO.File]::ReadAllText($v)
if(!$a.Contains('versionCode = 70')){throw 'VERSION_CONFLICT'}
[IO.File]::WriteAllText($v,$a.Replace('versionCode = 70','versionCode = 71').Replace('1.0.0-hamburger-chevron-rc43','1.0.0-webview-guard-rc44'),(New-Object System.Text.UTF8Encoding($false)))
