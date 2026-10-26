$ErrorActionPreference='Stop'
$r='E:\LeeWay-Work\agent-lee-fold6-unified'
$p=Join-Path $r 'app\src\main\assets\agent_lee_sphere_transparent.html'
$v=Join-Path $r 'app\build.gradle.kts'
Copy-Item $p ($p+'.a2-hamburger-reference-before.bak') -Force
$s=[IO.File]::ReadAllText($p)
if($s.Contains('LEEWAY_A2_REFERENCE_HAMBURGER_V1')){throw 'ALREADY_STAGED'}
$css=@'
<style id="LEEWAY_A2_REFERENCE_HAMBURGER_V1">
/* Owner approved navigation design: existing ten destinations, same handlers. */
.drawer{width:min(580px,98vw)!important;max-width:98vw!important;padding:20px 16px 26px!important;background:radial-gradient(ellipse at 94% 3%,#1558a03b,transparent 53%),linear-gradient(155deg,#020a1d,#041c39 75%,#020b1e)!important;border:1px solid #2bbfff!important;border-radius:18px!important;box-shadow:inset 0 0 35px #0089ee2b,0 0 20px #007dd955!important}
.drawer-header{align-items:start!important;gap:13px!important;padding:12px 4px 20px!important;min-height:108px}
.drawer-title{font-size:clamp(28px,6vw,48px)!important;line-height:1.1!important;font-weight:850!important;text-shadow:0 0 12px #248fed}
.drawer-subtitle{font-size:clamp(14px,2.6vw,19px)!important;color:#a0bde9!important;line-height:1.4!important;margin-top:9px}
#close-drawer,.close-btn{background:#b91c1c!important;border:2px solid #ff8f96!important;border-radius:14px!important;color:white!important;min-width:66px!important;min-height:66px!important;font-size:36px!important;box-shadow:0 0 17px #ea323b80!important;flex-shrink:0}
.settings-list{gap:13px!important}
.element-card.setting-card{display:grid!important;grid-template-columns:62px minmax(0,1fr) auto!important;align-items:center!important;gap:14px!important;width:100%!important;min-height:116px!important;padding:16px!important;background:linear-gradient(135deg,#092749f2,#082142f5)!important;border:1.5px solid #36bfff!important;border-radius:20px!important;box-shadow:inset 0 0 14px #078ada22,0 0 12px #009df630!important}
.element-card.setting-card::before{content:"◈";display:grid;place-items:center;width:62px;height:68px;border:1.5px solid #40c5ff;border-radius:14px;background:#0a3156;color:#4dd9ff;font-size:36px;box-shadow:0 0 15px #07a7f750}
.element-card[data-setting="digital-brain"]::before{content:"◎"}
.element-card[data-setting="continuum"]::before{content:"▤"}
.element-card[data-setting="diagnostics"]::before{content:"⚙"}
.element-card[data-setting="voice"]::before{content:"♩"}
.element-card[data-setting="workstation"]::before{content:"⬡"}
.element-card[data-setting="models"]::before{content:"◇"}
.element-card[data-setting="skills"]::before{content:"✦"}
.element-card[data-setting="authority"]::before{content:"⬡"}
.element-card[data-setting="evidence"]::before{content:"▱"}
.element-card[data-setting="about"]::before{content:"ℹ"}
.element-card .element-name{font-size:clamp(18px,3.5vw,28px)!important;font-weight:750!important;line-height:1.22!important}
.element-card .element-motto{font-size:clamp(13px,2.65vw,18px)!important;line-height:1.4!important;color:#adc9eb!important}
.element-card .element-action{color:#4fd5ff!important;font-size:clamp(15px,2.6vw,21px)!important;font-weight:800!important}
.element-card .element-action::after{content:" ❯";font-size:1.25em}
.element-card:focus-visible{outline:3px solid #fff;outline-offset:2px}
@media(max-width:400px){.drawer{padding:10px!important}.element-card.setting-card{min-height:93px!important;grid-template-columns:48px minmax(0,1fr) auto!important;padding:10px!important;gap:10px!important}.element-card.setting-card::before{width:46px;height:52px;font-size:25px}.drawer-header{min-height:85px}#close-drawer{min-width:54px!important;min-height:54px!important;font-size:28px!important}}
</style>
'@
if(!$s.Contains('</head>')){throw 'HTML_HEAD_MISSING'}
$s=$s.Replace('</head>',$css+[Environment]::NewLine+'</head>')
[IO.File]::WriteAllText($p,$s,(New-Object System.Text.UTF8Encoding($false)))
$a=[IO.File]::ReadAllText($v)
if(!$a.Contains('versionCode = 66')){throw 'VERSION_CONFLICT'}
Copy-Item $v ($v+'.a2-hamburger-reference-before.bak') -Force
[IO.File]::WriteAllText($v,$a.Replace('versionCode = 66','versionCode = 67').Replace('1.0.0-roundbox-drag-scope-rc39','1.0.0-hamburger-reference-rc40'),(New-Object System.Text.UTF8Encoding($false)))
Get-FileHash $p,$v -Algorithm SHA256|ForEach-Object {$_.Path+' '+$_.Hash}
