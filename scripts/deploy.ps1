param(
    [Parameter(Mandatory=$true)][string]$Jar,
    [Parameter(Mandatory=$true)][string]$Destino,
    [Parameter(Mandatory=$true)][ValidatePattern('^[a-zA-Z0-9._-]+$')][string]$Revision,
    [ValidateRange(1024,65535)][int]$Puerto = 8081
)
$ErrorActionPreference = 'Stop'
$jarFuente = (Resolve-Path -LiteralPath $Jar).Path
$raiz = [IO.Path]::GetFullPath($Destino)
New-Item -ItemType Directory -Force -Path $raiz | Out-Null
$bloqueo = $null
try {
    $bloqueo = [IO.File]::Open((Join-Path $raiz 'deploy.lock'), 'OpenOrCreate', 'ReadWrite', 'None')
} catch { throw 'Otro despliegue utiliza esta carpeta. Esperar y reintentar.' }
try {
    $actual = Join-Path $raiz 'actual.json'
    $anterior = if (Test-Path -LiteralPath $actual) { Get-Content -LiteralPath $actual -Raw | ConvertFrom-Json } else { $null }
    $idVersion = (Get-Date -Format 'yyyyMMddHHmmssfff') + '-' + $Revision
    $version = Join-Path $raiz ('versiones\' + $idVersion)
    New-Item -ItemType Directory -Force -Path $version | Out-Null
    $jarDestino = Join-Path $version 'catalogo.jar'
    Copy-Item -LiteralPath $jarFuente -Destination $jarDestino
    $hash = (Get-FileHash -LiteralPath $jarDestino -Algorithm SHA256).Hash
    if ($hash -ne (Get-FileHash -LiteralPath $jarFuente -Algorithm SHA256).Hash) { throw 'El JAR copiado no coincide con el original.' }
    $datos = Join-Path $raiz 'datos\catalogo.json'
    $archivoPid = Join-Path $raiz 'app.pid'
    $java = (Get-Command java.exe -ErrorAction Stop).Source

    function Detener-Propio {
        if (Test-Path -LiteralPath $archivoPid) {
            $appPid = [int](Get-Content -LiteralPath $archivoPid -Raw)
            $existe = Get-Process -Id $appPid -ErrorAction SilentlyContinue
            $proceso = if ($existe) { Get-CimInstance Win32_Process -Filter "ProcessId = $appPid" -ErrorAction Stop } else { $null }
            if ($proceso) {
                if ($proceso.Name -ne 'java.exe' -or -not $proceso.CommandLine.Contains($raiz) -or -not $proceso.CommandLine.Contains('catalogo.jar')) {
                    throw 'El PID no pertenece a esta aplicacion. Se cancela sin detenerlo.'
                }
                Stop-Process -Id $appPid -Force
                Wait-Process -Id $appPid -Timeout 15 -ErrorAction SilentlyContinue
            }
            Remove-Item -LiteralPath $archivoPid -Force
        }
    }
    function Iniciar-Version([string]$rutaJar, [string]$rev, [string]$dirLog) {
        $env:JENKINS_NODE_COOKIE = 'dontKillMe'
        $argumentos = @('-jar', ('"' + $rutaJar + '"'), ('--server.port=' + $Puerto), ('"--catalogo.archivo=' + $datos + '"'), ('--catalogo.revision=' + $rev))
        $iniciado = Start-Process -FilePath $java -ArgumentList $argumentos -WorkingDirectory $raiz -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $dirLog 'stdout.log') -RedirectStandardError (Join-Path $dirLog 'stderr.log')
        Set-Content -LiteralPath $archivoPid -Value $iniciado.Id -Encoding ASCII
        $limite = (Get-Date).AddSeconds(60)
        do {
            Start-Sleep -Milliseconds 500
            $iniciado.Refresh()
            if ($iniciado.HasExited) { throw ('La aplicacion termino antes del smoke test. Ver ' + $dirLog) }
            try {
                $estado = Invoke-RestMethod -Uri "http://localhost:$Puerto/api/estado" -TimeoutSec 2
                if ($estado.estado -eq 'OK' -and $estado.revision -eq $rev) {
                    $portada = Invoke-WebRequest -Uri "http://localhost:$Puerto/" -UseBasicParsing -TimeoutSec 3
                    $catalogo = Invoke-WebRequest -Uri "http://localhost:$Puerto/api/videojuegos" -UseBasicParsing -TimeoutSec 3
                    if ($portada.StatusCode -eq 200 -and $catalogo.StatusCode -eq 200) { return }
                }
            } catch { }
        } while ((Get-Date) -lt $limite)
        throw 'Smoke test sin exito en 60 segundos.'
    }
    Detener-Propio
    try {
        Iniciar-Version $jarDestino $Revision $version
        $metadata = @{ revision=$Revision; jar=$jarDestino; sha256=$hash; puerto=$Puerto; fecha=(Get-Date).ToString('o') }
        $temporal = Join-Path $raiz 'actual.tmp'
        $metadata | ConvertTo-Json | Set-Content -LiteralPath $temporal -Encoding UTF8
        Move-Item -LiteralPath $temporal -Destination $actual -Force
        Write-Output "DESPLIEGUE OK revision=$Revision sha256=$hash puerto=$Puerto"
    } catch {
        $fallo = $_
        Detener-Propio
        if ($anterior) {
            $logRollback = Join-Path $version 'rollback'
            New-Item -ItemType Directory -Force -Path $logRollback | Out-Null
            Iniciar-Version $anterior.jar $anterior.revision $logRollback
            Write-Output ('ROLLBACK OK revision=' + $anterior.revision)
        }
        throw $fallo
    }
} finally {
    if ($bloqueo) { $bloqueo.Dispose() }
}
