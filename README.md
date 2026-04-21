<div align="center">
  <img width="100%" src="https://capsule-render.vercel.app/api?type=waving&color=7C4DFF,4F8EF7&height=140&section=header&text=PeerLearn&fontSize=52&fontColor=ffffff&fontAlignY=40&animation=fadeIn&desc=Where%20Developers%20Teach%20%26%20Learn&descAlignY=62&descSize=18"/>
</div>

<div align="center">

[![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack_Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Firebase](https://img.shields.io/badge/Firebase-FFCA28?style=for-the-badge&logo=firebase&logoColor=black)](https://firebase.google.com)
[![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://android.com)

<br/>

![Version](https://img.shields.io/badge/version-0.2.0-7C4DFF?style=flat-square)
![Build](https://img.shields.io/badge/build-passing-00C853?style=flat-square)
![Platform](https://img.shields.io/badge/platform-Android%207.1+-blue?style=flat-square)
![Made with ❤️](https://img.shields.io/badge/made%20with-❤️%20in%20Nagpur-FF4081?style=flat-square)

</div>

<br/>

> **PeerLearn** is a peer-to-peer skill exchange platform for developers — where you teach what you know, and learn what you don't. No courses. No lectures. Just developers helping developers. 🤝

<br/>

---

## ✨ Features

<table>
<tr>
<td width="50%">

### 🔐 Auth
- Email sign-in + verification
- Auto profile setup flow
- Secure session management

### 👤 Profile
- Custom avatar photo (Cloudinary)
- Skills you **know** + want to **learn**
- GitHub & LinkedIn links

### 🤝 Peer Matching
- Smart skill-based matching
- Match percentage score
- Connect / Accept / Decline

</td>
<td width="50%">

### 💬 Real-time Chat
- Instant messaging
- **Code snippets** with syntax highlight
- Image sharing
- Typing indicators

### 🔔 Notifications
- Connection request alerts
- Real-time push (OneSignal)
- In-app notification center

### 📖 Study Sessions
- Session timer
- Topic tracking
- Pause / Resume / End

</td>
</tr>
</table>

---

## 🚀 Progress

```
Phase 01  ████████████████████  100%  ✅  Auth + Login UI
Phase 02  ████████████████████  100%  ✅  Profile + Photo Upload
Phase 03  ████████████████████  100%  ✅  Peer Matching + Connect
Phase 04  ████████████████████  100%  ✅  Real-time Chat + Code Share
Phase 05  ████████████████████  100%  ✅  Push Notifications
Phase 06  ████████░░░░░░░░░░░░   40%  🔨  Study Sessions
Phase 07  ░░░░░░░░░░░░░░░░░░░░    0%  🎯  Play Store Launch
```

---

## 🏗️ Tech Stack

```kotlin
object TechStack {
    val language    = "Kotlin 100%"
    val ui          = "Jetpack Compose + Material 3"
    val auth        = "Firebase Authentication"
    val database    = "Cloud Firestore (real-time)"
    val storage     = "Cloudinary"
    val messaging   = "OneSignal Push Notifications"
    val pattern     = "MVVM + Clean Architecture"
    val minSdk      = "API 25 (Android 7.1+)"
}
```

---

## 📱 Screens

```
🔐 Login          →  Clean dark auth UI
📋 Skill Setup    →  Onboarding with skill selection
🏠 Home           →  Feed + peer discovery
🔍 Match          →  Skill-matched peer cards
👤 Profile        →  Full dev profile with social links
✏️  Edit Profile   →  Update skills, bio, photo
🔔 Notifications  →  Connection requests & alerts
👥 Peer Profile   →  View any peer's profile + connect
💬 Chat           →  Real-time chat with code sharing
```

---

## ⚡ Run Locally

```bash
# 1. Clone the repo
git clone https://github.com/SAHILMASKE79/PeerLearn.git

# 2. Open in Android Studio

# 3. Add your config files
app/google-services.json        <- Firebase config
local.properties                <- Cloudinary keys

# 4. Hit Run ▶️
```

> ⚠️ `google-services.json` and `local.properties` are gitignored for security.

---

## 📁 Project Structure

```
app/src/main/java/com/sahil/peerlearn/
 ┣ 📂 ui/theme/          →  Dark space theme + colors
 ┣ 📄 MainActivity.kt    →  Nav host + auth flow
 ┣ 📄 UserRepository.kt  →  All Firestore operations
 ┣ 📄 ChatScreen.kt      →  Real-time chat UI
 ┣ 📄 MatchScreen.kt     →  Peer discovery
 ┣ 📄 ProfileScreen.kt   →  User profile
 ┗ 📄 ...and more
```

---

<div align="center">

### 🧑‍💻 Built by

```
╔══════════════════════════════════════════╗
║                                          ║
║     SAHIL MASKE                          ║
║     Android Developer · Nagpur 🇮🇳       ║
║     Self-taught · Self-driven 💪         ║
║                                          ║
║     "jo khud se karte hain —            ║
║      woh alag hote hain."               ║
║                                          ║
╚══════════════════════════════════════════╝
```

[![GitHub](https://img.shields.io/badge/GitHub-SAHILMASKE79-181717?style=for-the-badge&logo=github)](https://github.com/SAHILMASKE79)

<br/>

⭐ **Star this repo if you find it useful!**

<img width="100%" src="https://capsule-render.vercel.app/api?type=waving&color=4F8EF7,7C4DFF&height=100&section=footer"/>

</div>
