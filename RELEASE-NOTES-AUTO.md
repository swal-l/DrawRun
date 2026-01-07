# 🤖 Génération Automatique des Release Notes

## ✨ Nouvelle Fonctionnalité

Le script `deploy.ps1` génère maintenant **automatiquement** les notes de version dans `version_info.json` en analysant vos commits Git!

---

## 🎯 Comment Ça Marche

### 1. Analyse des Commits Git

Le script récupère les commits récents:
```powershell
# Commits des 7 derniers jours
git log --pretty=format:"%s" --since="7 days ago"

# Ou les 10 derniers commits si moins de 7 jours
git log --pretty=format:"%s" -n 10
```

### 2. Catégorisation Intelligente

Les commits sont automatiquement catégorisés en **Features** ou **Fixes**:

#### 🎉 Features (Nouvelles fonctionnalités)
Détectés par:
- Mots-clés: `feat`, `feature`, `add`, `new`, `implement`
- Emojis: ✨ 🎉 ⚡ 🚀

**Exemples:**
```
feat: Ajout de la synchronisation Garmin
✨ Nouvelle interface de statistiques
add: Support des montres Garmin
🚀 Implémentation du mode 3D
```

#### 🐛 Fixes (Corrections de bugs)
Détectés par:
- Mots-clés: `fix`, `bug`, `correct`, `resolve`
- Emojis: 🐛 🔧 ✅

**Exemples:**
```
fix: Correction erreur 401 Garmin
🐛 Résolution crash au démarrage
correct: Affichage incorrect des zones
✅ Fix synchronisation Strava
```

### 3. Génération du JSON

Le script met à jour automatiquement `version_info.json`:

```json
{
  "latestVersionCode": 31,
  "latestVersionName": "2.6",
  "downloadUrl": "https://swal-l.github.io/DrawRun/DrawRun_v2.6.apk",
  "releaseNotes": {
    "features": [
      "Ajout de la synchronisation Garmin",
      "Nouvelle interface de statistiques",
      "Support des montres Garmin"
    ],
    "fixes": [
      "Correction erreur 401 Garmin",
      "Résolution crash au démarrage",
      "Fix synchronisation Strava"
    ]
  }
}
```

---

## 📝 Bonnes Pratiques pour les Commits

Pour que la génération automatique fonctionne bien, utilisez des messages de commit clairs:

### ✅ Bon Format

```bash
# Features
git commit -m "feat: Ajout synchronisation Health Connect"
git commit -m "✨ Nouveau graphique de fréquence cardiaque"
git commit -m "add: Support MapLibre 3D"

# Fixes
git commit -m "fix: Correction calcul de la cadence"
git commit -m "🐛 Résolution problème de cache"
git commit -m "correct: Affichage des zones FC"
```

### ❌ Format à Éviter

```bash
# Trop vague
git commit -m "update"
git commit -m "changes"
git commit -m "wip"

# Pas de catégorie claire
git commit -m "Modification du code"
git commit -m "Changements divers"
```

---

## 🎨 Emojis Recommandés

Utilisez ces emojis pour une meilleure catégorisation:

### Features
- ✨ `:sparkles:` - Nouvelle fonctionnalité
- 🎉 `:tada:` - Début de projet / Grosse feature
- ⚡ `:zap:` - Amélioration de performance
- 🚀 `:rocket:` - Déploiement / Release

### Fixes
- 🐛 `:bug:` - Correction de bug
- 🔧 `:wrench:` - Correction de configuration
- ✅ `:white_check_mark:` - Tests / Validation

### Autres (non catégorisés mais utiles)
- 📝 `:memo:` - Documentation
- 🎨 `:art:` - Amélioration du code
- ♻️ `:recycle:` - Refactoring
- 🔥 `:fire:` - Suppression de code

---

## 🔄 Fallback Automatique

Si aucun commit catégorisé n'est trouvé, le script utilise des messages génériques:

```json
{
  "features": [
    "Améliorations de performance",
    "Optimisations diverses"
  ],
  "fixes": [
    "Corrections de bugs mineurs",
    "Améliorations de stabilité"
  ]
}
```

