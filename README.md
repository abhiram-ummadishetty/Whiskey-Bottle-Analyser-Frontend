# Edge AI — Android (Kotlin)

Native Android client that mirrors the web PWA: local signup/login, per-launch
backend URL configuration, live camera streaming to the Python YOLO + OCR
backend, and a Meta Wearables (smart-glasses) video source.

## Modules

- `ui/auth` — Room-backed signup/login (first name, last name, password). MVP
  uses a salted SHA-256 hash; swap for `androidx.security` / Argon2 before
  shipping.
- `ui/gate` — Per-launch backend URL prompt, mirrors `BackendGate` on the web.
- `ui/home` — "Hi {firstName}, what are we analysing today?" with Phone Camera
  and Meta Glasses entry points.
- `ui/camera` — CameraX preview, throttled JPEG capture, POST `/infer`,
  overlay bounding boxes + OCR labels with IoU-tracked smoothing.
- `ui/glasses` — Meta Wearables CameraAccess integration (see
  `glasses/MetaGlassesSource.kt`). Falls back to the rear camera while the
  glasses are not paired so the pipeline can be validated end-to-end.

## Permissions

Requested at runtime, only when needed:

- `CAMERA` — phone camera preview.
- `RECORD_AUDIO` — voice summaries / future audio capture.
- `BLUETOOTH_CONNECT` + `BLUETOOTH_SCAN` (API 31+) — pair with Meta Glasses.
- `ACCESS_FINE_LOCATION` — required by Android for BLE scanning on some OEMs.
- `INTERNET` — talk to the Python backend on the LAN.

The glasses permissions (`BLUETOOTH_*`) are only prompted from
`GlassesActivity` when the user opts in to pair, not at app launch.

## Meta Glasses

`glasses/MetaGlassesSource.kt` is integrated with Meta's `mwdat` SDK (Device Access Toolkit). 

To complete the setup:
1. **GitHub Token**: The SDK is hosted on GitHub Packages. You must add your GitHub username (`gpr.user`) and a Personal Access Token (`gpr.key` with `read:packages` scope) to your `local.properties` file.
2. **App Credentials**: The app is currently configured with dev IDs ("0"). See `app/build.gradle.kts` for production updates.
3. **Registration**: Ensure your glasses are paired in the Meta View app before starting the stream.

## Backend

No code change. Point the gate at the same FastAPI service in `../backend/`.

## Build

```
cd android
./gradlew :app:assembleDebug
```

Min SDK 29 (Android 10), target SDK 35.
