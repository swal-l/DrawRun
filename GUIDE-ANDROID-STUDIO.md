# 🎯 Guide Rapide - Build avec Android Studio

## ✅ Vérification Préalable
Tous les fichiers sont OK! Vous pouvez builder sans problème.

---

## 🏗️ Méthode 1: Interface Android Studio (RECOMMANDÉ)

### Étapes:
1. **Ouvrir le projet**
   - Lancez Android Studio
   - `File` → `Open` → Sélectionnez `c:\Users\lomic\Dev\orbital-belt`

2. **Attendre la synchronisation Gradle**
   - Laissez Android Studio synchroniser (barre de progression en bas)
   - Peut prendre 1-2 minutes la première fois

3. **Builder l'APK**
   - Menu: `Build` → `Build Bundle(s) / APK(s)` → `Build APK(s)`
   - Ou raccourci: `Ctrl+Shift+A` puis tapez "Build APK"

4. **Récupérer l'APK**
   - Une notification apparaîtra en bas à droite
   - Cliquez sur `locate` pour ouvrir le dossier
   - L'APK est dans: `app/build/outputs/apk/release/DrawRun_v2.5.apk`

### Temps de Build:
- **Premier build:** 3-5 minutes
- **Builds suivants:** 1-2 minutes (cache Gradle)

---

## ⚡ Méthode 2: Script Rapide (Terminal)

### Dans PowerShell:
```powershell
cd c:\Users\lomic\Dev\orbital-belt
.\build-simple.ps1
```

**Avantages:**
- Plus rapide qu'Android Studio
- Pas besoin d'ouvrir l'IDE
- Parfait pour les builds de test

---

## 🐛 Résolution de Problèmes

### "Gradle sync failed"
1. Fermez Android Studio
2. Supprimez `.gradle` et `.idea`
3. Rouvrez le projet

### "SDK not found"
Vérifiez que `local.properties` contient:
```properties
sdk.dir=C\:\\Users\\lomic\\AppData\\Local\\Android\\Sdk
```

### Build très lent?
1. Activez le daemon Gradle (déjà fait dans le projet)
2. Augmentez la RAM Gradle dans `gradle.properties`:
   ```properties
   org.gradle.jvmargs=-Xmx2048m
   ```

### Cache corrompu?
```powershell
.\gradlew.bat --stop
Remove-Item -Recurse -Force .gradle
.\gradlew.bat clean
```

---

## 📦 Types de Build

### Debug (pour tester)
- Plus rapide à compiler
- APK plus gros
- Permet le debugging
```powershell
.\gradlew.bat assembleDebug
```

### Release (pour distribuer)
- Optimisé et signé
- APK plus petit
- Prêt pour distribution
```powershell
.\gradlew.bat assembleRelease
```

---

## 🚀 Workflow Recommandé

### Pour le Développement:
1. Ouvrez Android Studio
2. Faites vos modifications
3. `Build` → `Build APK(s)`
4. Testez sur appareil/émulateur

### Pour Publier:
1. Testez avec un build Debug
2. Vérifiez que tout fonctionne
3. Lancez `.\deploy.ps1` (met à jour version + site web)

---

## ✅ Checklist Avant Build

- [ ] Android Studio installé
- [ ] JDK configuré (inclus avec AS)
- [ ] SDK Android installé
- [ ] `local.properties` existe
- [ ] Connexion Internet (pour dépendances)

---

## 📍 Fichiers Importants

| Fichier | Description |
|---------|-------------|
| `build-simple.ps1` | Build rapide sans déploiement |
| `deploy.ps1` | Build + déploiement complet |
| `app/build.gradle.kts` | Configuration du build |
| `local.properties` | Chemins SDK (ignoré par Git) |

---

## 💡 Astuces

### Build Incrémental
Android Studio ne rebuild que ce qui a changé → beaucoup plus rapide!

### Parallel Build
Déjà activé dans le projet:
```kotlin
--parallel --build-cache
```

### Daemon Gradle
Reste actif entre les builds → gain de temps

---

## 🎯 Résumé

**Pour builder rapidement:**
→ Android Studio: `Build` → `Build APK(s)`

**Pour tester vite:**
→ `.\build-simple.ps1`

**Pour publier:**
→ `.\deploy.ps1` (seulement quand prêt!)

---

**Tout est prêt! Vous pouvez builder maintenant! 🚀**
