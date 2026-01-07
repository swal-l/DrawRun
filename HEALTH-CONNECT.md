# Guide Health Connect 💚

## Qu'est-ce que Health Connect?

**Health Connect** est la plateforme centralisée de Google pour gérer vos données de santé et de fitness sur Android. Elle permet à DrawRun de synchroniser automatiquement vos activités depuis toutes vos applications de sport préférées.

### Avantages

✅ **Synchronisation Universelle** - Fonctionne avec Garmin, Strava, Polar, Suunto, et bien d'autres  
✅ **Données Complètes** - FC, vitesse, puissance, GPS, cadence, calories, etc.  
✅ **Sécurisé** - Vos données restent sur votre appareil  
✅ **Automatique** - Pas besoin de connexions OAuth multiples  
✅ **Natif Android** - Intégration système optimisée  

---

## Installation et Configuration

### 1. Installer Health Connect

Si vous n'avez pas encore Health Connect:

1. Ouvrez le **Play Store**
2. Recherchez "**Health Connect**"
3. Installez l'application officielle de Google
4. Ouvrez Health Connect et suivez la configuration initiale

### 2. Connecter vos Applications de Sport

Pour que DrawRun puisse accéder à vos activités, vos apps de sport doivent partager leurs données avec Health Connect:

#### Garmin Connect
1. Ouvrez **Garmin Connect**
2. Allez dans **Paramètres** → **Confidentialité**
3. Activez **Health Connect**
4. Autorisez le partage de données

#### Strava
1. Ouvrez **Strava**
2. Allez dans **Paramètres** → **Applications et Services**
3. Connectez **Health Connect**
4. Autorisez les permissions

#### Polar Flow / Suunto App
Même processus - cherchez l'option Health Connect dans les paramètres de l'application.

### 3. Autoriser DrawRun

Au premier lancement de DrawRun:

1. L'écran d'onboarding s'affiche
2. Appuyez sur **"Connecter Health Connect"**
3. Autorisez toutes les permissions demandées:
   - 📊 Sessions d'exercice
   - ❤️ Fréquence cardiaque
   - 🏃 Vitesse et distance
   - ⚡ Puissance
   - 👣 Cadence
   - 📍 Routes GPS
   - 🔥 Calories
   - 🫁 Fréquence respiratoire

4. Appuyez sur **"Commencer"**

---

## Utilisation

### Synchronisation Automatique

DrawRun synchronise automatiquement vos activités:
- ✅ Au démarrage de l'application
- ✅ Lorsque vous revenez sur l'app
- ✅ Via le bouton de synchronisation manuelle

### Synchronisation Manuelle

Pour forcer une synchronisation:

1. Allez dans l'onglet **"Analyse"**
2. Appuyez sur le bouton **flottant de synchronisation** (icône de rafraîchissement)
3. Un popup vous indique le résultat:
   - ✅ "X nouvelles activités synchronisées"
   - ℹ️ "Déjà à jour - 0 nouvelles"
   - ⚠️ "Connectez Health Connect dans les paramètres"

### Gérer les Permissions

Pour modifier les permissions Health Connect:

1. Ouvrez les **Paramètres Android**
2. Allez dans **Apps** → **Health Connect**
3. Sélectionnez **DrawRun**
4. Gérez les permissions individuellement

---

## Données Synchronisées

DrawRun récupère les données suivantes depuis Health Connect:

### Sessions d'Exercice
- Type d'activité (course, natation, vélo, etc.)
- Date et heure
- Durée
- Titre/Notes

### Métriques Cardiaques
- Fréquence cardiaque moyenne
- Fréquence cardiaque maximale
- Échantillons FC seconde par seconde
- Zones cardiaques

### Métriques de Performance
- Distance totale
- Vitesse moyenne et instantanée
- Allure (min/km)
- Puissance (watts) - si disponible
- Cadence (pas/min ou RPM)

### Données GPS
- Routes complètes (latitude/longitude)
- Altitude
- Polylines pour affichage carte

### Autres Métriques
- Calories brûlées
- Gain d'élévation
- Fréquence respiratoire
- Running dynamics (si disponible)

