@echo off
rmdir /s /q .git 2>nul & git init -b main && git add -A && git commit -m "Fresh upload" && git remote add origin https://github.com/InigmaEffect/Stealth-Sense-Mod.git && git config http.postBuffer 524288000 && git push -u origin main --force
pause
