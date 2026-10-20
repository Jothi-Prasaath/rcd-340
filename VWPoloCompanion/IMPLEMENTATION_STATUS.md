# Implementation status (preview)

This source preview implements the first local-first milestone:

- Editable vehicle display name, model/variant, and subtitle; saved locally.
- Dark/light themes and selectable neon accent colours; saved locally.
- GPS trip start/stop using a foreground location service and ongoing notification.
- GPS speed, approximate trip distance, elapsed trip time, and basic local trip totals/history summary.
- Launch Google Maps, Spotify (with YouTube Music web fallback), and phone dialer.
- Local storage; no account, ads, or cloud backend.

## Not yet implemented / verified

- Android Auto car-screen app integration and Play Store category approval.
- OBD-II Bluetooth adapter protocol and live ECU values.
- Spotify in-app playback/session controls and track metadata.
- Voice assistant, notification reading, WhatsApp workflows, route history map/export.
- Custom image backgrounds and configurable widget layout.
- Window control. This remains conditional on safe vehicle-specific support.
- Automated build and device testing. This source preview has not been compiled in this environment.

GPS values are estimates and must not be treated as vehicle-certified speed or engine telemetry. GPS background behaviour depends on Android permissions, device battery policy, and testing on the target phone.
