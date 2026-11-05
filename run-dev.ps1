#
# - Project   : iPOS - Point of Sale, Stock and Back-Office
# - Package   : project
# - File      : run-dev.ps1
# - Date      : 2026. 11. 05.
# - User      : Heritier NTAGANIRA
# - Desc      : Start local development with a prompted OpenAI API key.
#

param(
    [string]$Profile = "dev"
)

$ErrorActionPreference = "Stop"
Set-Location -LiteralPath $PSScriptRoot

if ([string]::IsNullOrWhiteSpace($env:OPENAI_API_KEY) -or $env:OPENAI_API_KEY -eq "not-configured") {
    $secureKey = Read-Host -Prompt "OpenAI API key" -AsSecureString
    $bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secureKey)
    try {
        $env:OPENAI_API_KEY = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr)
    }
    finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)
    }
}

if ([string]::IsNullOrWhiteSpace($env:OPENAI_API_KEY)) {
    throw "An OpenAI API key is required."
}

$env:IPOS_PROFILE = $Profile

& "$PSScriptRoot\mvnw.cmd" spring-boot:run
exit $LASTEXITCODE
