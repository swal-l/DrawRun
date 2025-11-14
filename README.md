# Générateur de Programme d'Entraînement - Application Web

Application Flask pour générer des programmes d'entraînement de course à pied personnalisés.

## Installation

1. Installez les dépendances :
```bash
pip install -r requirements.txt
```

## Lancement de l'application

```bash
python app.py
```

L'application sera accessible sur : http://localhost:5000

## Utilisation

1. Ouvrez votre navigateur à l'adresse http://localhost:5000
2. Remplissez le formulaire avec :
   - Volume de temps hebdomadaire (en heures)
   - Nombre de semaines d'entraînement
   - Objectif (distance en km)
3. Cliquez sur "Générer mon programme"
4. Consultez votre programme détaillé semaine par semaine

## Fonctionnalités

- ✅ Génération de programmes d'entraînement personnalisés
- ✅ Séances de VMA, Seuil et Endurance Fondamentale
- ✅ Périodisation avec cycles de récupération
- ✅ Interface web intuitive et responsive
- ✅ Affichage détaillé de chaque séance

## Structure du projet

```
Algo_prog/
├── app.py                 # Application Flask principale
├── requirements.txt       # Dépendances Python
├── README.md             # Documentation
├── templates/
│   ├── index.html        # Page d'accueil avec formulaire
│   └── results.html      # Page de résultats
└── static/
    └── style.css         # Styles CSS
```
