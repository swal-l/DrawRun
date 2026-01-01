# 🏃 DrawRun - Intégration APIs Officielles

## ✅ APIs Implémentées

DrawRun supporte l'envoi automatique d'entraînements vers :
- **Garmin Connect** (Course & Natation)
- **Strava** (Course & Natation)
- **Polar Flow** (Course & Natation)
- **Suunto App** (Course & Natation)

## 📋 Configuration des APIs

### 1. Garmin Connect API

**Étapes:**
1. Créez un compte développeur sur [developer.garmin.com](https://developer.garmin.com/gc-developer-program/overview)
2. Créez une nouvelle application
3. Obtenez vos clés:
   - Consumer Key
   - Consumer Secret
4. Ouvrez `app/src/main/java/com/orbital/run/api/GarminAPI.kt`
5. Remplacez:
   ```kotlin
   private const val CONSUMER_KEY = "VOTRE_CLE_ICI"
   private const val CONSUMER_SECRET = "VOTRE_SECRET_ICI"
   ```

**Documentation:** https://developer.garmin.com/gc-developer-program/overview

---

### 2. Strava API

**Étapes:**
1. Créez une app sur [strava.com/settings/api](https://www.strava.com/settings/api)
2. Obtenez vos clés:
   - Client ID
   - Client Secret
3. Ouvrez `app/src/main/java/com/orbital/run/api/OtherAPIs.kt`
4. Dans `StravaAPI`, remplacez:
   ```kotlin
   private const val CLIENT_ID = "VOTRE_ID_ICI"
   private const val CLIENT_SECRET = "VOTRE_SECRET_ICI"
   ```

**Documentation:** https://developers.strava.com/docs/reference/

---

### 3. Polar Flow API

**Étapes:**
1. Demandez l'accès sur [polar.com/accesslink-api](https://www.polar.com/accesslink-api/)
2. Créez une application
3. Obtenez vos clés API
4. Dans `app/src/main/java/com/orbital/run/api/OtherAPIs.kt`
5. Dans `PolarAPI`, remplacez:
   ```kotlin
   private const val CLIENT_ID = "VOTRE_ID_ICI"
   private const val CLIENT_SECRET = "VOTRE_SECRET_ICI"
   ```

**Documentation:** https://www.polar.com/accesslink-api/

---

### 4. Suunto App API

**Étapes:**
1. Créez un compte sur [apizone.suunto.com](https://apizone.suunto.com/)
2. Enregistrez votre application
3. Obtenez les credentials
4. Dans `app/src/main/java/com/orbital/run/api/OtherAPIs.kt`
5. Dans `SuuntoAPI`, remplacez:
   ```kotlin
   private const val CLIENT_ID = "VOTRE_ID_ICI"
   private const val CLIENT_SECRET = "VOTRE_SECRET_ICI"
   ```

**Documentation:** https://apizone.suunto.com/

---

## 🔑 Utilisation dans l'App

### Connexion à un Service

1. Ouvrez l'app DrawRun
2. Allez dans **Paramètres** (onglet "Moi")
3. Section "Applications & Montres"
4. Cliquez sur **"Lier"** pour le service souhaité
5. Suivez le flux OAuth dans le navigateur
6. Autorisez l'application
7. Retournez dans DrawRun

### Envoi d'un Entraînement

1. Créez une séance de natation ou course
2. Cliquez sur le bouton **Partager** (icône d'envoi)
3. L'app détecte automatiquement le service connecté
4. L'entraînement est envoyé vers votre compte

### Multi-Services

- Vous pouvez connecter **plusieurs services simultanément**
- L'app utilisera le **premier service connecté** dans l'ordre:
  1. Garmin Connect
  2. Strava
  3. Polar Flow
  4. Suunto App

---

## 🛠 Mode Développement

Si aucune API n'est configurée, l'app propose automatiquement :
- Export JSON structuré
- Résumé texte enrichi avec emojis
- Partage via Intent Android (Email, WhatsApp, etc.)

---

## ⚠️ Notes Importantes

### Sécurité
- **NE COMMITTEZ JAMAIS vos clés API dans Git**
- Utilisez des variables d'environnement en production
- Les clés doivent rester privées

### Limitations
- Les APIs ont des quotas et limites
- OAuth nécessite une connexion internet
- Certaines APIs peuvent avoir des délais de validation

### Format des Données
- **Garmin**: Format JSON propriétaire
- **Strava**: GPX ou FIT
- **Polar**: JSON AccessLink format
- **Suunto**: FIT files

---

## 📱 Architecture

```
DrawRun
├── api/
│   ├── GarminAPI.kt (OAuth 1.0 + Workout Upload)
│   └── OtherAPIs.kt (Strava, Polar, Suunto - OAuth 2.0)
├── ui/
│   ├── MainScreen.kt (Routing & shareWorkout)
│   └── GarminConnectCard.kt (OAuth UI)
└── logic/
    └── Workout.kt (Data models)
```

### Flux OAuth

1. User clique "Lier" → `openAuthorizationPage()`
2. Navigateur s'ouvre → Authorization URL
3. User autorise → Redirection avec code
4. App échange code → `exchangeToken()`
5. Access Token sauvegardé → `isAuthenticated() = true`
6. Upload possible → `uploadWorkout()`

---

## 🚀 Prochaines Étapes

- [ ] Ajouter stockage sécurisé des tokens (EncryptedSharedPreferences)
- [ ] Implémenter refresh token automatique
- [ ] Ajouter gestion multi-comptes
- [ ] Support format FIT natif (plus universel)
- [ ] Synchronisation bidirectionnelle (import workouts)

---

## 📞 Support

Pour toute question sur l'intégration des APIs:
- Garmin: developer.garmin.com/support
- Strava: developers.strava.com/docs
- Polar: Contactez Polar via leur portail
- Suunto: support.suunto.com

---

**Version:** 1.0  
**Dernière mise à jour:** 2025-12-12
