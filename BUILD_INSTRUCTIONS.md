# IE COPILOT - Build & Deployment Guide

## 1. Prerequisites
- **Android Studio Ladybug (2024.2.1) or higher**
- **JDK 17**
- **Gradle 8.7+**

## 2. API Key Configuration (CRITICAL)
IE COPILOT uses the **Secrets Gradle Plugin** for secure API key management.
Do NOT hardcode keys in the source code.

1. Open the **Secrets panel** in AI Studio.
2. Add the following keys:
   - `GEMINI_API_KEY`: Your Google Gemini API Key.
   - `OPENAI_API_KEY`: (Optional) For OpenAI-compatible providers.
   - `NVIDIA_NIM_KEY`: (Optional) For NVIDIA NIM endpoints.

The build system will automatically inject these into `BuildConfig` at compile time.

## 3. Build Commands
Use the standard Gradle wrapper to build the application:

### Debug Build
```bash
gradle assembleDebug
```

### Production Release (Unsigned)
```bash
gradle assembleRelease
```

### Run Tests
```bash
# Unit and Robolectric Tests
gradle :app:testDebugUnitTest

# Visual Regression (Screenshot) Tests
gradle :app:verifyRoborazziDebug
```

## 4. Signing Config
For production release, update the `signingConfigs` block in `app/build.gradle.kts` with your enterprise keystore. 
**Note:** The current debug build uses the default `debug.keystore` provided by the environment.

## 5. Support & Developer
For technical inquiries or system extension, contact **Abhay Singh**.
*Industrial Engineering | Manufacturing Excellence*
