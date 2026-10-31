# Setting Up Whisk for Development

This guide covers everything you need to compile, run, and start developing Whisk.

## Prerequisites

1. **Android Studio**: Download the latest **Android Studio Ladybug** (or newer).
2. **Java Development Kit (JDK)**: JDK 17 is required.
3. **Android SDK**: Ensure SDK version 37 is installed via the SDK Manager.
4. **Git**: To clone the repository.

## Cloning the Repository

```bash
git clone https://github.com/whisk/whisk.git
cd whisk
```

## Opening the Project
1. Open Android Studio.
2. Select **File > Open...** and navigate to the cloned `whisk` directory.
3. Allow Gradle to sync. This may take a few minutes as dependencies are downloaded.

## Building and Running

You can build and run the app directly from Android Studio by clicking the **Run** button (Shift + F10) with an emulator or physical device connected.

Alternatively, use the command line:

```bash
# Build the debug APK
./gradlew :app:assembleDebug

# Install on a connected device
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Launch the app
adb shell am start -n com.whisk/com.whisk.MainActivity
```

## Running Tests

Whisk maintains a suite of unit tests. Run them locally to ensure your changes haven't broken existing functionality:

```bash
./gradlew :app:testDebugUnitTest
```

## Local AI Inference Setup (LiteRT-LM)

Whisk uses on-device LLMs via Google's MediaPipe Tasks GenAI and LiteRT-LM. 
To fully test the on-device AI features:
1. Download a compatible model (e.g., `Gemma 4 E2B .task` or `.bin` format) from the official LiteRT community or Hugging Face.
2. Push the model to the device's external storage or place it in the app's `assets/models` folder (if supported by your current branch).
3. Update the model path in the Settings UI inside the app to point to your downloaded model file.

## Troubleshooting

- **Gradle Sync Fails**: Double-check that your Android Studio is pointing to JDK 17 (Settings -> Build, Execution, Deployment -> Build Tools -> Gradle -> Gradle JDK).
- **Out of Memory on Build**: You may need to increase the Gradle heap size in `gradle.properties` (e.g., `org.gradle.jvmargs=-Xmx4g`).