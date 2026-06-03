# MiniLauncher Dev Container
# JDK 21 + Android SDK for reproducible builds
# No "works on my machine" — every contributor gets the same environment.

FROM ubuntu:24.04

# Prevent interactive prompts during package installation
ENV DEBIAN_FRONTEND=noninteractive

# JDK 21 (required by AGP 9.x)
# Android SDK Command Line Tools
# Git, curl, unzip for SDK setup
# OpenSSH for potential remote access
RUN apt-get update && apt-get install -y --no-install-recommends \
    openjdk-21-jdk-headless \
    git \
    curl \
    unzip \
    openssh-client \
    ca-certificates \
    && rm -rf /var/lib/apt/lists/*

ENV JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64

# Android SDK directory
ENV ANDROID_HOME=/opt/android-sdk
ENV ANDROID_SDK_ROOT=/opt/android-sdk

# Add Android SDK to PATH
ENV PATH="${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools:${ANDROID_HOME}/emulator:${PATH}"

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
RUN sdkmanager --install \
    "platforms;android-36" \
    "build-tools;36.0.0" \
    "platform-tools" \
    "extras;android;m2repository" \
    "extras;google;m2repository"

# Create a non-root user for development
ARG USERNAME=dev
ARG USER_UID=1000
ARG USER_GID=1000

# Remove the default ubuntu user that occupies UID/GID 1000
RUN if id -u ubuntu > /dev/null 2>&1; then userdel -r ubuntu 2>/dev/null || true; fi && \
    groupadd --gid ${USER_GID} ${USERNAME} && \
    useradd --uid ${USER_UID} --gid ${USER_GID} --create-home --shell /bin/bash ${USERNAME} && \
    chown -R ${USERNAME}:${USERNAME} ${ANDROID_HOME}

# Gradle wrapper will be downloaded on first build
# Pre-warm Gradle distribution by creating the gradle cache directory
USER ${USERNAME}
RUN mkdir -p /home/${USERNAME}/.gradle

WORKDIR /workspace

# Default: bash login shell
CMD ["bash"]