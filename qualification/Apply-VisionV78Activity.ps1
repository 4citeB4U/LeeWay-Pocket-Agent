$ErrorActionPreference='Stop'
$r='E:\LeeWay-Work\agent-lee-fold6-unified'
$dir='E:\LeeWay-Work\c3-vision-lane-a-qualification\qualification'
$a=Join-Path $r 'app\src\main\assets\agent_lee_sphere_transparent.html'
$n=Join-Path $r 'app\src\main\java\industries\leeway\pocket\FloatingSphereWindow.kt'
$m=Join-Path $r 'app\src\main\AndroidManifest.xml'
$g=Join-Path $r 'app\build.gradle.kts'
$h=@{$a='8B142F5B80D2FF823414F75B75B159EA843A38F7C71309E9266272E0D825B8BC';$n='4E0D5ACF0164E16A205FD64ED22F9265BBA95B8BAEAD1D9DEE6C8CC096036E39';$m='C40B7427D369E1A459EAC13ABAC0AF58AEEB030A728C25EF235112EAFE224421';$g='245AAC494305AC8EAA9E0AA7CFBB90AE32DED00064B4B62B34C906BDCBD7C9A0'}
foreach($p in $h.Keys){if((Get-FileHash $p).Hash -ne $h[$p]){throw ('SOURCE_DRIFT '+$p)}}
$backup=Join-Path $dir ('v77-scoped-backup-'+[DateTime]::UtcNow.ToString('yyyyMMddTHHmmssZ'))
New-Item -ItemType Directory $backup | Out-Null
foreach($p in $h.Keys){Copy-Item $p (Join-Path $backup (Split-Path $p -Leaf))}
$html=[IO.File]::ReadAllText($a);$native=[IO.File]::ReadAllText($n);$manifest=[IO.File]::ReadAllText($m);$gradle=[IO.File]::ReadAllText($g)
$old="skills:'openSkills'};"
if(-not $html.Contains($old)){throw 'MENU_HANDLER_DRIFT'}
$html=$html.Replace($old,"skills:'openSkills',vision:'openVision'};")
$old='        @JavascriptInterface fun openVoiceStudio(){main.post{service.startActivity(Intent(service,VoiceStudioActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))}}'
if(-not $native.Contains($old)){throw 'BRIDGE_DRIFT'}
$extra='        @JavascriptInterface fun openVision(){main.post{service.startActivity(Intent(service,VisionActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))}}'
$native=$native.Replace($old,$old+[Environment]::NewLine+$extra)
$old='<activity android:name=".SkillsActivity" android:exported="false" android:theme="@style/AppTheme.TransparentVoice" />'
if(-not $manifest.Contains($old)){throw 'MANIFEST_DRIFT'}
$manifest=$manifest.Replace($old,$old+[Environment]::NewLine+'        <activity android:name=".VisionActivity" android:exported="false" android:theme="@style/AppTheme.TransparentVoice" />')
if(-not $gradle.Contains('versionCode = 78')){throw 'VERSION_DRIFT'}
$gradle=$gradle.Replace('versionCode = 78','versionCode = 79').Replace('versionName = "1.0.0-voice-in-package-rc51"','versionName = "1.0.0-voice-vision-foreground-rc52"')
[IO.File]::WriteAllText($a,$html,[Text.UTF8Encoding]::new($false))
[IO.File]::WriteAllText($n,$native,[Text.UTF8Encoding]::new($false))
[IO.File]::WriteAllText($m,$manifest,[Text.UTF8Encoding]::new($false))
[IO.File]::WriteAllText($g,$gradle,[Text.UTF8Encoding]::new($false))
[pscustomobject]@{action='VISION_NATIVE_ACTIVITY_STAGED';backup=$backup;versionCode=79;visionActivityHash=(Get-FileHash (Join-Path $r 'app\src\main\java\industries\leeway\pocket\VisionActivity.kt')).Hash;htmlHash=(Get-FileHash $a).Hash;manifestHash=(Get-FileHash $m).Hash}|ConvertTo-Json
