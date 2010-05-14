@echo off
call mvn clean package

call rmdir /s /q "%USERPROFILE%\.IntelliJIdea90-sandbox\config\plugins\SBT"
call "C:\Program Files\7-Zip\7z.exe" x -y -o"%USERPROFILE%\.IntelliJIdea90-sandbox\config\plugins" sbt-dist/target/idea-sbt-plugin-*.zip

set IDEA_PROPERTIES=%~dp0\idea.properties
call "C:\Program Files (x86)\JetBrains\IntelliJ IDEA Community Edition 9.0.2\bin\idea.exe"
