# find_stale_class.ps1
# Usage:
#   .\find_stale_class.ps1 -DomainPath "C:\glassfish\domains\domain1" -Pattern "WishlistMB_NEW"
param(
    [Parameter(Mandatory=$true)]
    [string]$DomainPath,

    [string]$Pattern = "WishlistMB_NEW"
)

Write-Host "Searching for '$Pattern' under $DomainPath ..."
Get-ChildItem -Path $DomainPath -Recurse -Force -ErrorAction SilentlyContinue |
    Select-String -Pattern $Pattern -SimpleMatch -List | ForEach-Object {
        Write-Host $_.Path
    }

Write-Host "Search complete."