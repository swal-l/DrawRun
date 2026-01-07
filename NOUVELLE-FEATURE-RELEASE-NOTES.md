# ✅ Nouvelle Fonctionnalité Ajoutée!

**Date:** 2026-01-07 16:02  
**Feature:** Génération Automatique des Release Notes 🤖

---

## 🎯 Ce Qui a Été Ajouté

### ✨ Génération Automatique des Notes de Version

Le script `deploy.ps1` génère maintenant **automatiquement** les notes de version dans `version_info.json`!

---

## 🔄 Comment Ça Fonctionne

### 1. Analyse des Commits Git

Le script analyse vos commits récents:
- 📅 Commits des **7 derniers jours**
- 📝 Ou les **10 derniers commits**

### 2. Catégorisation Intelligente

Les commits sont automatiquement triés en:

#### 🎉 **Features** (Nouvelles fonctionnalités)
Détectés par:
- Mots-clés: `feat`, `feature`, `add`, `new`, `implement`
- Emojis: ✨ 🎉 ⚡ 🚀

**Exemples:**
```bash
git commit -m "feat: Ajout synchronisation Garmin"
git commit -m "✨ Nouveau graphique 3D"
git commit -m "add: Support Health Connect"
```

#### 🐛 **Fixes** (Corrections)
Détectés par:
- Mots-clés: `fix`, `bug`, `correct`, `resolve`
- Emojis: 🐛 🔧 ✅

**Exemples:**
```bash
git commit -m "fix: Correction erreur 401"
git commit -m "🐛 Résolution crash au démarrage"
git commit -m "correct: Affichage des zones"
```

### 3. Mise à Jour Automatique

Le script met à jour `version_info.json`:

```json
{
  "latestVersionCode": 31,
  "latestVersionName": "2.6",
  "downloadUrl": "https://swal-l.github.io/DrawRun/DrawRun_v2.6.apk",
  "releaseNotes": {
    "features": [
      "Ajout synchronisation Garmin",
      "Nouveau graphique 3D",
      "Support Health Connect"
    ],
    "fixes": [
      "Correction erreur 401",
      "Résolution crash au démarrage",
      "Affichage des zones"
    ]
  }
}
```

---

## 📋 Modifications Apportées

### Fichier: `deploy.ps1`

#### ✅ Ajouté:
1. **Variable `$versionInfoFile`** - Référence au fichier JSON
2. **Extraction du `versionCode`** - Pour mettre à jour le JSON
3. **Analyse des commits Git** - Récupération des messages
4. **Catégorisation intelligente** - Features vs Fixes
5. **Génération des notes** - Création automatique
6. **Mise à jour du JSON** - Avec les nouvelles notes

#### 📊 Affichage:
```
[5/6] Updating website files...
  ⚙ Generating release notes...
  ✓ Updated version_info.json with AI-generated notes
    Features: 3 | Fixes: 2
```

---

## 🎨 Bonnes Pratiques

Pour que la génération fonctionne bien, utilisez des commits clairs:

### ✅ Recommandé:
```bash
# Features
git commit -m "feat: Nouvelle fonctionnalité X"
git commit -m "✨ Ajout de Y"
git commit -m "add: Support de Z"

# Fixes
git commit -m "fix: Correction du bug X"
git commit -m "🐛 Résolution problème Y"
git commit -m "correct: Affichage de Z"
```

### ❌ À Éviter:
```bash
git commit -m "update"
git commit -m "changes"
git commit -m "wip"
git commit -m "modifications"
```

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

- **Max 5 features** affichées
- **Max 5 fixes** affichés
- Analyse **7 jours** ou **10 commits**

Cela garde les notes concises et lisibles.

---

## 🚀 Workflow Complet

### 1. Développez
```bash
# Faites vos modifications
```

### 2. Committez avec un message clair
```bash
git commit -m "✨ Ajout support MapLibre 3D"
git commit -m "🐛 Fix calcul de cadence"
```

### 3. Déployez
```bash
.\deploy.ps1
```

### 4. Le Script Fait Tout!
```
🔄 AUTO-INCREMENT VERSION
  Version Code: 30 → 31
  Version Name: 2.5 → 2.6

[3/6] Building APK...
  ✓ APK built successfully

[4/6] Deploying to docs...
  ✓ Deployed: DrawRun_v2.6.apk

[5/6] Updating website files...
  ⚙ Generating release notes...
  ✓ Updated version_info.json with AI-generated notes
    Features: 2 | Fixes: 1

[6/6] Pushing to GitHub...

✅ DEPLOYMENT COMPLETE!
```

---

## 📚 Documentation

J'ai créé plusieurs fichiers:

### 1. `RELEASE-NOTES-AUTO.md` 🤖
Documentation complète sur:
- Comment fonctionne la génération
- Bonnes pratiques de commits
- Emojis recommandés
- Personnalisation
- Exemples

### 2. `deploy.ps1` (modifié) ⚡
Script mis à jour avec:
- Génération automatique des notes
- Mise à jour de version_info.json
- Affichage du nombre de features/fixes

### 3. `README-BUILD.md` (mis à jour) 📖
Guide de build avec la nouvelle fonctionnalité

---

## ✅ Ce Que Fait Maintenant deploy.ps1

1. ⬆️ **Auto-incrémente** la version (Code + Name)
2. 🔨 **Build** l'APK (avec retry intelligent)
3. 📋 **Copie** vers docs/
4. 🌐 **Met à jour** index.html
5. 🤖 **Génère** les release notes (analyse Git)
6. 📝 **Met à jour** version_info.json
7. 📤 **Commit et push** Git

**Tout est automatique!**

---

## 🎯 Avantages

✅ **Plus besoin d'écrire manuellement** les notes de version  
✅ **Cohérence** - Format standardisé  
✅ **Rapidité** - Génération en secondes  
✅ **Intelligence** - Catégorisation automatique  
✅ **Flexibilité** - Fallback si pas de commits  
✅ **Intégration** - Utilisé par l'app Android (UpdateManager)  

---

## 📱 Utilisation dans l'App

L'app Android utilise `version_info.json` pour:
1. Détecter les nouvelles versions
2. Afficher les features et fixes
3. Proposer le téléchargement

Les utilisateurs verront automatiquement:
```
🎉 Nouvelle version 2.6 disponible!

Nouveautés:
✨ Ajout support MapLibre 3D
✨ Nouveau graphique de puissance

Corrections:
🐛 Fix calcul de cadence
🐛 Résolution problème de cache
```

---

## 🎉 Résultat

Vous avez maintenant un système de déploiement **complètement automatisé**:

1. Vous codez
2. Vous committez avec des messages clairs
3. Vous lancez `.\deploy.ps1`
4. **Tout se fait automatiquement!**

**Plus besoin de:**
- ❌ Écrire manuellement les notes
- ❌ Mettre à jour version_info.json
- ❌ Se souvenir de ce qui a changé

**Le script fait tout pour vous!** 🚀

---

*Fonctionnalité ajoutée le 2026-01-07*
