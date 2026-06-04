# MiniLauncher Dev Container
# JDK 21 + Android SDK + Emulator for reproducible builds and on-device testing
# No "works on my machine" — every contributor gets the same environment.

FROM ubuntu:24.04

# Prevent interactive prompts during package installation
ENV DEBIAN_FRONTEND=noninteractive

# JDK 21 (required by AGP 9.x)
# Android SDK Command Line Tools
# Git, curl, unzip for SDK setup
# OpenSSH for potential remote access
# Display server: xvfb (virtual framebuffer), x11vnc (VNC server), novnc (browser VNC)
# Emulator deps: libgl1-mesa-glx (OpenGL), libpulse0 (audio), supervisor (process mgmt)
RUN apt-get update && apt-get install -y --no-install-recommends \
    openjdk-21-jdk-headless \
    git \
    curl \
    unzip \
    openssh-client \
    ca-certificates \
    xvfb \
    x11vnc \
    novnc \
    websockify \
    supervisor \
    libgl1 \
    libpulse0 \
    && rm -rf /var/lib/apt/lists/*

ENV JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64

# Android SDK directory
ENV ANDROID_HOME=/opt/android-sdk
ENV ANDROID_SDK_ROOT=/opt/android-sdk

# Add Android SDK to PATH
ENV PATH="${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools:${ANDROID_HOME}/emulator:${PATH}"

# Virtual display for headless emulator (used by start-emulator.sh)
ENV DISPLAY=:99

# Install Android SDK Command Line Tools
ENV CMDLINE_TOOLS_VERSION=11076708
RUN mkdir -p ${ANDROID_HOME}/cmdline-tools && \
    curl -fsSL "https://dl.google.com/android/repository/commandlinetools-linux-${CMDLINE_TOOLS_VERSION}_latest.zip" \
    -o /tmp/cmdline-tools.zip && \
    unzip -q /tmp/cmdline-tools.zip -d ${ANDROID_HOME}/cmdline-tools && \
    mv ${ANDROID_HOME}/cmdline-tools/cmdline-tools ${ANDROID_HOME}/cmdline-tools/latest && \
    rm /tmp/cmdline-tools.zip

# Accept all SDK licenses upfront
RUN yes | sdkmanager --licenses > /dev/null 2>&1 || true

# Install required SDK components
# Platform 36 (Android 16, target SDK)
# Build tools (latest for platform 36)
# Platform tools (adb, etc.)
# Android Support + Google Maven repos for AndroidX, Hilt, etc.
# Emulator + system image for on-device testing
RUN sdkmanager --install \
    "platforms;android-36" \
    "build-tools;36.0.0" \
    "platform-tools" \
    "extras;android;m2repository" \
    "extras;google;m2repository" \
    "emulator" \
    "system-images;android-36;google_apis;x86_64" \
    && rm -rf ${ANDROID_HOME}/.temp

# Accept licenses again after new packages
RUN yes | sdkmanager --licenses > /dev/null 2>&1 || true

# Create a non-root user for development
ARG USERNAME=dev
ARG USER_UID=1000
ARG USER_GID=1000

# Remove the default ubuntu user that occupies UID/GID 1000
# Add kvm group for hardware-accelerated emulator
RUN if id -u ubuntu > /dev/null 2>&1; then userdel -r ubuntu 2>/dev/null || true; fi && \
    groupadd --gid ${USER_GID} ${USERNAME} && \
    groupadd --gid 108 kvm 2>/dev/null || true && \
    useradd --uid ${USER_UID} --gid ${USER_GID} --groups kvm --create-home --shell /bin/bash ${USERNAME} && \
    chown -R ${USERNAME}:${USERNAME} ${ANDROID_HOME}

# Create AVD for the emulator (Pixel 6, API 36, x86_64)
# --force overwrites if it already exists
# Note: pixel_8 profile is not available in all SDK versions, pixel_6 is the latest guaranteed
RUN echo "no" | avdmanager create avd \
    -n Pixel_6_API_36 \
    -k "system-images;android-36;google_apis;x86_64" \
    -d "pixel_6" \
    --force

# Copy supervisor config for VNC stack
COPY supervisord.conf /etc/supervisor/conf.d/supervisord.conf

# Copy emulator startup script
COPY scripts/start-emulator.sh /usr/local/bin/start-emulator.sh
RUN chmod +x /usr/local/bin/start-emulator.sh

# Gradle wrapper will be downloaded on first build
# Pre-warm Gradle distribution by creating the gradle cache directory
USER ${USERNAME}
RUN mkdir -p /home/${USERNAME}/.gradle

WORKDIR /workspace

# Default: bash login shell
CMD ["bash"]