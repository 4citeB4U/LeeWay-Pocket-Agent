param([string]$Expression='JSON.stringify({title:document.title,iframe:[...document.querySelectorAll("iframe")].map(x=>({src:x.src,w:x.clientWidth,h:x.clientHeight})),visionMenu:!!document.querySelector("[data-setting=vision]"),panelOpen:document.getElementById("surface-panel")?.classList.contains("open")})')
$ErrorActionPreference='Stop'
$targets=Invoke-RestMethod 'http://127.0.0.1:9333/json'
$target=$targets | Where-Object { $_.url -match 'agent_lee_sphere_transparent.html' } | Select-Object -First 1
if(-not $target){throw 'WEBVIEW_NOT_FOUND'}
$ws=[System.Net.WebSockets.ClientWebSocket]::new()
$uri=[Uri]$target.webSocketDebuggerUrl
$ws.ConnectAsync($uri,[Threading.CancellationToken]::None).GetAwaiter().GetResult()
try{
 $payload=@{id=22;method='Runtime.evaluate';params=@{expression=$Expression;returnByValue=$true;awaitPromise=$true}}|ConvertTo-Json -Depth 8 -Compress
 $bytes=[Text.Encoding]::UTF8.GetBytes($payload)
 $ws.SendAsync([ArraySegment[byte]]$bytes,[System.Net.WebSockets.WebSocketMessageType]::Text,$true,[Threading.CancellationToken]::None).GetAwaiter().GetResult()
 $buffer=New-Object byte[] 65536
 for($i=0;$i -lt 100;$i++){
  $result=$ws.ReceiveAsync([ArraySegment[byte]]$buffer,[Threading.CancellationToken]::None).GetAwaiter().GetResult()
  $text=[Text.Encoding]::UTF8.GetString($buffer,0,$result.Count)
  $message=$text|ConvertFrom-Json
  if($message.id -eq 22){$message|ConvertTo-Json -Depth 14;break}
 }
}finally{$ws.Dispose()}
