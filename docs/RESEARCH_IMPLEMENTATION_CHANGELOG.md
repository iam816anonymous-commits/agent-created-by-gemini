# Research-Informed Implementation: Before/After Record

Base: `ffad2d0d56f8c146d7cd5d065c903dc782a0e8b2` (main at branch creation)

## Scope

This first implementation is deliberately limited to two safety properties inspired by mobile-agent research on fresh observations, explicit user control, and evidence-based action dispatch. It does not port another project's runtime or introduce a new dependency.

## Changes

### 1. Coordinate-only target resolution

**Before:** `TargetResolver` could manufacture a clickable `UiNode` from normalized coordinates, including a hardcoded 1080×2400 fallback, and return `Success`. The executor treated that success as permission to dispatch a tap.

**After:** If the snapshot has no accessible nodes or no semantic target matches, resolution fails explicitly. Coordinate guesses are not converted into verified target matches. This intentionally disables the unsafe fallback until a separate, explicitly unverified coordinate-action contract and safe dispatch policy are designed.

### 2. Android permission dialogs

**Before:** `InterruptionDetector` created a click command for text such as “Allow” and the executor could dispatch it as automatic recovery. Failed target resolution could also trigger scroll exploration and retry/recovery.

**After:** Permission-controller screens are classified by the window package or node package and require explicit user action. The detector returns no grant/dismiss command. The live click executor stops before dispatch, retries and automated recovery, and records a consent-required inconclusive result.

### 3. Missing live observations

**Before:** If a live click had no current snapshot, target resolution failed but the self-healing path could still dispatch a scroll exploration gesture.

**After:** A missing live snapshot stops the click step before any gesture, retry or automated recovery. The outcome is explicitly inconclusive rather than pretending the target could be safely resolved.

### 4. Regression coverage

- Added resolver tests for empty observations and absent semantic matches.
- Updated the permission-interruption test to assert that no automated permission command is produced.
- Added coverage for permission-controller node-package detection when the window package differs.

## Evidence and limitations

- Source changes and regression-test source are committed on a separate branch.
- Unit tests and Android compilation have **not** been executed by this GitHub API editing workflow. CI results must be checked before merging.
- Physical-device permission behavior and accessibility dispatch remain unverified until exercised on a device.
- No changes were made to main and no pull request was merged.
