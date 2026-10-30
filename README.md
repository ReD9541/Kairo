# ⏳ Kairo

<p align="center">
  <strong>An intelligent, circadian-aware daily schedule and transit orchestrator for Android.</strong>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android" />
  <img src="https://img.shields.io/badge/Kotlin-2.0-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin" />
  <img src="https://img.shields.io/badge/Jetpack_Compose-Material_3-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" alt="Compose" />
  <img src="https://img.shields.io/badge/Backend-Supabase-3ECF8E?style=for-the-badge&logo=supabase&logoColor=white" alt="Supabase" />
  <img src="https://img.shields.io/badge/Transit_API-TfNSW-002664?style=for-the-badge" alt="TfNSW" />
  <img src="https://img.shields.io/badge/License-MIT-blue?style=for-the-badge" alt="License" />
</p>

---

## 📖 Overview

**Kairo** bridges the gap between rigid calendar appointments and the fluid realities of human life. Traditional calendar apps tell you *when* an event is, but they ignore the essential human routines around it—transit delays, meal preparation, decompression after a long commute, and structured focus sessions.

Designed with a cosmic dark aesthetic and built 100% in **Jetpack Compose**, Kairo reads your local and remote calendar schedules, queries real-time public transit feeds (Transport for NSW), detects open time gaps, and automatically crafts a balanced, human-friendly daily schedule tailored to your circadian rhythm.

---

## ✨ Key Features

### 🧠 Circadian-Aware Routine Engine
* **Interactive Sleep Schedule Dial**: Set your ideal wake-up and bedtime hours with a circular gesture dial.
* **Morning & Evening Buffers**: Generates realistic morning routines (hygiene, breakfast, mindset) and evening wind-down blocks.
* **Meal Planning**: Automatically schedules dedicated lunch and dinner preparation & eating windows.
* **Decompression Blocks**: Automatically inserts buffer periods after commutes so you arrive home, relax, and transition smoothly.

### 🚆 Real-Time Transit & Commute Intelligence
* **Live TfNSW Integration**: Integrates directly with Transport for NSW Open Data API to compute precise travel itineraries (Train, Metro, Bus, Light Rail, and Ferry).
* **Location Overrides**: Set custom home, work, and campus/college locations to ensure accurate trip routing.
* **Dynamic Commute Legs**: Inspect step-by-step transit legs, departure platforms, and transfer windows.
* **Mode Filtering**: Filter transit preferences (e.g. *All Public Transport*, *Train Only*, *Bus Only*).

### 📅 Multi-Source Calendar Synchronization
* **Local Android Calendar**: Reads device calendars with runtime permission handling.
* **Remote iCal (.ics) Feeds**: Seamlessly parse work, university/college, and personal subscription feeds.
* **RFC 5545 Compliance**: Full line unfolding, timezone normalization, and recurring event (`RRULE`) expansion.
* **Smart Event Filtering**: Intelligently separates deadlines and assignments from fixed time commitments.

### ⏱️ Smart Focus & Study Blocks
* **Schedule Gap Detection**: Analyzes schedule intervals and converts free time into structured deep-work blocks.
* **Built-in Pomodoro Rhythm**: Configurable focus sessions (default: 90 mins) and micro-breaks (default: 15 mins).
* **Study Prep Buffers**: Adds quick setup periods before deep work sessions to ensure mental readiness.

### ⚡ Real-Life Agility
* **Quick Meal Mode**: Shorten meal prep time when you're in a rush to save schedule space.
* **Running Late Offsets**: Running behind schedule? Shift future routine activities forward with a single tap without scrambling your day.

### ☁️ Cloud Sync & Account Management
* **Supabase Authentication**: Secure user sign-up and sign-in.
* **Settings & Routine Cloud Backup**: Synchronize routine lengths, transit configurations, and milestone progress across your devices.

---

## 🏛️ Architecture & Tech Stack

Kairo is built following **Clean Architecture** principles and the modern **MVI/MVVM** reactive UI pattern:

