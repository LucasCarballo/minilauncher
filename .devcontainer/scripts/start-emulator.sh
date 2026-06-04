#!/usr/bin/env bash
# start-emulator.sh — Launch the Android emulator with VNC display in the dev container
#
# Usage:
#   start-emulator.sh              Start emulator with VNC (default)
#   start-emulator.sh --headless   Start emulator headless (no display, adb only)
#   start-emulator.sh --stop       Stop emulator and VNC services
#   start-emulator.sh --status     Check if emulator is running
#
# Access:
#   VNC client  → localhost:5900 (password: minilauncher)
#   Browser      → http://localhost:6080 (noVNC, password: minilauncher)
#   ADB          → adb devices (auto-connected)

set -euo pipefail

VNC_PASSWORD="minilauncher"
VNC_PORT=5900
NOVNC_PORT=6080
DISPLAY_NUM=99
AVD_NAME="Pixel_6_API_36"
DISPLAY=":$DISPLAY_NUM"
SUPERVISORD_PID_FILE="/tmp/supervisord-emulator.pid"
EMULATOR_LOG="/tmp/emulator.log"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

info()  { echo -e "${CYAN}[INFO]${NC} $*"; }
ok()    { echo -e "${GREEN}[OK]${NC} $*"; }
warn()  { echo -e "${YELLOW}[WARN]${NC} $*"; }
error() { echo -e "${RED}[ERROR]${NC} $*"; }

check_kvm() {
    if [ -e /dev/kvm ]; then
        ok "KVM acceleration available (/dev/kvm)"
        return 0
    else
        warn "KVM not available — emulator will use software rendering (slow)"
        warn "Add --device=/dev/kvm to your Docker run args for hardware acceleration"
        return 1
    fi
}

stop_services() {
    info "Stopping emulator and display services..."

    # Stop emulator
    pkill -f "emulator.*$AVD_NAME" 2>/dev/null || true
    pkill -f "qemu" 2>/dev/null || true

    # Stop supervisord (which manages Xvfb, x11vnc, websockify)
    if [ -f "$SUPERVISORD_PID_FILE" ]; then
        local pid
        pid=$(cat "$SUPERVISORD_PID_FILE")
        if kill -0 "$pid" 2>/dev/null; then
            supervisorctl -c /etc/supervisor/conf.d/supervisord.conf shutdown 2>/dev/null || true
            # Give it a moment, then force kill if needed
            sleep 2
            kill "$pid" 2>/dev/null || true
        fi
        rm -f "$SUPERVISORD_PID_FILE"
    fi

    # Kill any lingering processes
    pkill -f "Xvfb.*:$DISPLAY_NUM" 2>/dev/null || true
    pkill -f "x11vnc" 2>/dev/null || true
    pkill -f "websockify" 2>/dev/null || true

    ok "All services stopped"
}

show_status() {
    if pgrep -f "emulator.*$AVD_NAME" > /dev/null 2>&1; then
        ok "Emulator is running (AVD: $AVD_NAME)"
        echo ""
        adb devices -l 2>/dev/null || echo "  (adb not connected yet)"
    else
        info "Emulator is not running"
    fi

    if pgrep -f "Xvfb.*:$DISPLAY_NUM" > /dev/null 2>&1; then
        ok "Xvfb is running (display :$DISPLAY_NUM)"
    else
        info "Xvfb is not running"
    fi

    if pgrep -f "x11vnc" > /dev/null 2>&1; then
        ok "VNC server is running (port $VNC_PORT)"
    else
        info "VNC server is not running"
    fi

    if pgrep -f "websockify" > /dev/null 2>&1; then
        ok "noVNC is running (port $NOVNC_PORT)"
    else
        info "noVNC is not running"
    fi
}

wait_for_emulator() {
    local timeout=${1:-120}
    info "Waiting for emulator to boot (timeout: ${timeout}s)..."
    local elapsed=0
    while [ $elapsed -lt $timeout ]; do
        if adb shell getprop sys.boot_completed 2>/dev/null | grep -q "1"; then
            ok "Emulator booted successfully"
            return 0
        fi
        sleep 2
        elapsed=$((elapsed + 2))
    done
    warn "Emulator boot timed out after ${timeout}s — it may still be starting"
    return 1
}

