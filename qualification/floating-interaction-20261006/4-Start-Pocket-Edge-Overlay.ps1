<# REGION: LEEWAY.DEVICES.WINDOWS.UI; TAG: ORIGINAL_EMBLEM_FLOATING_TAB
WHO: Owner-enabled Agent Lee. WHAT: Maintain the existing floating logo tab and idle elemental colors.
WHEN: Existing boot/launcher. WHERE: Windows native display adapter.
WHY: Same original artwork, no alternate logo or identity. HOW: In-memory color matrix, existing launcher.
LICENSE: MIT #>
param([string]$EvidenceDirectory)
$ErrorActionPreference='Stop'
Add-Type -AssemblyName PresentationFramework,PresentationCore,WindowsBase,System.Drawing
$created=$false;$mutex=New-Object Threading.Mutex($true,'Global\LeeWayPocketEdgeOverlay',[ref]$created)
if(!$created){$mutex.Dispose();exit 0}
$root=Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$launcher=Join-Path $root 'Start-Agent-Lee.ps1';$logo=Join-Path $PSScriptRoot 'agent-lee-tag.png'
if(!(Test-Path $launcher) -or !(Test-Path $logo)){throw 'INSTALLED_AGENT_ARTWORK_OR_LAUNCHER_UNBOUND'}
$original=[Drawing.Bitmap]::FromFile($logo)
$frames=@()
try {
 foreach($angle in @(0,105,210,280)){
  $c=[single][Math]::Cos($angle*[Math]::PI/180);$s=[single][Math]::Sin($angle*[Math]::PI/180)
  $matrix=New-Object Drawing.Imaging.ColorMatrix
  $matrix.Matrix00=.213+.787*$c-.213*$s;$matrix.Matrix10=.715-.715*$c-.715*$s;$matrix.Matrix20=.072-.072*$c+.928*$s
  $matrix.Matrix01=.213-.213*$c+.143*$s;$matrix.Matrix11=.715+.285*$c+.140*$s;$matrix.Matrix21=.072-.072*$c-.283*$s
  $matrix.Matrix02=.213-.213*$c-.787*$s;$matrix.Matrix12=.715-.715*$c+.715*$s;$matrix.Matrix22=.072+.928*$c+.072*$s
  $image=New-Object Drawing.Bitmap(68,68);$graphics=[Drawing.Graphics]::FromImage($image);$attributes=New-Object Drawing.Imaging.ImageAttributes
  try{
   $attributes.SetColorMatrix($matrix);$graphics.Clear([Drawing.Color]::Transparent);$graphics.InterpolationMode=[Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
   $graphics.DrawImage($original,(New-Object Drawing.Rectangle(0,0,68,68)),0,0,$original.Width,$original.Height,[Drawing.GraphicsUnit]::Pixel,$attributes)
   $stream=New-Object IO.MemoryStream;$image.Save($stream,[Drawing.Imaging.ImageFormat]::Png);$stream.Position=0
   $bitmap=New-Object Windows.Media.Imaging.BitmapImage;$bitmap.BeginInit();$bitmap.CacheOption=[Windows.Media.Imaging.BitmapCacheOption]::OnLoad;$bitmap.StreamSource=$stream;$bitmap.EndInit();$bitmap.Freeze();$frames+=,$bitmap;$stream.Dispose()
  }finally{$graphics.Dispose();$attributes.Dispose();$image.Dispose()}
 }
}finally{$original.Dispose()}
$window=New-Object Windows.Window
$window.Width=68;$window.Height=68;$window.Title='Agent Lee Floating Emblem';$window.WindowStyle='None';$window.ResizeMode='NoResize';$window.Topmost=$true;$window.ShowInTaskbar=$false;$window.AllowsTransparency=$true;$window.Background=[Windows.Media.Brushes]::Transparent
$area=[Windows.SystemParameters]::WorkArea;$window.Left=$area.Right-$window.Width
$saved=Join-Path $root 'edge-y.txt';$y=if(Test-Path $saved){[double](Get-Content $saved -Raw)}else{$area.Top+($area.Height-$window.Height)/2};$window.Top=[Math]::Max($area.Top,[Math]::Min($area.Bottom-$window.Height,$y))
$imageView=New-Object Windows.Controls.Image;$imageView.Source=$frames[0];$imageView.Stretch='Uniform';$imageView.ToolTip='Open Agent Lee';$window.Content=$imageView
$script:startY=0.0;$script:startTop=0.0;$script:moved=$false;$script:frame=0
$imageView.Add_MouseLeftButtonDown({$script:startY=[Windows.Input.Mouse]::GetPosition($null).Y;$script:startTop=$window.Top;$script:moved=$false;$imageView.CaptureMouse()})
$imageView.Add_MouseMove({if($imageView.IsMouseCaptured){$delta=[Windows.Input.Mouse]::GetPosition($null).Y-$script:startY;if([Math]::Abs($delta)-gt 5){$script:moved=$true};$window.Top=[Math]::Max($area.Top,[Math]::Min($area.Bottom-$window.Height,$script:startTop+$delta))}})
$imageView.Add_MouseLeftButtonUp({$imageView.ReleaseMouseCapture();Set-Content $saved ([string]$window.Top);if(!$script:moved){Start-Process powershell.exe -ArgumentList @('-NoProfile','-ExecutionPolicy','Bypass','-WindowStyle','Hidden','-File',('"'+$launcher+'"')) -WindowStyle Hidden}})
if($EvidenceDirectory){[void][IO.Directory]::CreateDirectory($EvidenceDirectory)}
$timer=New-Object Windows.Threading.DispatcherTimer;$timer.Interval=[TimeSpan]::FromSeconds(2)
$timer.Add_Tick({
 $script:frame=($script:frame+1)%$frames.Count;$imageView.Source=$frames[$script:frame]
 if($EvidenceDirectory){
  $target=Join-Path $EvidenceDirectory ('emblem-frame-'+$script:frame+'.png')
  if(!(Test-Path $target)){$encoder=New-Object Windows.Media.Imaging.PngBitmapEncoder;$encoder.Frames.Add([Windows.Media.Imaging.BitmapFrame]::Create($frames[$script:frame]));$file=[IO.File]::Create($target);try{$encoder.Save($file)}finally{$file.Dispose()}}
  [ordered]@{state='NATIVE_WPF_TAB_RUNNING';pid=$PID;left=$window.Left;top=$window.Top;width=$window.Width;height=$window.Height;visible=$window.IsVisible;frame=$script:frame;artworkSha256=(Get-FileHash $logo -Algorithm SHA256).Hash;animation='IDLE_ARTWORK_COLOR_ONLY_NOT_SPEECH_OR_HEALTH';capturedAt=[DateTime]::UtcNow.ToString('o')}|ConvertTo-Json|Set-Content (Join-Path $EvidenceDirectory 'overlay-live.json') -Encoding UTF8
 }
})
$window.Add_Closed({$timer.Stop()});$timer.Start()
try{[void]$window.ShowDialog()}finally{$timer.Stop();$mutex.ReleaseMutex();$mutex.Dispose()}
