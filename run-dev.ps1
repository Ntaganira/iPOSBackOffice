#
# - Project   : iPOS - Point of Sale, Stock and Back-Office
# - Package   : project
# - File      : run-dev.ps1
# - Date      : 2026. 11. 05.
# - User      : Heritier NTAGANIRA
# - Desc      : Start local development with Ollama product extraction.
#

param(
    [string]$Profile = "dev"
)

$ErrorActionPreference = "Stop"
Set-Location -LiteralPath $PSScriptRoot

$env:IPOS_PROFILE = $Profile

if (-not (Get-Command ollama -ErrorAction SilentlyContinue)) {
    Write-Warning "Ollama was not found on PATH. Product image extraction will be unavailable."
}

& "$PSScriptRoot\mvnw.cmd" spring-boot:run
exit $LASTEXITCODE