start_with_vnc() {
    info "Starting Android emulator with VNC display..."

    # Fix KVM permissions — Docker passes /dev/kvm with host GID which may not match container's kvm group
    if [ -e /dev/kvm ]; then
        chmod 666 /dev/kvm 2>/dev/null || true
        ok "KVM acceleration available (/dev/kvm)"
    else
        warn "KVM not available — emulator will use software rendering (slow)"
        warn "Add --device=/dev/kvm to your Docker run args for hardware acceleration"
    fi

    # Start display stack via supervisord
    info "Starting virtual display (Xvfb + VNC + noVNC)..."
    supervisord -c /etc/supervisor/conf.d/supervisord.conf
    SUPERVISORD_PID=$(pgrep -f "supervisord.*supervisord.conf" | head -1)
    if [ -n "$SUPERVISORD_PID" ]; then
        echo "$SUPERVISORD_PID" > "$SUPERVISORD_PID_FILE"
    fi

    # Wait for display to be ready
    sleep 2

    if ! pgrep -f "Xvfb.*:$DISPLAY_NUM" > /dev/null 2>&1; then
        error "Xvfb failed to start. Check /tmp/xvfb-error.log"
        return 1
    fi
    ok "Virtual display is ready"

    # Build emulator command
    local KVM_FLAG="-accel on"
    if [ ! -e /dev/kvm ]; then
        KVM_FLAG="-accel off"
    fi

    # Launch emulator on the virtual display
    info "Launching emulator (AVD: $AVD_NAME)..."
    DISPLAY="$DISPLAY" \
    ANDROID_HOME="/opt/android-sdk" \
    JAVA_HOME="/usr/lib/jvm/java-21-openjdk-amd64" \
    emulator \
        -avd "$AVD_NAME" \
        -gpu swiftshader_indirect \
        -no-audio \
        -no-boot-anim \
        -no-snapshot-save \
        $KVM_FLAG \
        > "$EMULATOR_LOG" 2>&1 &

    wait_for_emulator 120

    echo ""
    ok "Emulator is ready!"
    echo ""
    echo "  ┌─────────────────────────────────────────────────┐"
    echo "  │  VNC:      vnc://localhost:$VNC_PORT"
    echo "  │             password: $VNC_PASSWORD"
    echo "  │"
    echo "  │  Browser:  http://localhost:$NOVNC_PORT"
    echo "  │             password: $VNC_PASSWORD"
    echo "  │"
    echo "  │  ADB:      adb devices"
    echo "  └─────────────────────────────────────────────────┘"
    echo ""
}

start_headless() {
    info "Starting Android emulator in headless mode..."
    check_kvm

    local KVM_FLAG="-accel on"
    if [ ! -e /dev/kvm ]; then
        KVM_FLAG="-accel off"
    fi

    emulator \
        -avd "$AVD_NAME" \
        -no-window \
        -no-audio \
        -no-boot-anim \
        -gpu off \
        $KVM_FLAG \
        -no-snapshot-save \
        > "$EMULATOR_LOG" 2>&1 &

    # Wait for emulator to appear in adb
    info "Waiting for emulator in adb..."
    local timeout=60
    local elapsed=0
    while [ $elapsed -lt $timeout ]; do
        if adb devices 2>/dev/null | grep -q "emulator"; then
            ok "Emulator connected via adb"
            break
        fi
        sleep 2
        elapsed=$((elapsed + 2))
    done

    if [ $elapsed -ge $timeout ]; then
        warn "Emulator not detected in adb after ${timeout}s"
    fi

    echo ""
    ok "Headless emulator is ready!"
    echo "  ADB:  adb devices"
    echo ""
}

case "${1:-}" in
    --headless)
        start_headless
        ;;
    --stop)
        stop_services
        ;;
    --status)
        show_status
        ;;
    --help|-h)
        echo "Usage: start-emulator.sh [OPTION]"
        echo ""
        echo "Options:"
        echo "  (none)       Start emulator with VNC display (default)"
        echo "  --headless   Start emulator without display (adb only)"
        echo "  --stop       Stop emulator and all display services"
        echo "  --status     Show running services status"
        echo "  --help       Show this help message"
        echo ""
        echo "Access:"
        echo "  VNC:      vnc://localhost:5900  (password: minilauncher)"
        echo "  Browser:  http://localhost:6080 (password: minilauncher)"
        echo "  ADB:      adb devices"
        ;;
    *)
        start_with_vnc
        ;;
esac