---

## 📊 Limites

- **Maximum 5 features** affichées
- **Maximum 5 fixes** affichés
- Analyse les **7 derniers jours** ou **10 derniers commits**

Cela évite d'avoir des notes de version trop longues.

---

## 🎯 Exemple Complet

### Commits depuis la dernière version:
```bash
✨ Ajout synchronisation Garmin complète
🐛 Fix erreur 401 lors de l'authentification
feat: Support des données de fréquence cardiaque
🔧 Correction affichage des zones
add: Graphique de puissance amélioré
fix: Résolution problème de cache Gradle
```

### Résultat dans version_info.json:
```json
{
  "latestVersionCode": 31,
  "latestVersionName": "2.6",
  "downloadUrl": "https://swal-l.github.io/DrawRun/DrawRun_v2.6.apk",
  "releaseNotes": {
    "features": [
      "Ajout synchronisation Garmin complète",
      "Support des données de fréquence cardiaque",
      "Graphique de puissance amélioré"
    ],
    "fixes": [
      "Fix erreur 401 lors de l'authentification",
      "Correction affichage des zones",
      "Résolution problème de cache Gradle"
    ]
  }
}
```

### Affichage dans le script:
```
[5/6] Updating website files...
  ⚙ Generating release notes...
  ✓ Updated version_info.json with AI-generated notes
    Features: 3 | Fixes: 3
```

---

## 🛠️ Personnalisation

### Modifier les Patterns de Détection

Éditez `deploy.ps1` ligne ~113:

```powershell
# Ajouter d'autres mots-clés pour features
if ($commit -match "^(feat|feature|add|new|implement|enhance)" -or ...)

# Ajouter d'autres mots-clés pour fixes
elseif ($commit -match "^(fix|bug|correct|resolve|patch)" -or ...)
```

### Modifier les Messages Fallback

Éditez `deploy.ps1` ligne ~142:

```powershell
if ($features.Count -eq 0 -and $fixes.Count -eq 0) {
    $features = @("Vos messages personnalisés")
    $fixes = @("Vos corrections personnalisées")
}
```

### Changer le Nombre de Commits Analysés

Éditez `deploy.ps1` ligne ~113:

```powershell
# Analyser les 20 derniers commits au lieu de 10
$gitLog = git log --pretty=format:"%s" -n 20 2>$null

# Analyser les 14 derniers jours au lieu de 7
$gitLog = git log --pretty=format:"%s" --since="14 days ago" 2>$null
```

---

## ✅ Avantages

✅ **Automatique** - Plus besoin d'écrire manuellement les notes  
✅ **Cohérent** - Format standardisé  
✅ **Rapide** - Génération en quelques secondes  
✅ **Intelligent** - Catégorisation automatique  
✅ **Flexible** - Fallback si pas de commits catégorisés  

---

## 📚 Utilisation avec UpdateManager

L'app Android utilise `version_info.json` pour détecter les mises à jour:

```kotlin
// Dans UpdateManager.kt
val updateInfo = checkForUpdate(currentVersionCode)
if (updateInfo != null) {
    // Affiche les features et fixes dans un dialogue
    updateInfo.releaseNotes.features.forEach { ... }
    updateInfo.releaseNotes.fixes.forEach { ... }
}
```

Les utilisateurs verront automatiquement les nouvelles fonctionnalités et corrections!

---

## 🎯 Workflow Recommandé

1. **Développez** votre fonctionnalité
2. **Committez** avec un message clair et catégorisé:
   ```bash
   git commit -m "✨ Ajout support MapLibre 3D"
   ```
3. **Lancez** le déploiement:
   ```bash
   .\deploy.ps1
   ```
4. Le script:
   - ✅ Incrémente la version
   - ✅ Build l'APK
   - ✅ **Génère les release notes automatiquement**
   - ✅ Met à jour version_info.json
   - ✅ Commit et push

**Tout est automatique!** 🚀

---

*Documentation générée le 2026-01-07*
