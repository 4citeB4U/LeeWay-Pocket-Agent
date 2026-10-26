$ErrorActionPreference='Stop'
$r='E:\LeeWay-Work\agent-lee-fold6-unified'
$m=Join-Path $r 'app\src\main\AndroidManifest.xml'
$v=Join-Path $r 'app\build.gradle.kts'
$s=Join-Path $r 'app\src\main\res\values\strings.xml'
Copy-Item $m ($m+'.a2-touch-gate-before.bak') -Force
Copy-Item $v ($v+'.a2-touch-gate-before.bak') -Force
$t=[IO.File]::ReadAllText($m)
if($t.Contains('AgentLeeTouchAuthority')){throw 'ALREADY_REGISTERED'}
$entry=@'
        <service
            android:name=".AgentLeeTouchAuthority"
            android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"
            android:exported="true">
            <intent-filter>
                <action android:name="android.accessibilityservice.AccessibilityService"/>
            </intent-filter>
            <meta-data android:name="android.accessibilityservice" android:resource="@xml/agent_lee_touch_authority"/>
        </service>
'@
$t=[regex]::Replace($t,'(?m)^([ \t]*<service\s*\r?\n[ \t]*android:name="\.PocketOverlayService")',$entry+"`n"+'$1',1)
if(!$t.Contains('android:name=".AgentLeeTouchAuthority"')){throw 'MANIFEST_PATCH_FAILED'}
[IO.File]::WriteAllText($m,$t,(New-Object Text.UTF8Encoding($false)))
$u=[IO.File]::ReadAllText($s)
$u=$u.Replace('</resources>','    <string name="agent_lee_touch_accessibility_description">Owner-enabled Agent Lee gestures for approved device actions. Enable explicitly in Android Accessibility settings.</string>'+[Environment]::NewLine+'</resources>')
[IO.File]::WriteAllText($s,$u,(New-Object Text.UTF8Encoding($false)))
$b=[IO.File]::ReadAllText($v)
if(!$b.Contains('versionCode = 72')){throw 'VERSION_CONFLICT'}
[IO.File]::WriteAllText($v,$b.Replace('versionCode = 72','versionCode = 73').Replace('1.0.0-cursor-foundation-rc45','1.0.0-touch-actuator-rc46'),(New-Object Text.UTF8Encoding($false)))
Write-Output 'A2_ADAPTER_REGISTERED'
