# KingShot — Product & Engineering Blueprint

**Version:** 0.1 · Prototype stage

## Product thesis
KingShot gives Android photographers meaningful control over capture processing while keeping computational photography optional. It should distinguish settings requested by the app, settings the camera reports as supported, and processing that remains under vendor control.

## Principles
1. Choice between Natural, Computational and Custom profiles.
2. Never claim a JPEG is completely unprocessed.
3. Preserve originals; edits should be non-destructive.
4. Show only controls supported by the active camera.
5. Keep capture fast and privacy-first; local processing by default.
6. Prove capture reliability before adding complex AI features.

## Feature roadmap
- **Camera Lab MVP:** Camera2 preview, capability report, JPEG capture, supported minimal-control requests.
- **Capture foundations:** RAW_SENSOR/DNG capture with reliable timestamp matching, exposure/focus controls, lens selection and orientation handling.
- **Review:** original/rendered pairing, capture metadata, side-by-side comparison.
- **Computational pipeline:** multi-frame HDR/night, denoising, tone mapping and portrait depth where feasible.
- **Creator tools:** video frame-rate/resolution controls, stabilization and export presets.

## Technical reality
The imaging pipeline includes sensor/readout, vendor HAL/ISP, Android camera controls and the app renderer. Public Camera2 requests control only documented and supported settings. Some vendor processing is not exposed. RAW_SENSOR can provide a flexible sensor-domain workflow when supported, but is not a guarantee of absolutely untouched data. JPEG is already rendered. Noise reduction, edge and shading Off should be requested only when reported in available modes. Pixel Camera's proprietary algorithms cannot simply be invoked by a third-party app.

## Architecture
UI → Capture Orchestrator → Camera2 capability registry → Camera session → JPEG/RAW readers → MediaStore + metadata → optional render pipeline.

Suggested modules: camera, capabilities, capture, raw, processing, media, profiles and diagnostics. Production capture should use a bounded processing queue, timestamp-based pairing of RAW images and TotalCaptureResult, lifecycle tests, output-stream compatibility checks, orientation handling, and robust storage/error handling.

## Research/verification rule
Features attributed to specific future generations such as Pixel 11 or iPhone 18 must be checked against official specifications for the exact model and region before they are presented as confirmed. This blueprint does not promise access to proprietary Apple or Google camera algorithms.

## Validation checklist
- Compile on CI and Android Studio.
- Test on at least two physical Android devices, including a Pixel.
- Verify permission denial/retry, lifecycle pause/resume, storage failures and capture latency.
- Compare repeated scenes under fixed lighting and document each device's available request modes.
- Validate RAW/DNG files in external viewers before calling the pipeline reliable.
- Add release signing only after security and device QA.
