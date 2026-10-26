param([string]$Method='Page.getFrameTree',[string]$ParamsJson='{}',[int]$Port=9333,[string]$TargetPattern='agent_lee_sphere_transparent.html')
$ErrorActionPreference='Stop'
$targets=Invoke-RestMethod "http://127.0.0.1:$Port/json"
$target=$targets|Where-Object {$_.url -match $TargetPattern}|Select-Object -First 1
if(-not $target){throw 'GOLDEN_WEBVIEW_NOT_FOUND'}
$ws=[Net.WebSockets.ClientWebSocket]::new()
$ws.ConnectAsync([Uri]$target.webSocketDebuggerUrl,[Threading.CancellationToken]::None).GetAwaiter().GetResult()|Out-Null
try {
  $payload=@{id=77;method=$Method;params=($ParamsJson|ConvertFrom-Json)}|ConvertTo-Json -Depth 12 -Compress
  $bytes=[Text.Encoding]::UTF8.GetBytes($payload)
  $ws.SendAsync([ArraySegment[byte]]$bytes,[Net.WebSockets.WebSocketMessageType]::Text,$true,[Threading.CancellationToken]::None).GetAwaiter().GetResult()|Out-Null
  $buf=New-Object byte[] 131072
  $sb=[Text.StringBuilder]::new()
  for($i=0;$i -lt 100;$i++){
    $result=$ws.ReceiveAsync([ArraySegment[byte]]$buf,[Threading.CancellationToken]::None).GetAwaiter().GetResult()
    [void]$sb.Append([Text.Encoding]::UTF8.GetString($buf,0,$result.Count))
    if(-not $result.EndOfMessage){continue}
    $msg=$sb.ToString()|ConvertFrom-Json
    $sb.Clear()|Out-Null
    if($msg.id -eq 77){$msg|ConvertTo-Json -Depth 20;break}
  }
}finally{$ws.Dispose()}
