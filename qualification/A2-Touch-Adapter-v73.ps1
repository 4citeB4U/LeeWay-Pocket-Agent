$ErrorActionPreference='Stop'
$r='E:\LeeWay-Work\agent-lee-fold6-unified'
$m=Join-Path $r 'app\src\main\AndroidManifest.xml'
$v=Join-Path $r 'app\build.gradle.kts'
$s=Join-Path $r 'app\src\main\res\values\strings.xml'
foreach($p in @($m,$v,$s)){Copy-Item $p ($p+'.a2-touch-before.bak') -Force}
$t=[IO.File]::ReadAllText($m)
$anchor='        <service'+[Environment]::NewLine+'            android:name=".PocketOverlayService"'
if(!$t.Contains($anchor)){throw 'MANIFEST_ANCHOR_CHANGED'}
$entry=@'
        <service
            android:name=".AgentLeeTouchAuthority"
            android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"
            android:exported="true">
            <intent-filter><action android:name="android.accessibilityservice.AccessibilityService" /></intent-filter>
            <meta-data android:name="android.accessibilityservice" android:resource="@xml/agent_lee_touch_authority" />
        </service>
'@
$t=$t.Replace($anchor,$entry+[Environment]::NewLine+$anchor)
[IO.File]::WriteAllText($m,$t,(New-Object Text.UTF8Encoding($false)))
$u=[IO.File]::ReadAllText($s)
if(!$u.Contains('</resources>')){throw 'STRINGS_CHANGED'}
$u=$u.Replace('</resources>','    <string name="agent_lee_touch_accessibility_description">Owner-enabled Agent Lee gestures for approved, verified device actions. Disabled until you enable it in Android Accessibility settings.</string>'+[Environment]::NewLine+'</resources>')
[IO.File]::WriteAllText($s,$u,(New-Object Text.UTF8Encoding($false)))
$b=[IO.File]::ReadAllText($v)
if(!$b.Contains('versionCode = 72')){throw 'VERSION_CONFLICT'}
[IO.File]::WriteAllText($v,$b.Replace('versionCode = 72','versionCode = 73').Replace('1.0.0-cursor-foundation-rc45','1.0.0-touch-actuator-rc46'),(New-Object Text.UTF8Encoding($false)))
