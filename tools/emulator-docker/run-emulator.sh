#!/usr/bin/env bash
# Starts the Android emulator inside Docker, with its window on the host desktop.
# Needed because the host's /etc/ld.so.preload (Citrix App Protection) crashes the emulator.
# The container uses host networking, so the host's adb sees the emulator as usual.
set -euo pipefail

SDK="${ANDROID_HOME:-$HOME/Android/Sdk}"
AVD="${1:-HealthTracker_Phone}"
XAUTH="${XAUTHORITY:-$HOME/.Xauthority}"

docker build -q -t healthtracker-emulator "$(dirname "$0")" >/dev/null

exec docker run --rm --name healthtracker-emulator \
    --network host \
    --device /dev/kvm --group-add "$(getent group kvm | cut -d: -f3)" \
    --user "$(id -u):$(id -g)" \
    -e HOME=/tmp/home -e ANDROID_HOME="$SDK" -e ANDROID_USER_HOME="$HOME/.android" -e ANDROID_AVD_HOME="$HOME/.android/avd" \
    -e DISPLAY="$DISPLAY" -e XAUTHORITY=/tmp/.Xauthority \
    -v /tmp/.X11-unix:/tmp/.X11-unix:ro -v "$XAUTH:/tmp/.Xauthority:ro" \
    -v "$SDK:$SDK" -v "$HOME/.android:$HOME/.android" -v "$HOME/.android:/tmp/home/.android" \
    -v /etc/localtime:/etc/localtime:ro \
    healthtracker-emulator \
    "$SDK/emulator/emulator" -avd "$AVD" -gpu swiftshader_indirect -no-snapshot-save -no-boot-anim -no-audio