---

## Résolution de Problèmes

### Aucune activité ne s'affiche

**Vérifications:**

1. **Health Connect est installé?**
   - Vérifiez dans le Play Store

2. **Vos apps de sport partagent leurs données?**
   - Ouvrez Health Connect
   - Vérifiez que Garmin/Strava/etc. sont connectés
   - Vérifiez qu'ils ont des données récentes

3. **DrawRun a les permissions?**
   - Ouvrez DrawRun → Paramètres
   - Vérifiez que Health Connect est "Connecté"
   - Si non, reconnectez

4. **Synchronisation manuelle**
   - Appuyez sur le bouton de sync dans l'onglet Analyse
   - Vérifiez le message de résultat

### Les données sont incomplètes

Certaines métriques (puissance, cadence) dépendent de votre équipement:
- **Puissance** : Nécessite un capteur de puissance
- **Cadence** : Nécessite un capteur de cadence ou montre compatible
- **GPS** : Nécessite une montre GPS ou téléphone

Si votre montre/app enregistre ces données, elles seront synchronisées via Health Connect.

### Activités en double

Si vous voyez des doublons:
1. Vérifiez que vous n'avez pas plusieurs apps qui enregistrent la même activité
2. Supprimez les doublons dans DrawRun (swipe ou menu)
3. Les activités supprimées sont blacklistées et ne reviendront pas

---

## Confidentialité et Sécurité

### Où sont stockées mes données?

- ✅ **Localement sur votre appareil** - DrawRun ne transfère aucune donnée vers des serveurs externes
- ✅ **Health Connect** - Géré par Google, chiffré et sécurisé
- ✅ **Contrôle total** - Vous pouvez révoquer les permissions à tout moment

### DrawRun peut-il modifier mes données?

- ❌ **Non** - DrawRun a uniquement un accès en **lecture seule**
- ✅ Vos données originales dans Garmin/Strava restent intactes
- ✅ Les suppressions dans DrawRun n'affectent pas vos autres apps

### Puis-je utiliser DrawRun sans Health Connect?

Non, DrawRun nécessite Health Connect pour fonctionner. C'est la seule source de données de l'application, ce qui garantit:
- Simplicité d'utilisation (une seule connexion)
- Compatibilité universelle (toutes les apps)
- Sécurité maximale (pas de tokens OAuth multiples)

---

## FAQ

### Q: Dois-je garder Garmin Connect/Strava installés?

**R:** Oui! Health Connect récupère les données depuis ces applications. Elles doivent rester installées et configurées pour partager avec Health Connect.

### Q: Les données sont-elles synchronisées en temps réel?

**R:** Health Connect synchronise périodiquement. DrawRun récupère les nouvelles données à chaque ouverture ou synchronisation manuelle.

### Q: Puis-je utiliser DrawRun hors ligne?

**R:** Oui! Une fois les activités synchronisées, vous pouvez les consulter hors ligne. La synchronisation nécessite une connexion internet.

### Q: Combien d'activités sont synchronisées?

**R:** DrawRun synchronise les **30 dernières activités** par défaut. Cela couvre généralement le dernier mois d'entraînement.

### Q: Health Connect consomme-t-il beaucoup de batterie?

**R:** Non, Health Connect est optimisé par Google et consomme très peu de batterie. DrawRun ne synchronise que lorsque vous l'ouvrez.

---

## Support

### Besoin d'aide?

- 📧 **Email** : Créez une issue sur [GitHub](https://github.com/swal-l/DrawRun/issues)
- 📚 **Documentation** : Consultez les autres guides dans le dépôt
- 🐛 **Bug** : Signalez-le via GitHub Issues

### Ressources Utiles

- [Documentation officielle Health Connect](https://developer.android.com/health-and-fitness/guides/health-connect)
- [FAQ Health Connect](https://support.google.com/android/answer/12458602)
- [Politique de confidentialité DrawRun](https://swal-l.github.io/DrawRun/)

---

**Profitez de DrawRun! 🏃💨**
