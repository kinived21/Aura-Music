# Aura Music Player - High-Fidelity Audio Experience 🎵

![Kotlin](https://img.shields.io/badge/kotlin-%237F52FF.svg?style=for-the-badge&logo=kotlin&logoColor=white)
![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Firebase](https://img.shields.io/badge/Firebase-039BE5?style=for-the-badge&logo=Firebase&logoColor=white)
![Gemini](https://img.shields.io/badge/Gemini-8E75C2?style=for-the-badge&logo=googlegemini&logoColor=white)

Aura Music Player is a robust Android application built with **Kotlin** and **Firebase** that delivers a high-quality music streaming experience. It features background playback, real-time data fetching, and smart notification controls.

---

## 🤖 AI Collaboration
This project was developed with the strategic assistance of **Gemini AI**. By utilizing AI tools, I was able to:
* **Architecture Design:** Built a stable Foreground Service for background audio that survives system resource management.
* **Modern API Support:** Solved complex security requirements for **Android 14+ (API 34/36)**, including `RECEIVER_EXPORTED` flags and exact `PendingIntent` mutability.
* **Logic Optimization:** Refined the `MediaPlayer` state machine to ensure zero-gap transitions between songs.
* **Debugging:** Resolved lifecycle issues where music would continue playing after the app was swiped away.

## ✨ Features
* **Firebase Integration:** Fetches song metadata and stream URLs dynamically from **Firebase Realtime Database/Firestore**.
* **Background Playback:** Uses a **Foreground Service** to keep the music playing even while using other apps.
* **Smart Notification Bar:** Custom MediaStyle notifications with Play/Pause, Next, and Previous controls.
* **Auto-Play Logic:** Intelligent playlist management that automatically starts the next track upon song completion.
* **Dynamic UI:** Real-time SeekBar updates, dynamic album art loading via Picasso, and smooth transitions.
* **Clean Exit:** Optimized to stop all audio and remove system notifications instantly when the task is removed.

## 🛠️ Tech Stack
* **Language:** Kotlin
* **Backend:** Firebase (Realtime Database & Storage)
* **Media Core:** Android MediaPlayer API
* **Components:** Services, BroadcastReceivers, MediaSessionCompat
* **Image Loading:** Picasso
* **UI:** Material Design 3, ViewBinding

---

## 📸 Preview

> **Note:** Install APP HERE-->
> [Download the Latest APK](https://drive.google.com/file/d/1YlodCJAfnKviOEYibz_oNaywRTkusWrC/view?usp=drive_link)
>
> **Note:** APP Video-->
> [App video](https://drive.google.com/file/d/1GUYQ98RlGwsdtHrUQ1mQKG-3YK97tV_-/view?usp=drive_link)

---

## 📸 Screenshots

### 🎵 Player Experience


| SignUp Screen | Home / Playlist | Player Screen | Notification Controls |
| :---: | :---: | :---: | :---: |
| ![Signup](https://github.com/user-attachments/assets/2011f7c2-d5e9-4787-9fe7-f8c445518491) | ![Home](https://github.com/user-attachments/assets/dcea876e-b2db-4d76-97ff-a203d25e2c3b) | ![Player](https://github.com/user-attachments/assets/c09910be-8412-4591-b62e-a231c258f698) | ![Notif](https://github.com/user-attachments/assets/8950a620-9c91-4c20-a9e3-88fc20def7e7) |

---

## 🧠 Technical Challenges & Solutions



* **Firebase Async Loading:** Implemented listener-based logic to ensure the UI only populates once the song data is successfully retrieved from Firebase.
* **API 36 Compatibility:** Addressed the strict Android 14+ requirement for explicit broadcast receiver flags (`RECEIVER_EXPORTED`) and background service type declarations (`mediaPlayback`).
* **Task Cleanup:** Overrode `onTaskRemoved` in the Service layer to ensure that swiping the app away clears the system audio buffer and notification tray.

## 🚀 How to Run
1. Clone this repository.
2. Add your `google-services.json` file to the `app/` folder.
3. Open the project in **Android Studio (Ladybug or later)**.
4. Build and run the `app` module on an **API 31+** device.

---
Developed by **Ved Narayan Kini**
