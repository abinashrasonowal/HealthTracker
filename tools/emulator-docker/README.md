# Android emulator in Docker

The emulator crashes on this laptop because `/etc/ld.so.preload` loads Citrix App Protection
(`libAppProtection.so`), whose `dlsym` hook sends the emulator's `pthread_create` wrapper into
infinite recursion. Inside a container that preload isn't present, so the emulator runs normally.
The SDK and AVD are mounted from the host; the window appears on the host desktop.

```bash
tools/emulator-docker/run-emulator.sh          # starts the HealthTracker_Phone AVD
./gradlew installDebug                          # host adb sees the emulator (host networking)
```

If it refuses to start with "Running multiple emulators with the same AVD", a previous run
crashed and left lock files: `rm ~/.android/avd/HealthTracker_Phone.avd/*.lock`.
