# 🚀 Guide de Build - DrawRun

## ✅ Vérification des Fichiers

Tous les fichiers essentiels sont OK:
- ✅ `app/build.gradle.kts` - Configuration Gradle valide
- ✅ `AndroidManifest.xml` - Manifest correct
- ✅ Fichiers Kotlin - Pas d'erreurs de compilation détectées
- ✅ Dépendances - Toutes les librairies sont compatibles

## 🏗️ Options de Build

### Option 1: Build Simple (RECOMMANDÉ pour dev)
**Le plus rapide - Juste compiler l'APK**

```powershell
.\build-simple.ps1
```

**Avantages:**
- ⚡ Très rapide (1-2 minutes)
- 🎯 Pas de modification de version
- 📦 APK dans `app/build/outputs/apk/release/`
- ✅ Parfait pour tester rapidement

### Option 2: Build avec Android Studio
**Interface graphique**

1. Ouvrir le projet dans Android Studio
2. Menu: `Build` → `Build Bundle(s) / APK(s)` → `Build APK(s)`
3. Attendre la compilation
4. Cliquer sur "locate" dans la notification

**Avantages:**
- 🖱️ Interface visuelle
- 🔍 Erreurs affichées clairement
- 🛠️ Outils de debug intégrés

### Option 3: Build Complet avec Déploiement ⚡ (OPTIMISÉ!)
**Pour publier sur le site web**

```powershell
.\deploy.ps1
```

**Ce script fait:**
1. ⬆️ Auto-incrémente la version
2. 🔨 Build l'APK (avec retry intelligent)
3. 📋 Copie vers `docs/`
4. 🌐 Met à jour `index.html`
5. 🤖 **Génère les release notes automatiquement** (analyse Git)
6. 📝 Met à jour `version_info.json`
7. 📤 Commit et push Git

**✨ NOUVEAU - Optimisations:**
- ⚡ **2-3x plus rapide** qu'avant
- 🎯 Ne nettoie le cache **que si le build échoue**
- 💾 Nettoie seulement le cache local (pas le cache global)
- 🔍 Affiche les erreurs si le build échoue
- 🤖 **Génère les release notes avec l'IA** en analysant vos commits Git

**Temps:** 1-2 minutes (au lieu de 5-10 min avant!)

> 📖 Voir `OPTIMISATIONS.md` pour les détails techniques  
> 🤖 Voir `RELEASE-NOTES-AUTO.md` pour la génération automatique des notes


## 🎯 Commandes Gradle Directes

### Build Release
```powershell
.\gradlew.bat assembleRelease
```

### Build Debug (plus rapide)
```powershell
.\gradlew.bat assembleDebug
```

### Nettoyer le projet
```powershell
.\gradlew.bat clean
```

### Nettoyer + Build
```powershell
.\gradlew.bat clean assembleRelease
```

## 📍 Localisation de l'APK

Après le build, l'APK se trouve ici:

**Release:**
```
app/build/outputs/apk/release/DrawRun_v2.5.apk
```

**Debug:**
```
app/build/outputs/apk/debug/DrawRun_v2.5.apk
```

## 🐛 Résolution de Problèmes

### Build échoue?

1. **Nettoyer le cache Gradle:**
```powershell
.\gradlew.bat --stop
Remove-Item -Recurse -Force .gradle
.\gradlew.bat clean
```

2. **Vérifier JAVA_HOME:**
```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
```

3. **Rebuild complet:**
```powershell
.\gradlew.bat clean assembleRelease --no-daemon
```

### Android Studio ne trouve pas le SDK?

Vérifier `local.properties` contient:
```properties
sdk.dir=C\:\\Users\\lomic\\AppData\\Local\\Android\\Sdk
```

## 📊 Version Actuelle

- **Version Name:** 2.5
- **Version Code:** 30

Pour changer manuellement, éditer `app/build.gradle.kts`:
```kotlin
versionCode = 30
versionName = "2.5"
```

## ✅ Recommandation

**Pour le développement quotidien:**
→ Utilisez `.\build-simple.ps1` ou Android Studio directement

**Pour publier une nouvelle version:**
→ Utilisez `.\deploy.ps1` (mais seulement quand vous êtes prêt!)

---

*Dernière mise à jour: 2026-01-07*
