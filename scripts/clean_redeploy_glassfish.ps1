# clean_redeploy_glassfish.ps1
# Edit the variables below for your environment, then run in PowerShell as Administrator if needed.

param(
    [string]$GlassfishHome = "C:\\glassfish",
    [string]$Domain = "domain1",
    [string]$AppName = "EZMart_Supermarket_Management-war",
    [string]$WarFile = "C:\\Users\\bopha\\Downloads\\project4_3\\EZMart_Supermarket_Management-war\\dist\\EZMart_Supermarket_Management-war.war"
)

$Asadmin = Join-Path $GlassfishHome "bin\asadmin.bat"
if (-not (Test-Path $Asadmin)) {
    Write-Error "asadmin not found at $Asadmin. Update \$GlassfishHome to point to your GlassFish installation."
    exit 1
}

Write-Host "Stopping domain $Domain..."
& $Asadmin stop-domain $Domain

Write-Host "Removing application folder(s) and caches..."
$domainPath = Join-Path $GlassfishHome "glassfish\domains\$Domain"
$appFolder = Join-Path $domainPath "applications\$AppName"
$generated = Join-Path $domainPath "generated"
$osgiCache = Join-Path $domainPath "osgi-cache"

# Remove if exists
if (Test-Path $appFolder) { Remove-Item -Recurse -Force $appFolder -ErrorAction SilentlyContinue; Write-Host "Deleted $appFolder" }
if (Test-Path $generated) { Remove-Item -Recurse -Force $generated -ErrorAction SilentlyContinue; Write-Host "Deleted $generated" }
if (Test-Path $osgiCache) { Remove-Item -Recurse -Force $osgiCache -ErrorAction SilentlyContinue; Write-Host "Deleted $osgiCache" }

Write-Host "Starting domain $Domain..."
& $Asadmin start-domain $Domain

Write-Host "Undeploying app (if still registered): $AppName"
& $Asadmin undeploy $AppName || Write-Host "Undeploy returned non-zero; continuing."

Write-Host "Deploying WAR: $WarFile"
if (-not (Test-Path $WarFile)) { Write-Error "WAR file not found at $WarFile"; exit 1 }
& $Asadmin deploy --contextroot /EZMart_Supermarket_Management-war --name $AppName $WarFile

Write-Host "Done. Check server logs for any remaining errors."