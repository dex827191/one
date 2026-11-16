# SSH Protocol — Xray-Core + Onering integration layer

This project now contains an integration layer for the `libv2ray.aar` published by
https://github.com/dharak36/xray-onering.

## Included
- `XrayCoreRuntime`: initializes the gomobile library and controls `CoreController`.
- `OneringConfig`: applies the documented `onering:yourdomain:bugdomain` TLS
  serverName convention and WebSocket Host header to a VMess outbound.
- Gradle picks up AAR files from `app/libs/`.
- GitHub Actions downloads the upstream Onering AAR before assembling the APK.

## Onering convention
The upstream README describes:
- address/server: bug domain
- WebSocket Host: your origin/server domain
- TLS SNI/serverName: `onering:yourdomain:bugdomain`
- transport: WebSocket, TLS

Example:
```kotlin
val configWithOnering = OneringConfig.apply(
    configJson = importedXrayJson,
    originDomain = "www.yourdomain.com",
    bugDomain = "support.zoom.us"
)
xrayRuntime.start(configWithOnering)
```

## Important integration boundary
`XrayCoreRuntime` runs Xray in proxy-only mode (`tunFd = 0`). A complete product
flow still needs to:
1. supply/import a valid Xray JSON config with a local SOCKS inbound;
2. route the app's VPN/TProxy ingress to that SOCKS inbound;
3. start/stop Xray from the foreground tunnel service and handle failure cleanup;
4. expose Xray profile fields/import UI.

The existing SSH tunnel engine is preserved and is not silently replaced. This integration
layer is deliberately separate so an Xray session cannot accidentally tear down an SSH
session. Do not treat the presence of the AAR alone as proof that the app's existing
Connect button already switches to Xray.

## GitHub Actions / repository layout
The Android project must be committed directly at the repository root. The root should
contain `gradlew`, `settings.gradle.kts`, `build.gradle.kts`, `gradle/`, and `app/`.
The workflow does not search for or extract project ZIP files. It checks out the source
from the repository root, downloads the Onering AAR into `app/libs/`, and builds the APK.

The workflow runs on pushes to `main` or `master` when project source/build files change,
on pull requests, and via `workflow_dispatch`.

## Build
GitHub Actions downloads the AAR from the Onering repository and then runs:
`./gradlew --no-daemon clean assembleDebug`

The AAR is not checked into this source archive. Local builds must place the upstream
`libv2ray.aar` at `app/libs/libv2ray.aar` first.
