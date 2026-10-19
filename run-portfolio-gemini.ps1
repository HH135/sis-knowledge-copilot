$env:SPRING_PROFILES_ACTIVE = "portfolio"
$env:APP_AI_EMBEDDING_PROVIDER = "gemini"
$env:APP_AI_EXTERNAL_ENABLED = "true"
if (-not $env:GEMINI_API_KEY) { $env:GEMINI_API_KEY = (Get-ItemProperty HKCU:\Environment).GEMINI_API_KEY }
if (-not $env:GEMINI_API_KEY) { Write-Error "GEMINI_API_KEY not set"; exit 1 }
.\mvnw.cmd spring-boot:run