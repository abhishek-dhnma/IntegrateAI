# Loads .env into process environment and starts the Spring Boot application
Write-Host "Loading environment variables from .env..." -ForegroundColor Cyan

Get-Content .env | Where-Object { $_ -match '^([^#=]+)=(.*)$' } | ForEach-Object {
    $name = $matches[1].Trim()
    $value = $matches[2].Trim()
    [System.Environment]::SetEnvironmentVariable($name, $value, 'Process')
}

Write-Host "Starting IntegrateAI with Nango & OpenRouter..." -ForegroundColor Green
& "C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2025.1.1.1\plugins\maven\lib\maven3\bin\mvn.cmd" spring-boot:run
