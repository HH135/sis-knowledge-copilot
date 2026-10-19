$env:SPRING_PROFILES_ACTIVE = "portfolio"
Remove-Item Env:APP_AI_EMBEDDING_PROVIDER, Env:APP_AI_EXTERNAL_ENABLED -ErrorAction SilentlyContinue
.\mvnw.cmd spring-boot:run