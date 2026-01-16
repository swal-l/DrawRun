# DrawRun Pro

> Plateforme d'analyse sportive moderne avec architecture Clean et design premium

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.21-blue.svg)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Compose-1.5.4-green.svg)](https://developer.android.com/jetpack/compose)
[![Architecture](https://img.shields.io/badge/Architecture-Clean-orange.svg)](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
[![License](https://img.shields.io/badge/License-MIT-red.svg)](LICENSE)

---

## 📱 À propos

**DrawRun** est une application Android d'analyse sportive de nouvelle génération, conçue avec une architecture Clean et un design system premium inspiré de Linear et Apple.

### ✨ Caractéristiques

- 📊 **Analytics avancés** : Graphiques interactifs avec Canvas, insights IA
- 🎨 **Design system** : 66 couleurs sémantiques, typography tabular, Material3
- 🔄 **Migration sécurisée** : Système de migration JSON → Room avec progress tracking
- 🏗️ **Architecture Clean** : Domain → Data → Presentation (MVVM + Hilt)
- ⚡ **Performance** : Sampling intelligent, lazy loading, animations spring

---

## 🏗️ Architecture

### Clean Architecture (3 couches)

```
┌─────────────────────────────────────┐
│         PRESENTATION                │
│  ViewModels + Screens + Components  │
│  (Jetpack Compose + Material3)      │
└─────────────┬───────────────────────┘
              │ StateFlow
┌─────────────▼───────────────────────┐
│           DOMAIN                    │
│  Models + Repositories (Interfaces) │
│  Business Logic (Pure Kotlin)       │
└─────────────┬───────────────────────┘
              │ Repository Pattern
┌─────────────▼───────────────────────┐
│            DATA                     │
│  Room Database + DAOs + Entities    │
│  Mappers + Migration                │
└─────────────────────────────────────┘
```

### Dépendances

- **Domain** : Aucune dépendance externe (Pure Kotlin)
- **Data** : Dépend de Domain (implémente les repositories)
- **Presentation** : Dépend de Domain (utilise les repositories via Hilt)

**Règle d'or** : Le flux de dépendances va toujours vers Domain (jamais l'inverse)

---

## 🛠️ Stack Technique

| Composant | Technologie | Version |
|-----------|-------------|---------|
| **Language** | Kotlin | 1.9.21 |
| **UI** | Jetpack Compose | 1.5.4 |
| **Theme** | Material3 | 1.1.2 |
| **DI** | Hilt | 2.50 |
| **Database** | Room | 2.6.1 |
| **Navigation** | Navigation Compose | 2.7.6 |
| **Serialization** | Kotlinx Serialization | 1.6.0 |
| **Coroutines** | Kotlinx Coroutines | 1.7.3 |

---

## 📁 Structure du Projet

```
app/src/main/java/com/orbital/run/
│
├── 📂 domain/
│   ├── models/              # Activity, TrainingPlan, Workout
│   ├── repositories/        # Interfaces (ActivityRepository)
│   └── calculations/        # Pure functions (pace, speed)
│
├── 📂 data/
│   ├── local/
│   │   ├── database/        # DrawRunDatabase, TypeConverters
│   │   ├── entities/        # ActivityEntity (Room @Entity)
│   │   └── dao/             # ActivityDao (Room @Dao)
│   ├── mappers/             # Entity ↔ Domain mappers
│   └── migration/           # LegacyJsonMigrator (JSON → Room)
│
├── 📂 presentation/
│   ├── components/          # StatCard, DrawRunButton, Charts
│   ├── navigation/          # NavGraph (type-safe routes)
│   └── screens/
│       ├── home/            # HomeViewModel + HomeScreen
│       ├── analytics/       # AnalyticsViewModel + AnalyticsScreen
│       ├── history/         # HistoryViewModel + HistoryScreen
│       └── settings/        # SettingsViewModel + SettingsScreen
│
└── 📂 ui/
    ├── theme/               # Color, Type, Theme (Design System)
    └── MainScreen.kt        # Entry point + Bottom Navigation
```

---

## 🎨 Design System

### Couleurs Sémantiques

**66 couleurs** avec support Light/Dark mode :

```kotlin
// Light Mode
Background       = #FBFCFE  // Off-white
Primary          = #0066FF  // Apple blue
Accent           = #00D9A0  // Teal vibrant

// Dark Mode (Linear-inspired)
Background       = #0F1419  // Deep gray
Primary          = #4D9FFF  // Lighter blue
```

### Typography avec Tabular Numbers

```kotlin
DataLarge = TextStyle(
    fontSize = 40.sp,
    fontWeight = FontWeight.Bold,
    fontFeatureSettings = "tnum"  // Monospace numbers
)
```

**Avantage** : Alignement vertical parfait dans les tableaux de données.

---

## 🔄 Migration Système

### Flux

```
JSON Legacy (5MB, 5000 activités)
        ↓
LegacyJsonMigrator (chunked processing)
        ↓
ActivityEntity (Room)
        ↓
SQLite Database
```

### Fonctionnalités

- ✅ **Validation** : Vérification JSON
- ✅ **Progress tracking** : `StateFlow` pour UI
- ✅ **Chunked processing** : 500 activités/batch
- ✅ **Transactions** : Atomicité garantie
- ✅ **Backup** : Copie avant migration
- ✅ **Rollback** : Restauration si erreur

### Performance

- **5MB JSON** : ~5.5 secondes
- **10,000 activités** : ~11 secondes
- **Memory** : <50MB peak

---

## 🚀 Installation

### Prérequis

- Android Studio Hedgehog (2023.1.1+)
- JDK 17
- Android SDK 34
- Gradle 8.2+

### Étapes

1. **Cloner**
   ```bash
   git clone https://github.com/swal-l/DrawRun.git
   cd DrawRun
   ```

2. **Build**
   ```bash
   ./gradlew build
   ```

3. **Run**
   - Connecter appareil/émulateur
   - Cliquer "Run" (▶️)

---

## 🧪 Tests

### Tests Unitaires

```bash
./gradlew test
```

**Couverture** :
- ✅ Domain models
- ✅ Calculations (pace, speed)
- ✅ ViewModels
- ✅ Mappers

---

## 📊 Écrans Principaux

### 🏠 Home
- Weekly stats : Distance, Durée, Activités
- Recent activities (last 5)
- Migration progress

### 📊 Analytics
- Time range filters (7D/30D/12M/All)
- Interactive Canvas chart
- AI insights

### 📜 History
- Sticky month headers
- Swipe-to-delete
- Sport filters

### ⚙️ Settings
- Dark mode toggle
- Strava sync
- Export CSV

---

## 🤝 Contribution

1. Fork le projet
2. Créer une branche (`git checkout -b feature/Feature`)
3. Commit (`git commit -m 'Add Feature'`)
4. Push (`git push origin feature/Feature`)
5. Ouvrir une Pull Request

---

## 📝 License

MIT License - Voir [LICENSE](LICENSE)

---

## 👨‍💻 Auteur

**Lomic** - [@swal-l](https://github.com/swal-l)

---

## 🙏 Remerciements

- **Linear** : Design system inspiration
- **Apple** : Visual hierarchy
- **Vercel** : Analytics clarity
- **Uncle Bob** : Clean Architecture

---

**DrawRun Pro** - Sport analytics, reimagined.
