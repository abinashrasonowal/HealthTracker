# Runtime libraries for the Android emulator. The SDK itself is mounted from the host.
FROM ubuntu:24.04
RUN apt-get update && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
        libx11-6 libx11-xcb1 libxext6 libxrender1 libxi6 libxtst6 libxrandr2 libxcursor1 libxcomposite1 \
        libxdamage1 libxfixes3 libxkbcommon0 libxkbcommon-x11-0 libxkbfile1 libxcb1 libxcb-icccm4 \
        libxcb-image0 libxcb-keysyms1 libxcb-randr0 libxcb-render-util0 libxcb-shape0 libxcb-xfixes0 \
        libxcb-xinerama0 libxcb-xkb1 libxcb-cursor0 libgl1 libegl1 libglx-mesa0 libgl1-mesa-dri \
        libpulse0 libasound2t64 libnss3 libdbus-1-3 libfontconfig1 libfreetype6 libglib2.0-0t64 \
        libdrm2 libgbm1 libbsd0 libuuid1 libtinfo6 libsm6 libice6 tzdata ca-certificates \
    && rm -rf /var/lib/apt/lists/*
