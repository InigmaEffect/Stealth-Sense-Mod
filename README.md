# StealthSense (Minecraft Java 26.3 — Fabric)

Realistic mob detection mod featuring FOV cones, distance falloff, environmental clarity (light + foliage/tree cover), high-speed blur bypass, intelligence-based ambush searching, and an in-game `#` key tuner menu for every mob.

## Quick Build via GitHub Actions
1. Double-click **`upload-to-github.bat`** inside this folder.
2. If you have GitHub CLI (`gh`) installed and logged in, it creates `https://github.com/InigmaEffect/StealthSense-26.3` automatically and pushes. Otherwise, it opens the GitHub New Repo page pre-filled—create the empty repo and re-run `upload-to-github.bat`.
3. GitHub Actions (`.github/workflows/build.yml`) compiles with Java 25 & Gradle 9.6.0 and publishes `stealthsense-jar` under the **Actions** tab.
