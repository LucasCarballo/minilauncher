# Android Emulator Setup

The dev container includes a fully configured Android emulator (Pixel 6, API 36) with VNC display access. No physical device or host SDK required.

> **Note:** The in-container emulator requires KVM + GPU support that Docker doesn't reliably provide. For the best experience, we recommend running the emulator on your **host machine** (see [Running on the Host](#running-on-the-host) below). The container setup works for builds, tests, and ADB — the emulator is a bonus when it works.

---

## Option A: Running on the Host (Recommended)

Running the emulator on your host machine gives you hardware-accelerated graphics, proper GPU support, and a smooth experience. The dev container handles builds and ADB — the emulator just needs to be reachable over the network.

### Prerequisites

1. **Android Studio** or **command-line tools** installed on your host
2. **KVM enabled** on your host (Linux) or **HAXM** (macOS/Windows)
3. An **AVD** created with API 36 (or any API ≥ 26)

### Linux

```bash
# 1. Install Android SDK command-line tools (if not already installed)
#    Skip this if you have Android Studio
mkdir -p ~/Android/Sdk/cmdline-tools
curl -fsSL https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip \
  -o /tmp/cmdline-tools.zip && \
  unzip -q /tmp/cmdline-tools.zip -d ~/Android/Sdk/cmdline-tools && \
  mv ~/Android/Sdk/cmdline-tools/cmdline-tools ~/Android/Sdk/cmdline-tools/latest && \
  rm /tmp/cmdline-tools.zip

# 2. Set environment variables
export ANDROID_HOME=$HOME/Android/Sdk
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator

# 3. Accept licenses and install emulator + system image
yes | sdkmanager --licenses
sdkmanager --install "platforms;android-36" "build-tools;36.0.0" "platform-tools" "emulator" "system-images;android-36;google_apis;x86_64"

# 4. Create an AVD
echo "no" | avdmanager create avd -n Pixel_6_API_36 -k "system-images;android-36;google_apis;x86_64" -d "pixel_6" --force

# 5. Start the emulator
emulator -avd Pixel_6_API_36

# 6. Connect ADB from the container
#    In another terminal (or from the dev container):
adb connect localhost:5555
```

### macOS

```bash
# 1. Install Android Studio from https://developer.android.com/studio
#    Or install via Homebrew:
brew install --cask android-studio

# 2. Open Android Studio → Tools → Device Manager → Create Device
#    Select "Pixel 6" → Choose system image "API 36" → Finish

# 3. Start the emulator from Android Studio or command line:
$ANDROID_HOME/emulator/emulator -avd Pixel_6_API_36

# 4. Connect ADB from the container
adb connect localhost:5555
```

### Connecting the Container to the Host Emulator

The dev container and host share the workspace via bind mount. To connect the container's ADB to the host emulator:

```bash
# From inside the dev container:
# Replace 172.17.0.1 with your host's Docker bridge IP (usually 172.17.0.1 on Linux)
adb connect 172.17.0.1:5555

# Or use host.docker.internal (works on Docker Desktop / macOS / Windows):
adb connect host.docker.internal:5555
```

### Building & Installing from the Container

```bash
# Inside the dev container:
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk

# Or in one step:
./gradlew installDebug
```

### Setting MiniLauncher as Default Launcher

After installing the APK:

1. Press the **Home** button on the emulator
2. Android will show a launcher picker — select **MiniLauncher**
3. Tap "Always" to set it as default

To switch back to the stock launcher: Settings → Apps → Default Apps → Home app.

---

## Option B: Running in the Container

The container includes an emulator with VNC display. This works best on Linux hosts with KVM support.

### Quick Start

```bash
# Start emulator with VNC display (default)
start-emulator.sh

# Start emulator without display (headless, adb only)
start-emulator.sh --headless

# Check running services
start-emulator.sh --status

# Stop everything
start-emulator.sh --stop
```

### Accessing the Emulator

| Method | Address | Password |
|--------|---------|----------|
| **Browser (noVNC)** | `http://localhost:6080` | `minilauncher` |
| **VNC client** | `vnc://localhost:5900` | `minilauncher` |
| **ADB** | `adb devices` | — |

