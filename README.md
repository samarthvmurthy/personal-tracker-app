# Fit40 Tracker Android App

Standalone Android app source for your 40 day workout and diet tracker.

## What it does
- Stores all app data in one persistent local file: `fit40_data.json`
- Preloads day 1 to day 40 with your Push Pull Legs rotation
- Tracks workout completion
- Tracks meal completion for each day
- Lets you log bodyweight and notes
- Shows progress charts for workouts, meals, and weight trend
- Supports JSON export and import backups
- Works fully offline

## Tech stack
- Kotlin
- Jetpack Compose
- One JSON file stored in app internal storage

## Project structure
- `app/src/main/java/com/fit40/app/MainActivity.kt` contains the full app logic and UI
- `app/src/main/AndroidManifest.xml`
- Gradle Kotlin DSL build files

## Data model
The app creates and maintains one file in internal app storage:
- `fit40_data.json`

This is persistent across app restarts.

## Build locally
Open the folder in Android Studio and build the app.

## Build with an online APK builder
Upload this project to a service that supports standard Android Gradle projects.
Typical steps:
1. Upload the full project folder or a Git repository containing it.
2. Use the default Gradle Android build.
3. Build debug APK first.
4. Download the generated APK and install it on your phone.

## Minimum Android version
- Android 8.0+

## Notes
- The app starts the 40 day plan from the first launch date.
- Today is calculated from that start date.
- Progress charts update automatically as you check meals and workouts.
