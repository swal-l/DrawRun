# DrawRun 🏃💨

**L'Art de Courir & de Nager.**

DrawRun est une application Android moderne pour analyser vos performances sportives avec une précision scientifique. Elle utilise **Google Health Connect** pour synchroniser automatiquement vos données depuis Garmin, Strava, et toutes vos applications de sport préférées.

## ✨ Fonctionnalités

### Analyse Multi-Sport
- 🏃 **Running** : Allure, zones cardiaques, puissance, cadence, **fréquence respiratoire**
- 🏊 **Natation** : SWOLF, nombre de mouvements, analyse par longueur
- 📊 **Métriques Avancées** : PMC, Eddington, **Répartition Globale des Zones**, Dérive Cardiaque

### Intelligence Artificielle & Automatisation
- 🤖 **AI Coach** : Détection automatique du type de séance (Fractionné, Seuil, Endurance)
- ❤️ **RHR Auto** : Synchronisation et lissage automatique de la fréquence cardiaque au repos
- 📈 **Analyse Prédictive** : Prévisions de performance sur 5k, 10k, Semi, Marathon

### Synchronisation Universelle
- ❤️ **Health Connect** : Synchronisation automatique avec toutes vos apps
  - Garmin Connect
  - Strava
  - Polar Flow
  - Suunto App
  - Et bien d'autres...

### Design Premium
- 🎨 Interface fluide et moderne
- 🌙 Mode sombre élégant
- 📱 Optimisé pour Android

## 🚀 Installation

### Pour les Utilisateurs
1. Téléchargez l'APK depuis [Releases](https://github.com/swal-l/DrawRun/releases)
2. Installez l'application
3. Autorisez Health Connect lors du premier lancement
4. Vos activités se synchronisent automatiquement!

### Pour les Développeurs
1. Cloner le dépôt
   ```bash
   git clone https://github.com/swal-l/DrawRun.git
   cd DrawRun
   ```

2. Ouvrir avec Android Studio

3. Synchroniser Gradle et compiler
   ```bash
   ./gradlew assembleRelease
   ```

Voir [README-BUILD.md](README-BUILD.md) pour plus de détails.

## 📱 Prérequis

- Android 9.0 (API 28) ou supérieur
- **Health Connect** installé (disponible sur le Play Store)
- Au moins une application de sport (Garmin Connect, Strava, etc.)

## 🔐 Confidentialité

DrawRun respecte votre vie privée:
- ✅ Toutes les données restent sur votre appareil
- ✅ Aucune collecte de données personnelles
- ✅ Aucun serveur externe
- ✅ Code source ouvert

Voir notre [Politique de Confidentialité](https://swal-l.github.io/DrawRun/)

## 📚 Documentation

- [Guide de Build](README-BUILD.md) - Comment compiler l'application
- [Guide Health Connect](HEALTH-CONNECT.md) - Configuration et utilisation
- [Optimisations](OPTIMISATIONS.md) - Détails techniques des optimisations
- [Release Notes Auto](RELEASE-NOTES-AUTO.md) - Génération automatique des notes

## 🛠️ Tech Stack

- **Langage** : Kotlin
- **UI** : Jetpack Compose
- **Architecture** : MVVM
- **Données** : Google Health Connect API
- **Build** : Gradle KTS
- **Graphiques** : Vico Charts

## 🤝 Contribution

Les contributions sont les bienvenues! N'hésitez pas à:
- Signaler des bugs via [Issues](https://github.com/swal-l/DrawRun/issues)
- Proposer des améliorations
- Soumettre des Pull Requests

## 📄 Licence

Ce projet est sous licence MIT. Voir [LICENSE](LICENSE) pour plus de détails.

## 👨‍💻 Auteur

Développé avec ❤️ par [swal-l](https://github.com/swal-l)

---

**Note:** DrawRun utilise exclusivement Health Connect pour la synchronisation des données. Assurez-vous que vos applications de sport (Garmin, Strava, etc.) sont configurées pour partager leurs données avec Health Connect.
