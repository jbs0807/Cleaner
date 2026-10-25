# Cleaner

Android memory and battery usage management app.

## Goals
- Show installed and currently active apps, memory usage and battery usage where Android permits.
- Hide stopped apps from the active-app list.
- Protect essential system, phone, messaging and user-selected apps from suggested actions.
- Provide links to Android's app settings to let the user stop or restrict unnecessary apps.
- Keep the interface simple and minimize background CPU and battery usage.

## Platform limitations
Android does not allow ordinary third-party apps to silently force-stop other apps, clear other apps' memory, or accurately monitor all running processes. Cleaner must use supported APIs and user-approved system settings; no accessibility automation or device-security bypasses.

## Next steps
Implement Android app UI, usage access permission flow, protected-app preferences, and GitHub Actions APK build.
