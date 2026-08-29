<div align="center">

# 🔋⚡ Battery Guard

### *The elegant charging alarm that protects your battery's lifespan.*

An offline, privacy-first Android utility that alerts you the instant your device reaches your custom charge threshold — so you never overcharge again.

[![Kotlin](https://img.shields.io/badge/Kotlin-100%25-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Min SDK](https://img.shields.io/badge/Min%20SDK-24-3DDC84?style=for-the-badge&logo=android&logoColor=white)](#)
[![License](https://img.shields.io/badge/License-MIT-yellow?style=for-the-badge)](#-license)
[![Privacy](https://img.shields.io/badge/Privacy-100%25%20Offline-000000?style=for-the-badge)](#-privacy-first-by-design)

</div>

---

## ✨ Why Battery Guard?

Modern lithium-ion batteries degrade fastest when kept at 100% charge for long periods. **Battery Guard** watches the charge level quietly in the background and sounds an alarm the moment you cross the line you set — helping you stick to the healthy **20%–80% charging window** without babysitting your phone.

No accounts. No ads. No trackers. Just a clean, dark, cosmic-themed interface doing one job extremely well.

<div align="center">

| 🎯 Custom Thresholds | 🔔 Instant Alarm | 📊 Live Stats | 🕓 Charge History | 🛡️ Fully Offline |
|:---:|:---:|:---:|:---:|:---:|
| Pick any target from 50%–100% | Sound + vibration the second you hit it | Speed, temperature & level in real time | Every charging session logged locally | Zero data ever leaves your device |

</div>

---

## 🚀 Features

- **🎚️ Custom Alert Thresholds** — Set your target battery level anywhere from 50% to 100%, and get immediate sound + vibration alerts when it's reached.
- **⚙️ Automated Background Monitoring** — A lightweight foreground service starts the instant you plug in the charger and shuts itself down the moment you unplug — no manual toggling, minimal battery/resource footprint.
- **📈 Real-Time Statistics** — Watch live charging speed, battery temperature, and charge percentage rendered in a beautifully crafted **Material 3** interface.
- **🗂️ Historic Charge Sessions** — Every session (charge gained, duration, temperature range) is stored locally so you can track your charging habits and battery health over time.
- **🔁 Persistent Protection** — Boot-completion receivers and background auto-triggers make sure the alarm reliably fires again after every device reboot.
- **🌌 Cosmic Dark UI** — A polished, high-contrast Material 3 design built entirely with Jetpack Compose.

---

## 🛠️ Tech Stack

Built as a modern, native Android application:

| Layer | Technology |
|---|---|
| **Language** | Kotlin |
| **UI Toolkit** | Jetpack Compose + Material 3 |
| **Architecture** | MVVM (ViewModel + Lifecycle + Coroutines) |
| **Local Storage** | Room (SQLite) — fully offline session history |
| **Networking** | Retrofit + Moshi + OkHttp |
| **AI Layer** | Firebase AI (Gemini API) |
| **Background Work** | Foreground Service + Boot Receiver + WakeLock |
| **Build System** | Gradle Kotlin DSL (KSP) |
| **Testing** | JUnit, Espresso, Robolectric, Roborazzi (screenshot tests) |

---

## 📱 Screenshots

<div align="center">

| Home | Live Monitoring | Charge History |
|:---:|:---:|:---:|
| *coming soon* | *coming soon* | *coming soon* |

</div>

> 💡 Drop your screenshots into `assets/` and swap the placeholders above once your first build is ready.

---

## 🧰 Getting Started

### Prerequisites

- [Android Studio](https://developer.android.com/studio) (latest stable)
- JDK 11+
- An Android device or emulator running **API 24 (Android 7.0)** or higher

### Clone & Run

```bash
git clone https://github.com/hamid647/Battery-alarm.git
cd Battery-alarm
```

1. Open the project in **Android Studio**.
2. Copy `.env.example` to `.env` and add your own key:
   ```env
   GEMINI_API_KEY=your_gemini_api_key_here
   ```
3. Let Gradle sync, then hit **Run ▶️** on your device or emulator.

### Build a Release Bundle

```bash
./gradlew bundleRelease
```

The signed `.aab` will be generated at `app/release/app-release.aab`, ready for Google Play submission.

---

## 🔐 Permissions Explained

Battery Guard requests only what it strictly needs to function as a reliable hardware alarm:

| Permission | Why it's needed |
|---|---|
| `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_SPECIAL_USE` | Keeps the low-power monitor alive while charging |
| `POST_NOTIFICATIONS` | Shows live charging status & alarm alerts (Android 13+) |
| `RECEIVE_BOOT_COMPLETED` | Re-arms monitoring after a device restart |
| `WAKE_LOCK` / `VIBRATE` | Wakes the CPU briefly and vibrates when the alarm fires |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Prevents OEM battery-savers from delaying the alarm |

---

## 🕶️ Privacy First, By Design

Battery Guard runs **entirely offline**.

- ❌ No analytics, no ads, no trackers
- ❌ No account, email, or personal data collected
- ✅ All charge history stays in a sandboxed local Room database
- ✅ Clear your entire history anytime from the **Charge History** screen
- ✅ Uninstalling the app permanently deletes all local data

---

## 🗺️ Roadmap

- [ ] Home-screen widget for at-a-glance battery status
- [ ] Low-battery alerts alongside the high-charge alarm
- [ ] Custom alarm tones & vibration patterns
- [ ] Charging session insights and trends
- [ ] Wear OS companion app

---

## 🤝 Contributing

Contributions, issues, and feature requests are welcome!

1. Fork the repo
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

---

## 📄 License

Distributed under the **MIT License**. See `LICENSE` for more information.

---

## 📬 Contact

**Hamid Raza** — [hamidraza927660@gmail.com](mailto:hamidraza927660@gmail.com)

Project Link: [github.com/hamid647/Battery-alarm](https://github.com/hamid647/Battery-alarm)

<div align="center">

Made with 🔋 and Kotlin — protect your battery, protect your device.

</div>
