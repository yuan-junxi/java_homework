$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

$files = Get-ChildItem -Recurse src,test -Filter *.java | ForEach-Object { $_.FullName }
if (Test-Path out) {
    Remove-Item -Recurse -Force out
}

javac -encoding UTF-8 -d out $files
jar --create --file mini-nosql-db.jar --main-class minidb.server.MiniDbServer -C out .

Write-Host "Build finished: mini-nosql-db.jar"