VS Code will auto-forward ports 6080 and 5900. The noVNC browser view opens automatically when the emulator starts.

### Known Limitations

- **Segfault on startup** — The emulator may crash inside Docker due to missing GPU/Vulkan support. This is a known Docker limitation. Use the host emulator (Option A) instead.
- **Slow without KVM** — Software rendering is 5-10x slower than hardware acceleration.
- **No audio** — The container emulator runs with `-no-audio`.

### How It Works

The emulator runs inside the Docker container with a virtual display stack:

```
Xvfb (virtual display :99)
  └─ Android Emulator (Pixel 6, API 36, x86_64)
  └─ x11vnc (VNC server on port 5900)
  └─ websockify + noVNC (browser VNC on port 6080)
```

- **Xvfb** provides a virtual X11 display so the emulator can render without a physical monitor
- **x11vnc** shares that display over VNC
- **websockify + noVNC** makes the VNC stream accessible from a web browser
- **supervisord** manages the display stack processes (auto-restart on crash)

### KVM Acceleration

The dev container is configured to pass through `/dev/kvm` for hardware-accelerated emulation. This requires:

1. **Host kernel support** — KVM enabled in BIOS/UEFI (Intel VT-x or AMD-V)
2. **Docker run args** — `--device=/dev/kvm` (already in `devcontainer.json`)

If KVM is unavailable, the emulator falls back to software rendering (significantly slower). The startup script will warn you.

### Checking KVM on the host

```bash
# Linux — check if KVM device exists
ls -la /dev/kvm

# Check CPU virtualization support
grep -E 'vmx|svm' /proc/cpuinfo
```

---

## Building & Installing

```bash
# Build debug APK
./gradlew assembleDebug

# Install on running emulator (host or container)
adb install app/build/outputs/apk/debug/app-debug.apk

# Or build and install in one step
./gradlew installDebug

# View logs
adb logcat

# Run instrumented tests
./gradlew connectedAndroidTest
```

---

## Troubleshooting

### Emulator won't start (in container)

```bash
# Check if services are running
start-emulator.sh --status

# Check emulator logs
cat /tmp/emulator.log
cat /tmp/emulator-error.log

# Check display stack logs
cat /tmp/xvfb.log
cat /tmp/x11vnc.log
cat /tmp/websockify.log
```

If the emulator segfaults, use the host emulator instead (Option A).

### "KVM not available" warning

The emulator will still work but slowly. Ensure:
1. Your host BIOS has virtualization enabled (Intel VT-x / AMD-V)
2. The Docker container has `/dev/kvm` passed through (check `devcontainer.json` `runArgs`)
3. On Linux: `sudo chmod 666 /dev/kvm` (or add your user to the `kvm` group)

### ADB can't find the emulator

```bash
# Restart ADB server
adb kill-server && adb start-server

# Check connected devices
adb devices

# Connect to host emulator from container
adb connect host.docker.internal:5555   # macOS/Windows
adb connect 172.17.0.1:5555             # Linux
```

### VNC connection refused

```bash
# Verify x11vnc is running
pgrep -f x11vnc

# Check the port
ss -tlnp | grep 5900

# Restart the display stack
start-emulator.sh --stop
start-emulator.sh
```

### Rebuilding the container

After changes to `.devcontainer/`, rebuild in VS Code:

```
Ctrl+Shift+P → "Dev Containers: Rebuild Container"
```

Or with Docker CLI:

```bash
docker build -t minilauncher-dev .devcontainer/
```

---

## Architecture Details

| Component | Version | Purpose |
|-----------|---------|---------|
| Android Emulator | Latest (SDK 36) | Device simulation |
| System Image | `android-36;google_apis;x86_64` | API 36 with Google APIs |
| AVD | Pixel 6 API 36 | Pre-configured virtual device |
| Xvfb | X11 virtual framebuffer | Headless display |
| x11vnc | VNC server | Remote display access |
| noVNC | Browser VNC client | Web-based display access |
| supervisord | Process manager | Auto-restart display services |