# Xray profile import templates

These are **templates only**. Replace every `example.invalid`, UUID, and password
with values supplied by your own server. They are not working credentials.

The app's profile importer recognizes:

- `vmess://` (base64 VMess share links)
- `vless://`
- `trojan://`
- `ss://`
- `socks://` / `socks5://`
- `http://` / `https://`
- Xray JSON files containing an `outbounds` array
- SSH Protocol's own `sp://` profile export

Use the Profiles screen's Import action and select a text file containing one
share link or one Xray JSON configuration. The parser stores the imported
Xray connection fields alongside the existing SSH profile model.

## Onering

For the Onering convention documented by dharak36/xray-onering, use a TLS
`serverName` in this form:

`onering:YOUR_ORIGIN_DOMAIN:YOUR_BUG_DOMAIN`

For WebSocket transport, set the WebSocket `Host` header to the origin domain.
The Onering helper in `app/src/main/java/com/mdevz/sp/xray/OneringConfig.kt`
can apply this convention to a compatible Xray JSON configuration.

## Runtime status

This source contains the Onering AAR lifecycle wrapper and profile parsing/model
support. The current app's SSH Connect/VPN routing flow is not yet wired to start
Xray profiles. Importing and storing an Xray profile must not be mistaken for a
verified live Xray tunnel. Full runtime switching and VPN routing remain a
separate integration step.
