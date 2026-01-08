# 🚀 DrawRun v3.0 - Intégration Strava Directe & Optimisations

> **Date :** 08 Janvier 2025
> **Version Code :** 45
> **Version Name :** 3.0

Cette mise à jour majeure marque l'arrivée de l'**intégration native Strava** et de nombreuses améliorations de stabilité.

## ✨ Nouveautés Principales

### 🟠 Intégration Strava Directe
*   **Connexion Native :** Connectez votre compte Strava directement depuis l'application.
*   **Auth Simplifiée :** Si l'application Strava est installée, l'authentification se fait sans passer par le navigateur !
*   **Confidentialité :** Vos identifiants sont gérés de manière sécurisée et ne transitent que vers Strava.

### 🔌 Autres Services (Legacy Support)
*   Retour des boutons pour **Garmin**, **Polar**, et **Suunto** dans le menu Paramètres.
*   Possibilité de définir manuellement l'état de connexion pour ces services.
*   *Note : L'intégration complète Health Connect reste le standard recommandé.*

### ⚙️ Configuration de la Synchronisation
Nouveau menu dans **Profil > Synchronisation**:
- Choisissez la période à synchroniser:
  - 30 derniers jours (défaut)
  - 60 jours
  - 3 derniers mois
  - 6 derniers mois
  - 1 an
  - **Tout l'historique**

### 🔄 Pagination Intelligente
Pour les synchronisations massives (ex: Tout l'historique):
- Chargement par blocs de **30 jours**
- Indicateur de progression visuel (ex: "3/12")
- Gestion optimisée de la mémoire

### 🎨 Nouvelle Identité Visuelle
- Nouvelle phrase d'accroche: "La Légèreté de la Course. La Puissance de l'Analyse."
- Interface épurée et indicateurs de progression.

---

## 🐛 Corrections & Optimisations

- **Fix:** Gestion améliorée des permissions Health Connect
- **Perf:** Lazy loading des activités via pagination
- **Clean:** Suppression de ~3500 lignes de code legacy (anciennes APIs)

---

## 🚀 Comment Tester

1. Installez l'APK v3.0
2. Allez dans **Profil > Applications & Montres**
3. Cliquez sur **Strava** pour tester la nouvelle connexion native.
4. Si l'application Strava est installée, elle s'ouvrira directement pour l'autorisation.
5. Vérifiez que le statut passe à "Connecté".

---

