# SSH Protocol — Root Android project with Xray profile import scaffolding

This repository is laid out as an Android Gradle project directly at repository root.
Upload the **contents** of this folder to GitHub (not the ZIP itself and not a parent folder).

## Root layout

- `gradlew`, `settings.gradle.kts`, `build.gradle.kts`, `gradle/`
- `app/` Android application source and resources
- `.github/workflows/android.yml` GitHub Actions build workflow
- `docs/xray-profiles/` example Xray JSON templates

## GitHub Actions

Push the files to `main` or `master`, then open the Actions tab. The workflow checks
that Gradle files exist at repository root, downloads `libv2ray.aar` from the Onering
repository into `app/libs/`, and runs `assembleDebug`. You can also start it manually
with `workflow_dispatch`.

## Xray profile import

The profile URI codec recognizes VMess, VLESS, Trojan, Shadowsocks, SOCKS, HTTP(S),
Xray JSON and the app's own `sp://` profile export. In the Profiles screen use Import
and select a text file with one link or one Xray JSON configuration. See
`docs/xray-profiles/README.md` for examples and the Onering SNI convention.

## Important integration status

The project includes the Xray profile data model, persistence fields, URI/JSON parsing,
Onering configuration helper and `libv2ray.aar` lifecycle wrapper. The existing SSH
Connect/VPN routing flow has **not yet been wired to launch Xray profiles**. Therefore
this source should be treated as an integration-development build, not a verified
Xray-capable release APK. The GitHub workflow has not been run in this environment.
