# PeerLearn

A peer-to-peer skill exchange platform for developers. Teach what you know. Learn what you don't.

![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=flat-square&logo=kotlin&logoColor=white)
![Compose](https://img.shields.io/badge/Jetpack_Compose-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white)
![Firebase](https://img.shields.io/badge/Firebase-FFCA28?style=flat-square&logo=firebase&logoColor=black)
![Status](https://img.shields.io/badge/Status-Active-00C853?style=flat-square)

---

## Overview

PeerLearn connects developers based on skills they have and skills they want to learn. No courses, no instructors — just developers helping each other grow.

---

## Features

| Module | Description | Status |
|--------|-------------|--------|
| Auth | Email sign-in with verification | ✅ |
| Profile | Bio, skills, photo, social links | ✅ |
| Peer Match | Skill-based matching with score | ✅ |
| Connect | Send / Accept / Decline requests | ✅ |
| Chat | Real-time messaging + code sharing | ✅ |
| Notifications | Push + in-app alerts | ✅ |
| Study Sessions | Shared timer with topic tracking | 🔨 |
| Play Store | Public release | 🎯 |

---

## Stack

```
Language     Kotlin
UI           Jetpack Compose + Material 3
Auth         Firebase Authentication
Database     Cloud Firestore
Storage      Cloudinary
Push         OneSignal
Pattern      MVVM
Min SDK      API 25 (Android 7.1+)
```

---

## Getting Started

```bash
git clone https://github.com/SAHILMASKE79/PeerLearn.git
```

1. Open in Android Studio
2. Add `app/google-services.json` from your Firebase project
3. Add Cloudinary credentials to `local.properties`
4. Run on emulator or device

---

## Project Structure

```
app/src/main/java/com/sahil/peerlearn/
├── ui/theme/            Dark space theme
├── MainActivity.kt      Navigation + auth flow
├── UserRepository.kt    Firestore operations
├── ChatScreen.kt        Real-time chat
├── MatchScreen.kt       Peer discovery
├── ProfileScreen.kt     User profile
└── ...
```

---

Built by [Sahil Maske](https://github.com/SAHILMASKE79) · Nagpur, India
