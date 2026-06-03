# MiniLauncher

Minimal, fast Android launcher. Zero bloat, zero tracking, zero network calls.

A home screen that gets out of your way.

## Features

- **Instant** — Cold start < 300ms. App list renders < 100ms.
- **Minimal** — APK < 3MB. No ads, no analytics, no internet permission.
- **Functional** — MVI architecture with pure reducers. Every state transition is deterministic, testable, and replayable.
- **Resilient** — Defensive PackageManager, DataStore corruption recovery, debounced package events. One bad app doesn't crash your launcher.

## Tech Stack

| Component | Choice |
|-----------|--------|
| Language | Kotlin 2.1+ |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVI (unidirectional data flow) |
| DI | Hilt |
| Async | Coroutines + Flow |
| Persistence | DataStore (Proto + Preferences) |
| Build | AGP 9.x, KSP, R8 full mode |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 36 (Android 16) |

## Development Setup

### Option 1: Dev Container (Recommended)

The project ships a Dev Container that gives you a fully configured Android development environment in seconds. No SDK downloads, no JDK version conflicts, no "works on my machine."

**Prerequisites:**
- [Docker](https://docs.docker.com/get-docker/)
- [VS Code](https://code.visualstudio.com/) with the [Dev Containers extension](https://marketplace.visualstudio.com/items?itemName=ms-vscode-remote.remote-containers) — or any editor with Dev Container support (JetBrains Gateway, Neovim, etc.)

**Steps:**

```bash
# 1. Clone the repo
git clone https://github.com/<your-org>/minilauncher.git
cd minilauncher

# 2. Open in VS Code
code .

# 3. When prompted, click "Reopen in Container"
#    Or: Ctrl+Shift+P → "Dev Containers: Reopen in Container"
```

The container builds with JDK 21, Android SDK 36, and all required build tools. First build takes ~5 minutes for SDK downloads. Subsequent starts are instant.

**What's inside the container:**

| Tool | Version | Purpose |
|------|---------|---------|
| JDK | 21 (LTS) | Kotlin + AGP 9.x compilation |
| Android SDK Command Line Tools | latest | SDK manager |
| Android SDK Platform | 36 | Target SDK |
| Android SDK Build Tools | 36.x.x | AAPT2, D8, R8 |
| Gradle | via wrapper | Build system |
| ktlint | latest | Code style enforcement |

**Running builds inside the container:**

```bash
# Debug build
./gradlew assembleDebug

# Run unit tests
./gradlew test

# Run Android instrumented tests (requires emulator or device)
./gradlew connectedAndroidTest

# Lint check
./gradlew ktlintCheck

# Clean build
./gradlew clean assembleDebug
```

### Option 2: Local Setup

If you prefer to develop outside a container, here's what you need.

**Prerequisites:**

| Requirement | Version | Install |
|-------------|---------|---------|
| JDK | 21 (LTS) | [Adoptium](https://adoptium.net/) or `sdk install java` |
| Android SDK | Platform 36 | Via Android Studio or [command line tools](https://developer.android.com/studio#command-line-tools-only) |
| Android Build Tools | 36.x.x | `sdkmanager "build-tools;36.x.x"` |
| Kotlin | 2.1+ | Bundled via Gradle |
| Android Studio | Meerkat (2025.1) or newer | [Download](https://developer.android.com/studio) |

**Steps:**

```bash
# 1. Set JAVA_HOME
export JAVA_HOME=/path/to/jdk-21

# 2. Set ANDROID_HOME (if not set by Android Studio)
export ANDROID_HOME=$HOME/Android/Sdk   # Linux
export ANDROID_HOME=$HOME/Library/Android/sdk  # macOS

# 3. Accept SDK licenses
$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager --licenses

# 4. Install required SDK components
$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager \
  "platforms;android-36" \
  "build-tools;36.0.0" \
  "platform-tools"

# 5. Clone and build
git clone https://github.com/<your-org>/minilauncher.git
cd minilauncher
./gradlew assembleDebug
```

**Verifying your setup:**

```bash
# All checks should pass
./gradlew check
```

### Option 3: Android Studio (GUI)

1. Install [Android Studio Meerkat](https://developer.android.com/studio) or newer
2. Open the project: **File → Open → select the `minilauncher` directory**
3. Android Studio will prompt to sync Gradle. Accept.
4. JDK 21 should be auto-configured. If not: **File → Project Structure → SDK Location → JDK 21**
5. Run the app on a device or emulator.

## Project Structure

```
minilauncher/
├── .devcontainer/          ← Containerized dev environment
│   ├── Dockerfile          ← JDK 21 + Android SDK
│   ├── devcontainer.json   ← VS Code / Codespaces config
│   └── setup.sh            ← SDK component installation
├── app/                    ← Application module (start here, split later)
├── baseline-profile/        ← Macrobenchmark + profile generator
├── docs/
│   └── architecture-research.md  ← Architecture decision record
├── AGENTS.md               ← Development guidelines and conventions
├── README.md               ← This file
└── .editorconfig           ← Code style (ktlint-compatible)
```

The project starts as a single `:app` module. Modules are extracted when needed (YAGNI):

| When | Extract |
|------|---------|
| 2+ features share data logic | `:core:data` |
| 2+ screens share Compose components | `:core:designsystem` |
| Incremental builds exceed 30s | `:feature:*` per screen |

## Architecture

MiniLauncher uses **MVI (Model-View-Intent)** with unidirectional data flow:

```
User Action → Intent → Reducer (pure function) → New UiState → Compose Render
                                    ↓
                              Side Effects (Channel)
```

**Key rules:**
- **Reducer is pure** — no coroutines, no side effects, no `Job` handles
- **Single immutable state** per screen — `@Immutable` data classes, `ImmutableList`
- **Effects via `Channel<Effect>(BUFFERED)`** — never `SharedFlow` for one-shot events
- **Screen is a pure function** of `(state, actions)` — zero Android imports

See [AGENTS.md](./AGENTS.md) for full conventions, templates, and anti-patterns.

## Testing

```bash
# Unit tests (pure JUnit, no Android framework)
./gradlew test

# Instrumented tests (requires device/emulator)
./gradlew connectedAndroidTest

# Lint
./gradlew ktlintCheck

# All checks
./gradlew check
```

**Coverage targets:** Reducers 95%+, Repositories 80%+, Compose UI 60%+, Store/ViewModel 70%+.

## Contributing

1. Fork the repo
2. Create a feature branch: `feat/my-feature`
3. Make your changes. Follow [AGENTS.md](./AGENTS.md) conventions.
4. Run `./gradlew check` — all tests and lint must pass.
5. Open a pull request.

**Commit style:** Conventional commits. `feat:`, `fix:`, `perf:`, `refactor:`, `test:`, `docs:`.

## License

[Apache License 2.0](./LICENSE) — use it, modify it, ship it. Just don't blame us if your home screen gets too fast.