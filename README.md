<div align="center">

<img src="https://img.shields.io/badge/PeerLearn-v1.0-7C4DFF?style=for-the-badge&logo=android&logoColor=white"/>

# 🧑‍💻 PeerLearn

### *Teach what you know. Learn what you don't.*

A peer-to-peer skill exchange platform for programmers — connect with developers, share knowledge, and grow together.

![Kotlin](https://img.shields.io/badge/Kotlin-7C4DFF?style=flat-square&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-1DB954?style=flat-square&logo=jetpackcompose&logoColor=white)
![Firebase](https://img.shields.io/badge/Firebase-FFCA28?style=flat-square&logo=firebase&logoColor=black)
![MVVM](https://img.shields.io/badge/Architecture-MVVM-7C4DFF?style=flat-square)
![Status](https://img.shields.io/badge/Status-Active-1DB954?style=flat-square)

</div>

---

## 📱 About

PeerLearn solves a simple problem — **every programmer knows something someone else doesn't.**

Instead of paying for courses, connect with a peer who knows what you want to learn, and teach them something in return. Skill exchange, not skill purchase.

---

## ✨ Features

| Feature | Status |
|---|---|
| 🔐 Firebase Authentication (Email/Password) | ✅ Done |
| 👤 User Profile with Skill Tags | ✅ Done |
| 🤝 Peer Matching by Overlapping Skills | ✅ Done |
| 💬 Real-time Chat | ✅ Done |
| ⌨️ Typing Indicators | ✅ Done |
| ✅ Read Receipts | ✅ Done |
| 💻 In-Chat Code Sharing | ✅ Done |
| 🔔 Push Notifications (OneSignal) | ✅ Done |
| 📁 Media via Firebase Storage | ✅ Done |
| 🌐 Session Management | 🔄 In Progress |

---

## 🛠️ Tech Stack

```
Language        → Kotlin
UI              → Jetpack Compose
Architecture    → MVVM (ViewModel + StateFlow)
Auth            → Firebase Authentication
Database        → Firebase Firestore + Realtime Database
Storage         → Firebase Cloud Storage
Notifications   → OneSignal
```

---

## 🏗️ Project Structure

```
com.sahil.peerlearn/
├── ui/
│   ├── auth/          # Login & Register screens
│   ├── profile/       # User profile & skill setup
│   ├── matching/      # Peer discovery & matching
│   └── chat/          # Real-time chat UI
├── viewmodel/         # MVVM ViewModels
├── repository/        # Firebase data layer
├── model/             # Data classes
└── utils/             # Helpers & extensions
```

---

## 🎨 Design

Dark themed UI built entirely with Jetpack Compose.

| Token | Value |
|---|---|
| Background | `#0D0D1A` |
| Surface | `#1A1A2E` |
| Primary | `#7C4DFF` |
| Accent | `#1DB954` |

---

## 🚀 Getting Started

```bash
# 1. Clone the repo
git clone https://github.com/SAHILMASKE79/PeerLearn.git

# 2. Open in Android Studio

# 3. Add your google-services.json from Firebase Console
#    Place it in: app/google-services.json

# 4. Add your OneSignal App ID in strings.xml

# 5. Run on emulator or device (API 26+)
```

---

## 📸 Screenshots

> Coming soon

---

## 👨‍💻 Developer

**Sahil Maske**
Diploma CS Student · Android Developer · Nagpur, India

[![GitHub](https://img.shields.io/badge/GitHub-SAHILMASKE79-181717?style=flat-square&logo=github)](https://github.com/SAHILMASKE79)
[![Email](https://img.shields.io/badge/Email-sahilmaske79%40gmail.com-D14836?style=flat-square&logo=gmail&logoColor=white)](mailto:sahilmaske79@gmail.com)

---

<div align="center">
<sub>Built with 💜 using Kotlin & Jetpack Compose</sub>
</div>
