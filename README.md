# 🍽️ FoodRanker

> Discover, rate and share the best dishes around you.

FoodRanker is an Android app that lets users rate individual restaurant dishes — not just restaurants. Find the best *tortilla* in Madrid, the best *ramen* in your city, or share your own discoveries with the community.

---

## ✨ Features

- 📸 **Dish discovery** — Browse dishes by category, location or trending
- ⭐ **Rating system** — Rate dishes and see community scores
- 🔐 **Authentication** — Sign up / log in with email
- 🖼️ **Image upload** — Add photos of dishes via camera or gallery
- 👤 **User profiles** — Track your ratings and collections
- 🔔 **Push notifications** — Get notified about activity *(in progress)*
- 📈 **Trending screen** — See what's hot right now *(in progress)*

---

## 🛠️ Tech stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin |
| UI | Jetpack Compose |
| Architecture | MVVM + Clean Architecture |
| DI | Hilt |
| Backend | Firebase (Auth + Firestore) |
| Storage | Cloudinary |
| Async | Kotlin Coroutines + Flow |

---

## 🏗️ Architecture

```
app/
├── data/
│   ├── model/          # Data classes
│   └── repository/     # Repository implementations
├── ui/
│   ├── components/     # Reusable composables
│   ├── screens/        # Screen composables
│   └── theme/          # Design system
└── di/                 # Hilt modules
```

---

## 🚀 Getting started

### Prerequisites
- Android Studio Hedgehog or later
- Android SDK 26+
- A Firebase project with Authentication and Firestore enabled
- A Cloudinary account

### Setup

1. Clone the repository
   ```bash
   git clone https://github.com/sdelapenya/FoodRanker.git
   ```

2. Create a Firebase project at [console.firebase.google.com](https://console.firebase.google.com) and download `google-services.json` into the `app/` folder

3. Add your Cloudinary credentials to `local.properties`:
   ```
   CLOUDINARY_CLOUD_NAME=your_cloud_name
   CLOUDINARY_API_KEY=your_api_key
   CLOUDINARY_API_SECRET=your_api_secret
   ```

4. Build and run the project in Android Studio

---

## 📱 Screenshots

*Coming soon*

---

## 🗺️ Roadmap

- [x] Authentication
- [x] Dish CRUD
- [x] Image upload
- [x] Rating system
- [x] User profiles
- [ ] Likes system
- [ ] Push notifications
- [ ] Trending screen
- [ ] Skeleton loading states
- [ ] Splash screen

---

## 👤 Author

**Sergio de la Peña**
- Website: [sdelapenya.dev](https://sergio.sdelapenya.dev)
- LinkedIn: [sergiodelapenya](https://www.linkedin.com/in/sergiodelapenya/)
- GitHub: [@sdelapenya](https://github.com/sdelapenya)

---

*Built with ❤️ and too much coffee.*
