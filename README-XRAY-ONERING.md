# SSH Protocol — SSH + Xray-Core / Onering

This source has been extended with a separate SSH/Xray profile type and an Xray editor.

## Xray/Onering fields
- Protocol: VMess, VLESS, Trojan
- Server, port, UUID or Trojan password
- Transport: WebSocket or TCP
- Security: TLS or none
- WebSocket Host and Path
- TLS ServerName/SNI

The `ServerName` value is passed to Xray exactly as entered. For Onering use:

`onering:yourdomain:bugdomain`

Example from the upstream Onering README:
- Server: `support.zoom.us`
- Port: `443`
- Host: `www.yourdomain.com`
- ServerName: `onering:www.yourdomain.com:support.zoom.us`
- Network: `ws`
- Security: `tls`

## Bundled Xray binary

The supplied `libv2ray.aar` is included at `app/libs/libv2ray.aar`.
It contains `libv2ray.Libv2ray`, `libv2ray.CoreController`, and
`libv2ray.CoreCallbackHandler`, plus native `libgojni.so` libraries for
`arm64-v8a`, `armeabi-v7a`, `x86`, and `x86_64`.

The integration calls the AAR API directly. The actual AAR exposes
`CoreController.startLoop(String)` (one argument) and `stopLoop()`.
Its `geoip.dat` and `geosite.dat` assets are copied into the private
`assets` directory before `Libv2ray.initCoreEnv(...)`.

The app excludes its own UID from Android VPN routes so native Xray outbound
sockets do not loop back into the VPN; the local SOCKS inbound remains the
VPN tunnel's upstream.

## Build / test status

This is a source integration; a successful Android build and device-level connection test are still required. The GitHub Actions workflow checks the bundled AAR's exported API and builds an APK.

Existing profiles are migrated implicitly as SSH profiles (`connectionType=SSH`). SSH fields and the SSH tunnel path remain in place.

## Important notes

- Onering uses the patched Xray build in the linked AAR; a regular upstream Xray AAR may not understand the special `onering:` serverName.
- The Onering README states that the method is designed around Cloudflare CDN (CloudFront is also mentioned) and still requires the server domain to be behind a supported CDN.
- The APK has not been built in this environment: the Gradle wrapper could not download Gradle because outbound DNS/network access was unavailable. A device-level Xray/Onering connection test also remains outstanding.


## GitHub Actions build

The repository includes `.github/workflows/android-build.yml`.

1. Push this project to a GitHub repository.
2. Open **Actions** → **Android Build**.
3. Select **Run workflow**.
4. Choose `debug` (recommended for testing) or `release` (unsigned release APK).
5. Open the completed run and download the `ssh-protocol-...-apk` artifact.

Pushes to `main` or `master` also run a debug build automatically.
The workflow verifies the bundled `app/libs/libv2ray.aar` API before compiling.


## VLESS / VMess import and routing

The app accepts `vless://` and `vmess://` share links through Android VIEW intents
and shared text. Imported profiles are stored with `connectionType=XRAY`.
When such a profile is active, TunnelEngine starts Xray-Core and routes the VPN
SOCKS inbound to Xray; the SSH transport, SSH authentication and SSH payload
pipeline are not started. VLESS/VMess WebSocket path, Host, TLS, SNI/Onering and
TLS fingerprint are parsed from the share link.
