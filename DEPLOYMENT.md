# Programme d'Entraînement Course à Pied

Application web Flask pour générer des programmes d'entraînement personnalisés.

## Déploiement avec Dokploy

### Prérequis
- Un compte Dokploy configuré
- Git installé localement

### Étapes de déploiement

#### 1. Initialiser un dépôt Git (si pas déjà fait)
```bash
git init
git add .
git commit -m "Initial commit"
```

#### 2. Pousser vers un dépôt distant (GitHub, GitLab, etc.)
```bash
git remote add origin <URL_DE_VOTRE_DEPOT>
git branch -M main
git push -u origin main
```

#### 3. Configuration dans Dokploy

1. **Connectez-vous à Dokploy**
2. **Créez une nouvelle application**
   - Cliquez sur "New Application"
   - Choisissez "Docker" comme type

3. **Configurez le dépôt**
   - Repository URL : Collez l'URL de votre dépôt Git
   - Branch : `main` (ou votre branche principale)

4. **Configuration Docker**
   - Dockerfile path : `./Dockerfile`
   - Port : `5000`

5. **Variables d'environnement** (optionnel)
   ```
   FLASK_ENV=production
   ```

6. **Déployez**
   - Cliquez sur "Deploy"
   - Attendez que le build se termine

#### 4. Tester l'application
Une fois déployée, Dokploy vous donnera une URL pour accéder à votre application.

## Alternative : Déploiement manuel avec Docker

### Test local
```bash
# Construire l'image
docker build -t training-app .

# Lancer le conteneur
docker run -p 5000:5000 training-app
```

Accédez à http://localhost:5000

### Avec Docker Compose
```bash
docker-compose up -d
```

## Structure du projet
```
.
├── app.py              # Application Flask principale
├── requirements.txt    # Dépendances Python
├── Dockerfile         # Configuration Docker
├── docker-compose.yml # Configuration Docker Compose
├── static/
│   └── style.css      # Styles CSS
└── templates/
    ├── index.html     # Page d'accueil
    └── results.html   # Page des résultats
```

## Technologies utilisées
- Flask 3.0.0
- Gunicorn (serveur WSGI)
- Docker
