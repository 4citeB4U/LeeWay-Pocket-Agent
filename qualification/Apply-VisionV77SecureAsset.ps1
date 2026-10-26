$ErrorActionPreference='Stop'
$root='E:\LeeWay-Work\agent-lee-fold6-unified'
$stage='E:\LeeWay-Work\c3-vision-lane-a-qualification\qualification'
$html=Join-Path $root 'app\src\main\assets\agent_lee_sphere_transparent.html'
$native=Join-Path $root 'app\src\main\java\industries\leeway\pocket\FloatingSphereWindow.kt'
$gradle=Join-Path $root 'app\build.gradle.kts'
$vision='E:\LeeWay-Work\vision-50-test-worktree-20261010\index.html'
$expected=@{
 $html='79D46FDAE815DB8AA3A4357DBE7CD2EE1FEB6590ABCBBBB860A90947A281B3EF'
 $native='3509E3FD442E6E587AA4DD23EC4A1E4A3B6FCDBDF09940C4DFED37F04A8E1D6B'
 $gradle='1B5B07BF4BBAB3F07FDC0CC7402FAF3393130BF2D843B70347BFF429DCACA74D'
 $vision='5328B7515E6337E7311812D5FF9101E4155FB8163FA5E3323B44B4318BC3941C'
}
foreach($f in $expected.Keys){if((Get-FileHash $f).Hash -ne $expected[$f]){throw ('SOURCE_DRIFT '+$f)}}
$backup=Join-Path $stage ('v76-scoped-backup-'+[DateTime]::UtcNow.ToString('yyyyMMddTHHmmssZ'))
New-Item -ItemType Directory $backup | Out-Null
Copy-Item $html (Join-Path $backup 'agent_lee_sphere_transparent.html')
Copy-Item $native (Join-Path $backup 'FloatingSphereWindow.kt')
Copy-Item $gradle (Join-Path $backup 'build.gradle.kts')
$h=[IO.File]::ReadAllText($html)
$old='https://leeway-agent-lee-vision.vercel.app/'
$new='https://vision.leeway.invalid/vision/index.html'
if(([regex]::Matches($h,[regex]::Escape($old))).Count -ne 1){throw 'AMBIGUOUS_VISION_URL'}
$h=$h.Replace($old,$new)
$n=[IO.File]::ReadAllText($native)
$oldOverride='override fun shouldOverrideUrlLoading(v:WebView,request:WebResourceRequest)=request.url.toString()!="file:///android_asset/agent_lee_sphere_transparent.html"'
$newOverride='override fun shouldOverrideUrlLoading(v:WebView,request:WebResourceRequest)= !(request.url.toString()=="file:///android_asset/agent_lee_sphere_transparent.html" || (!request.isForMainFrame && request.url.toString()=="https://vision.leeway.invalid/vision/index.html"))'
if(-not $n.Contains($oldOverride)){throw 'NATIVE_URL_GUARD_DRIFT'}
$n=$n.Replace($oldOverride,$newOverride)
$anchor='if(uri.scheme=="file"&&uri.path?.startsWith("/android_asset/")==true)return null'
$addition=@'
                    // TAG: GOVERNED_LOCAL_VISION_HTTPS_ASSET. Exact URL only, no remote pass-through.
                    if(uri.toString()=="https://vision.leeway.invalid/vision/index.html") {
                        return WebResourceResponse("text/html","UTF-8",service.assets.open("vision/index.html"))
                    }
'@
if(-not $n.Contains($anchor)){throw 'INTERCEPTOR_DRIFT'}
$n=$n.Replace($anchor,$anchor + [Environment]::NewLine + $addition.TrimEnd())
$load='            loadUrl("file:///android_asset/agent_lee_sphere_transparent.html")'
$chrome=@'
            // TAG: VISION_CAMERA_ORIGIN_GATE. Keep external and audio grants denied.
            webChromeClient=object:WebChromeClient(){
                override fun onPermissionRequest(request:PermissionRequest){
                    val origin=request.origin.toString().trimEnd('/')
                    val onlyCamera=request.resources.size==1 && request.resources[0]==PermissionRequest.RESOURCE_VIDEO_CAPTURE
                    val androidGranted=service.checkSelfPermission(android.Manifest.permission.CAMERA)==android.content.pm.PackageManager.PERMISSION_GRANTED
                    if(origin=="https://vision.leeway.invalid" && onlyCamera && androidGranted && panel){
                        request.grant(arrayOf(PermissionRequest.RESOURCE_VIDEO_CAPTURE))
                    } else request.deny()
                }
            }
'@
if(-not $n.Contains($load)){throw 'LOAD_DRIFT'}
$n=$n.Replace($load,$chrome + [Environment]::NewLine + $load)
$g=[IO.File]::ReadAllText($gradle)
if(-not $g.Contains('versionCode = 76')){throw 'VERSION_DRIFT'}
$g=$g.Replace('versionCode = 76','versionCode = 77').Replace('versionName = "1.0.0-vision-in-package-rc49"','versionName = "1.0.0-vision-secure-local-rc50"')
$dest=Join-Path $root 'app\src\main\assets\vision\index.html'
New-Item -ItemType Directory (Split-Path $dest -Parent) -Force | Out-Null
Copy-Item $vision $dest
[IO.File]::WriteAllText($html,$h,[Text.UTF8Encoding]::new($false))
[IO.File]::WriteAllText($native,$n,[Text.UTF8Encoding]::new($false))
[IO.File]::WriteAllText($gradle,$g,[Text.UTF8Encoding]::new($false))
[pscustomobject]@{state='SECURE_LOCAL_VISION_SOURCE_STAGED';backup=$backup;version=77;visionAssetHash=(Get-FileHash $dest).Hash;htmlHash=(Get-FileHash $html).Hash;nativeHash=(Get-FileHash $native).Hash} | ConvertTo-Json
