# DrawRun 🏃💨

**L'Art de Courir & de Nager.**

DrawRun est une application Android moderne pour analyser vos performances sportives avec une précision scientifique. Elle combine les données de **Strava** et **Google Health Connect** pour offrir des métriques avancées introuvables ailleurs.

## Fonctionnalités
-   **Analyse Multi-Sport** : Running & Natation.
-   **Nombre d'Eddington** : Calculez votre score légendaire.
-   **Advanced Metrics** : Analyse des données brutes (Allure, Puissance, Fréquence Cardiaque).
-   **AI Coach** : Conseils personnalisés basés sur votre charge (Fitness/Fatigue).
-   **Design Premium** : Interface fluide et moderne inspirée du Glassmorphism.

## Site Web & Confidentialité
Le site web officiel (Landing Page, Politique de Confidentialité) est hébergé dans ce dépôt sous le dossier `/website`.
[Voir le site](https://swal-l.github.io/DrawRun/website/) *(Une fois GitHub Pages activé)*

## Installation (Développement)
1.  Cloner le dépôt.
2.  Ajouter votre fichier `local.properties` à la racine avec vos clés API :
    ```properties
    STRAVA_CLIENT_ID=votre_id
    STRAVA_CLIENT_SECRET=votre_secret
    ```
3.  Ouvrir avec Android Studio et synchroniser Gradle.

## Tech Stack
-   **Langage** : Kotlin
-   **UI** : Jetpack Compose
-   **Architecture** : MVVM
-   **API** : Strava API (OAuth 2.0 via Deep Link), Google Health Connect
-   **Build** : Gradle KTS

## Auteur
Développé par [swal-l](https://github.com/swal-l).
