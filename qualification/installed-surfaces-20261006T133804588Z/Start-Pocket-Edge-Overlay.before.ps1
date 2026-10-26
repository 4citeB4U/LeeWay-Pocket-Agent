param([switch]$Stop)
Add-Type -AssemblyName PresentationFramework,PresentationCore,WindowsBase
$created=$false
$mutex=New-Object Threading.Mutex($true,'Global\LeeWayPocketEdgeOverlay',[ref]$created)
if(!$created){exit 0}
$root='E:\LeeWay-Work\pc-consciousness-live'
$profile=Join-Path $root 'pocket-chrome-profile-v2'
$chrome='C:\Program Files\Google\Chrome\Application\chrome.exe'
$w=New-Object Windows.Window
$w.Width=64;$w.Height=64;$w.WindowStyle='None';$w.ResizeMode='NoResize';$w.Topmost=$true;$w.ShowInTaskbar=$false;$w.AllowsTransparency=$true;$w.Background=[Windows.Media.Brushes]::Transparent
$wa=[Windows.SystemParameters]::WorkArea;$w.Left=$wa.Right-$w.Width;$saved=Join-Path $root 'edge-y.txt';$y=if(Test-Path $saved){[double](Get-Content $saved -Raw)}else{$wa.Top+($wa.Height-$w.Height)/2};$w.Top=[Math]::Max($wa.Top,[Math]::Min($wa.Bottom-$w.Height,$y))
$b=New-Object Windows.Controls.Border;$b.Background=[Windows.Media.Brushes]::Transparent;$b.BorderThickness=New-Object Windows.Thickness(0)
$logo=Join-Path $root 'runtime\\machine-consciousness\\agent-lee-tag.png'
if(!(Test-Path $logo)){$logo=Join-Path $PSScriptRoot 'agent-lee-tag.png'}
$img=New-Object Windows.Controls.Image
$bmp=New-Object Windows.Media.Imaging.BitmapImage
$bmp.BeginInit();$bmp.UriSource=New-Object Uri($logo);$bmp.CacheOption=[Windows.Media.Imaging.BitmapCacheOption]::OnLoad;$bmp.EndInit()
$img.Source=$bmp;$img.Stretch='UniformToFill';$img.Margin=New-Object Windows.Thickness(0);$b.Child=$img;$w.Content=$b
$script:startY=0.0;$script:startTop=0.0;$script:moved=$false
$b.Add_MouseLeftButtonDown({$script:startY=[Windows.Input.Mouse]::GetPosition($null).Y;$script:startTop=$w.Top;$script:moved=$false;$b.CaptureMouse()})
$b.Add_MouseMove({if($b.IsMouseCaptured){$now=[Windows.Input.Mouse]::GetPosition($null).Y;$d=$now-$script:startY;if([Math]::Abs($d)-gt 5){$script:moved=$true};$w.Top=[Math]::Max($wa.Top,[Math]::Min($wa.Bottom-$w.Height,$script:startTop+$d))}})
$b.Add_MouseLeftButtonUp({$b.ReleaseMouseCapture();Set-Content $saved ([string]$w.Top);if(!$script:moved){New-Item -ItemType Directory $profile -Force|Out-Null;Start-Process $chrome -ArgumentList @('--user-data-dir='+$profile,'--app=http://127.0.0.1:8890/ui','--window-size=520,640','--window-position=940,170','--no-first-run')}})
$w.ShowDialog()|Out-Null
