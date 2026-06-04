# Android Emulator Setup

The dev container includes a fully configured Android emulator (Pixel 8, API 36) with VNC display access. No physical device or host SDK required.

## Quick Start

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

## Accessing the Emulator

| Method | Address | Password |
|--------|---------|----------|
| **Browser (noVNC)** | `http://localhost:6080` | `minilauncher` |
| **VNC client** | `vnc://localhost:5900` | `minilauncher` |
| **ADB** | `adb devices` | — |

VS Code will auto-forward ports 6080 and 5900. The noVNC browser view opens automatically when the emulator starts.

## How It Works

The emulator runs inside the Docker container with a virtual display stack:

```
Xvfb (virtual display :99)
  └─ Android Emulator (Pixel 8, API 36, x86_64)
  └─ x11vnc (VNC server on port 5900)
  └─ websockify + noVNC (browser VNC on port 6080)
```

- **Xvfb** provides a virtual X11 display so the emulator can render without a physical monitor
- **x11vnc** shares that display over VNC
- **websockify + noVNC** makes the VNC stream accessible from a web browser
- **supervisord** manages the display stack processes (auto-restart on crash)

## KVM Acceleration

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

## Building & Installing on the Emulator

```bash
# Build debug APK
./gradlew assembleDebug

# Install on running emulator
adb install app/build/outputs/apk/debug/app-debug.apk

# Or build and install in one step
./gradlew installDebug

# View logs
adb logcat

# Run instrumented tests
./gradlew connectedAndroidTest
```

## Troubleshooting

### Emulator won't start

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

## Architecture Details

| Component | Version | Purpose |
|-----------|---------|---------|
| Android Emulator | Latest (SDK 36) | Device simulation |
| System Image | `android-36;google_apis;x86_64` | API 36 with Google APIs |
| AVD | Pixel 8 API 36 | Pre-configured virtual device |
| Xvfb | X11 virtual framebuffer | Headless display |
| x11vnc | VNC server | Remote display access |
| noVNC | Browser VNC client | Web-based display access |
| supervisord | Process manager | Auto-restart display services |