```
┌────────────────────────────────────────────────────────┐
│                   Presentation Layer                   │
│       Jetpack Compose (Material 3) + StateFlow         │
│  DashboardScreen | EventDetail | Settings | Milestone  │
└───────────────────────────┬────────────────────────────┘
                            │
┌───────────────────────────▼────────────────────────────┐
│                       Domain Layer                     │
│      Use Cases + Domain Models + Repository Interfaces │
│  CalculateCommuteUseCase | FindScheduleGapsUseCase     │
└───────────────────────────┬────────────────────────────┘
                            │
┌───────────────────────────▼────────────────────────────┐
│                        Data Layer                      │
│   DataStore | Supabase Client | Retrofit (TfNSW API)   │
│   CalendarProviderDataSource | ICalDataSource (RRULE)  │
└────────────────────────────────────────────────────────┘
```

### Core Technologies

| Technology | Purpose |
| :--- | :--- |
| **Kotlin 2.0** | Primary language, modern coroutines, and type-safe serialization |
| **Jetpack Compose (Material 3)** | Declarative, dynamic UI with custom canvas animations |
| **Kotlin Coroutines & Flow** | Asynchronous operations and reactive data streams |
| **Navigation 3 / Compose Nav** | Flexible, type-safe screen navigation |
| **DataStore Preferences** | Local reactive storage for user configurations and preferences |
| **Retrofit 2 & OkHttp** | REST API consumption with Moshi JSON code generation |
| **Transport for NSW (TfNSW) API** | Real-time transit journey planning and public transport routing |
| **Supabase (Auth & Postgrest)** | Cloud authentication, remote user settings, and profile synchronization |
| **Room Database** | Offline caching and persistent storage |
| **Accompanist Permissions** | Seamless runtime permission requests for calendar access |

---

## 🚀 Getting Started

### Prerequisites
* **Android Studio**: Ladybug (2024.2.1) or newer
* **JDK**: Version 11 or 17
* **Android SDK**: Target API 37 (Minimum API 24)

### 1. Clone the Repository
```bash
git clone https://github.com/ReD9541/Kairo.git
cd Kairo
```

### 2. Configure Local Properties
Create or edit `local.properties` in the project root to configure your API keys:

```properties
# Android SDK location
sdk.dir=/path/to/your/Android/sdk

# Transport for NSW API Key (Sign up at https://opendata.transport.nsw.gov.au/)
TFNSW_API_KEY=your_tfnsw_api_key_here

# Supabase Configuration
SUPABASE_URL=https://your-project-id.supabase.co
SUPABASE_ANON_KEY=your_supabase_anon_key_here
```

> [!NOTE]
> If `TFNSW_API_KEY` is left blank, Kairo will use fallback transit calculations without failing.

### 3. Build & Run
Open the project in Android Studio or compile using Gradle:

```bash
# Build debug APK
./gradlew assembleDebug

# Run unit tests
./gradlew test
```

---

## 🌐 Deploying to GitHub Pages

You can host this documentation directly on **GitHub Pages**:

1. Go to your repository on GitHub: `https://github.com/ReD9541/Kairo`.
2. Navigate to **Settings** > **Pages** (under "Code and automation").
3. Under **Build and deployment**:
   * **Source**: Select `Deploy from a branch`.
   * **Branch**: Choose `master` (or `main`) and set the folder to `/ (root)`.
4. Click **Save**.
5. Your GitHub Pages site will be live within minutes at:
   ```
   https://<your-username>.github.io/Kairo/
   ```

*(Optional)* To apply a modern Jekyll theme (like Cayman or Slate), you can add a `_config.yml` file to the root of the repository:
```yaml
theme: jekyll-theme-cayman
title: Kairo
description: Intelligent Circadian Routine & Transit Scheduler for Android
```

---

## 🔒 Permissions & Security

Kairo requires minimal permissions strictly necessary for its features:
* `android.permission.INTERNET`: Required for fetching real-time TfNSW transit schedules and syncing with Supabase.
* `android.permission.READ_CALENDAR`: Optional runtime permission used only to read your local calendar events so Kairo can build your schedule around them.

Your calendar data is processed locally on-device and is never shared with third parties.

---

## 🤝 Contributing

Contributions, feedback, and ideas are welcome!
1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

## 📄 License

Distributed under the MIT License. See `LICENSE` for more information.

---

<p align="center">
  Crafted with care for balanced living & productive flow.
</p>
