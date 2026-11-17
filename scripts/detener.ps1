param([Parameter(Mandatory=$true)][string]$Destino)
$ErrorActionPreference = 'Stop'
$raiz = [IO.Path]::GetFullPath($Destino)
$archivoPid = Join-Path $raiz 'app.pid'
if (-not (Test-Path -LiteralPath $archivoPid)) { Write-Output 'La aplicacion no tiene PID registrado.'; exit 0 }
$appPid = [int](Get-Content -LiteralPath $archivoPid -Raw)
$existe = Get-Process -Id $appPid -ErrorAction SilentlyContinue
$proceso = if ($existe) { Get-CimInstance Win32_Process -Filter "ProcessId = $appPid" -ErrorAction Stop } else { $null }
if ($proceso) {
    if ($proceso.Name -ne 'java.exe' -or -not $proceso.CommandLine.Contains($raiz) -or -not $proceso.CommandLine.Contains('catalogo.jar')) { throw 'PID ajeno: no se detiene.' }
    Stop-Process -Id $appPid -Force
}
Remove-Item -LiteralPath $archivoPid -Force
Write-Output 'Aplicacion detenida. Datos conservados.'
