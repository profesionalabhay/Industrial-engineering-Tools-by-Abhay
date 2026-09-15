# IE COPILOT — VERSION 1

## Release Information
- **Application Name**: IE COPILOT
- **Description**: AI-powered Industrial Engineering Workbench
- **Version Code**: 1
- **Version Name**: 1.0
- **Package ID**: com.iecopilot.app
- **Platform**: Android (Native Jetpack Compose)

## Features Included (Version 1 Freeze)
- **Core Design System**: Unified "Stitch" design system utilizing Material 3, optimized for mobile workflows, adaptive for both portrait and landscape orientation.
- **Projects & Navigation**: Clean App Shell architecture supporting multiple projects, smooth navigation, and a centralized manufacturing data repository.
- **Data Model**: Local, in-memory `ManufacturingRepository` mocking standard Industrial Engineering architectures.
- **Process & Station Setup**: Define process structures, stations, and cycle time targets.
- **Elemental Time Study**: Stopwatch-driven workflow for measuring individual work elements, including performance rating and allowance application.
- **Video Study (Architecture ready)**: Local UI workflows ready to ingest video recordings for micro-motion analysis, integrating device camera capabilities.
- **Value Added Analysis (VA/NNVA/NVA)**: Categorize observations into standard lean waste buckets.
- **Yamazumi (Stacked Bar Charts)**: Visual workload distribution charts per station using Compose-native drawing.
- **Work Balance / Line Balance**: Compare elemental times against Takt Time to optimize operator assignment.
- **What-If Simulator**: Interactive sandbox to model "What-If" scenarios without destructing the active baseline (e.g., changing demand, adding operators).
- **Value Stream Mapping (VSM)**: High-level mapping of material and information flow.
- **Motion & Spaghetti Studies**: Workflows to track operator movement and micro-motions.
- **Capacity Planning**: Automated calculations for Takt Time, Cycle Time, UPH (Units per Hour), and required overtime based on demand parameters.
- **Manpower Planning**: Automated calculation of theoretical headcount vs. actual allocated operators, complete with a tactical recommendation engine for shared, floating, or multi-machine operators.

## Known Limitations (Version 1)
- **Offline / Syncing Behavior**: The current data architecture uses an in-memory repository mock. True local persistence (e.g., Room SQLite) or cloud syncing (e.g., Firebase/Spanner) is simulated. All entries persist only for the session lifecycle. 
- **Video Processing**: While camera/storage permissions and UI logic exist for Video Study, heavy on-device computer vision or AI processing is deferred to future server-side integrations.
- **Authentication**: No live auth gate is currently implemented; the app bypasses direct user authentication to immediately present the workspace.

## Deferred Version 2 Features
- Production-grade AI Copilot integrations.
- Advanced cloud reporting and dashboarding.
- Real-time OEE (Overall Equipment Effectiveness) integration.
- Continuous Kaizen tracking loops.
- Generative AI Standard Work documentation.
- Advanced Ergonomic risk assessments.
- True offline-first synchronization using Room/DataStore to a remote cloud environment.

## Build Information
- The application has been fully audited and successfully compiled via Gradle (`assembleDebug` and `assembleRelease` configurations are supported).
- **Target SDK**: 36
- **Minimum SDK**: 24
- **Required Permissions**: `INTERNET`, `CAMERA`.

## Deployment Instructions
- The source package can be exported as a complete ZIP from the current AI Studio environment.
- APK compilation is supported. To retrieve the `IE-Copilot-V1-release.apk` (or debug APK), use the platform's native export / download artifact functionality. 
- *Note for developers:* Do NOT expose the provided debug keystores or any future API secrets inside the client bundle when publishing to the Google Play Store.
