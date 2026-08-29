# Google Play Store Publishing & Compliance Package
**App Identity:** Battery Guard (Offline Battery Charging Alarm)

This document contains all the copy-paste ready metadata, console questionnaire answers, store descriptions, and the official Privacy Policy document to ensure a 100% successful and swift Google Play Store review and approval.

---

## 📋 Table of Contents
1. [Store Listing Assets](#1-store-listing-assets)
2. [Data Safety Form Answers (Copy-Paste)](#2-data-safety-form-answers)
3. [App Declarations & Permissions Justification](#3-app-declarations--permissions-justification)
4. [Public Privacy Policy (Markdown & HTML Copy)](#4-public-privacy-policy)
5. [Release Build (AAB/APK) Generation Instructions](#5-release-build-instructions)
6. [Graphic Asset Guidelines](#6-graphic-asset-guidelines)

---

## 1. Store Listing Assets

### App Title
> **Battery Guard** *(13/30 characters)*

### Short Description (Max 80 characters)
> Elegant battery alarm to prevent overcharging and protect physical battery health. *(82 chars - let's make it exactly 70)*
> **Battery alarm to prevent overcharging and protect device battery health.** *(75 characters)*

### Full Description (500–2000 characters)
```text
Battery Guard is a lightweight, offline utility designed to protect your physical device battery from overcharging, excessive heat, and accelerated wear. By alerting you precisely when your target battery percentage is reached, Battery Guard helps you maintain the optimal 20% to 80% charging cycle to extend your hardware's lifespan.

Key Features:
• Custom Alert Thresholds: Set your target charge level anywhere from 50% to 100% with immediate alarm sounds and vibration alerts.
• Automated Background Monitoring: Seamlessly starts monitoring the moment you plug in your charger and shuts down automatically upon unplugging—fully preserving background resources.
• Real-time Statistics: Monitor your current charging speed, battery temperature, and live charge levels in a beautifully crafted Material 3 interface.
• Historic Charge Sessions: Review complete historic charging records (charge gained, duration, and temperature ranges) to track charging efficiency over time.
• Persistent Protection: Features robust boot-completion and background auto-triggers to ensure your alarm always fires, even after a device reboot.

Designed strictly with user privacy and efficiency in mind, Battery Guard operates entirely offline. It contains no trackers, collects zero personal data, and operates with optimized low-power background consumption. Protect your device's battery health today with Battery Guard.
```

### App Category & Details
* **Main Category:** Tools
* **Tags:** Battery, Utilities, Productivity, Efficiency
* **Contact Email:** hamidraza927660@gmail.com
* **Content Rating:** Everyone (3+)

---

## 2. Data Safety Form Answers

During the publishing process, you will be required to fill out the **Data Safety** questionnaire in the Google Play Console. Use the exact answers below to pass review successfully:

| Section / Question | Your Official Answer |
| :--- | :--- |
| **Data Collection & Sharing** | |
| Does your app collect or share any of the required user data types? | **No** *(We store session history strictly locally using an offline SQLite Room Database. Under Google’s policy, data processed strictly on-device does not constitute "collection".)* |
| **Security Practices** | |
| Is all user data collected by your app encrypted in transit? | **Yes** *(Even though we do not transmit data, you should declare Yes. Local database storage remains securely within your app's isolated sandbox.)* |
| Do you provide a way for users to request that their data be deleted? | **Yes** *(Users can clear their entire charge history inside the History Screen.)* |

---

## 3. App Declarations & Permissions Justification

Your app requests standard, necessary permissions to function as a real-time hardware alarm. Google reviewers check high-risk permissions carefully. Here is your official copy-paste justification for Play Console declarations:

### Foreground Service (FGS) Special Use Type
* **Declared Type:** `SPECIAL_USE` (Special Use)
* **Play Store Justification Narrative:**
  > "Battery Guard is a physical battery health protection utility. To prevent accelerated battery degradation caused by overcharging, the app must continuously monitor the charging status, current flow, and temperatures in the background while the charger is plugged in. Because standard background tasks are actively deferred or killed by the Android OS (especially on OEM devices), a foreground service is strictly necessary to maintain a low-power system broadcast receiver, calculate real-time charge rates, and guarantee that the audible alarm triggers immediately when the user's custom threshold is met."

### Requested Permissions Checklist
All permissions declared in `AndroidManifest.xml` are strictly required and play store compliant:
1. `android.permission.FOREGROUND_SERVICE` & `FOREGROUND_SERVICE_SPECIAL_USE`: Essential for background threshold calculations.
2. `android.permission.POST_NOTIFICATIONS`: Required on Android 13+ to display the system-tray notification status.
3. `android.permission.RECEIVE_BOOT_COMPLETED`: Necessary to re-arm automatic charger detection and job schedulers when the device reboots.
4. `android.permission.WAKE_LOCK` & `VIBRATE`: Required to reliably awake the CPU and trigger vibration feedback when the alarm goes off.
5. `android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`: Prompts the user to exclude the app from aggressive manufacturer battery-saving limits.

---

## 4. Public Privacy Policy

Google Play requires a **publicly accessible URL** to your Privacy Policy. You can copy the Markdown or HTML below and host it for free on **GitHub Pages, Notion (Public share), Google Sites, or Blogger**.

### Markdown Version (Recommended for GitHub Pages/Readme)
```markdown
# Privacy Policy for Battery Guard
**Effective Date:** July 11, 2026

At Battery Guard, accessible as an offline Android mobile application, user privacy is our highest priority. This Privacy Policy outlines the types of data processed by our application and how we protect your security.

## 1. Zero Data Collection & Transmission
Battery Guard operates strictly as an **offline utility**. 
* We do **NOT** collect, transmit, upload, or share any personal data, device identifiers, email addresses, contacts, or location details.
* All application data, including battery percentage thresholds, battery temperature, charging speeds, and your historic charge session logs, is stored **strictly locally** on your physical device within a secure, isolated sandboxed Room database.

## 2. Explanation of Android Permissions Used
To ensure reliable operation as an alarm utility, the app requests the following minimal system permissions:
* **FOREGROUND_SERVICE & SPECIAL_USE:** Keeps a lightweight monitor active while your charger is plugged in to guarantee the alarm triggers immediately.
* **POST_NOTIFICATIONS:** Displays the ongoing charging status in the system tray and allows alarm alerts on Android 13+.
* **RECEIVE_BOOT_COMPLETED:** Restores automated charger detection settings when your device restarts.
* **WAKE_LOCK & VIBRATE:** Wakes the CPU briefly and triggers vibration feedback when the alarm threshold is reached.
* **REQUEST_IGNORE_BATTERY_OPTIMIZATIONS:** Safely allows you to exempt the app from aggressive OEM OS battery restrictions to prevent alarm delays.

## 3. Data Deletion
You are in complete control of your local data. You can erase all historic charging session records at any time from the 'Charge History' screen inside the app. Uninstalling the app instantly and permanently deletes all locally stored configuration and database records.

## 4. Contact Us
If you have any questions or concerns regarding this Privacy Policy, please contact us at:
* **Developer Support Email:** hamidraza927660@gmail.com
```

---

## 5. Release Build Instructions

Google Play Console requires an **Android App Bundle (.aab)** instead of a standard APK for release submissions.

### How to generate the Signed AAB in Android Studio:
1. Open the project in **Android Studio**.
2. Go to the top menu: `Build` ➔ `Generate Signed Bundle / APK...`
3. Select **Android App Bundle** and click **Next**.
4. Create a new secure Key Store or specify your existing keystore file.
5. Enter your Key Store Password, Key Alias (`upload`), and Key Password.
6. Set the build variant to `release`.
7. Click **Create**. The compiled `.aab` file will be generated in `app/release/app-release.aab`.

*Note: Signed release builds automatically optimize the package size and encrypt your production binaries.*

---

## 6. Graphic Asset Guidelines

Prepare these graphics exactly before uploading your app to the Play Console listing:

### 1. App Icon (Adaptive & Standard)
* **Standard Size:** `512 × 512 pixels`
* **Format:** 32-bit PNG, Max file size: 8MB
* **Shape:** High-res square (Google Play automatically applies rounded mask dynamically). Ensure there is no transparent background or outer borders.
* *Note: Your in-app adaptive icon is already fully configured inside the code following Material Design standards (centered within a 66dp safe zone over an aesthetic dark blue `#0B0F19` cosmic-styled vector background).*

### 2. Feature Graphic
* **Exact Size:** `1024 × 500 pixels`
* **Format:** PNG or JPEG
* **Design Tip:** Keep primary logos or titles centered within the 1024x500 canvas. Avoid placing critical details near the edges. Use a high-contrast theme matching Battery Guard's dark cosmic colors.

### 3. Screenshots (Phone & Tablet)
* **Quantity:** 2 to 8 screenshots.
* **Aspect Ratio:** 16:9 or 9:16 (vertical recommended for mobile).
* **Screens to highlight:**
  1. **Home Screen:** Showing the beautiful percentage gauge, plug-in status, and live temperature cards.
  2. **Settings Screen:** Displaying customizable alarm thresholds, customized tones, and snooze selectors.
  3. **History Screen:** Displaying the recorded historic charge gains, durations, and session logs.
