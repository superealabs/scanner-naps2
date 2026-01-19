# Scan Component - Web Component de Scan Embarqué

Web component réutilisable pour intégrer la fonctionnalité de scan de documents dans des pages web existantes.

## Structure

```
scan-embed/
├── index.html              # Page de test avec input file
├── scan-component.js       # Web component principal (modal)
├── scan-api.js            # Client API pour communiquer avec le scanner
├── scan-styles.css        # Styles isolés du component
└── README.MD              # Documentation
```

## Installation

1. Copier les fichiers dans votre projet
2. Inclure les scripts dans votre page HTML :

```html
<script src="scan-api.js"></script>
<script src="scan-component.js"></script>
```

## Utilisation

### HTML

```html
<!-- Input file cible -->
<input type="file" id="myFileInput" accept=".pdf" />

<!-- Web Component -->
<scan-modal 
    id="scanModal"
    api-base-url="http://localhost:7070/api/scans"
    target-input-id="myFileInput">
</scan-modal>

<!-- Bouton pour ouvrir la modal -->
<button onclick="document.getElementById('scanModal').open()">
    Scanner un document
</button>
```

### JavaScript

```javascript
const scanModal = document.getElementById('scanModal');

// Ouvrir la modal
scanModal.open();

// Fermer la modal
scanModal.close();

// Écouter les événements
scanModal.addEventListener('scan-completed', (e) => {
    console.log('PDF scanné:', e.detail.file);
    // Le fichier a été injecté dans l'input file
});

scanModal.addEventListener('scan-cancelled', () => {
    console.log('Scan annulé');
});

scanModal.addEventListener('scan-error', (e) => {
    console.error('Erreur:', e.detail);
});
```

## Attributs

- `api-base-url` (optionnel) : URL de base de l'API scanner. Par défaut: `http://localhost:7070/api/scans`
- `target-input-id` (optionnel) : ID de l'input file dans lequel injecter le PDF scanné

## Méthodes

- `open()` : Ouvre la modal de scan
- `close()` : Ferme la modal et annule le scan en cours

## Événements

- `scan-completed` : Émis quand un PDF est validé. `e.detail.file` contient le File object
- `scan-cancelled` : Émis quand l'utilisateur annule le scan
- `scan-error` : Émis en cas d'erreur. `e.detail` contient les détails de l'erreur

## Configuration du scan

Le component permet de configurer :

- **Scanner** : Nom du scanner (par défaut: "HP ScanJet Pro 2000 s2 (USB)")
- **Taille de papier** : A4, A3, Letter, Legal, etc.
- **Mode couleur** : Couleur, Niveaux de gris, Noir et blanc
- **Driver** : WIA (Windows), TWAIN, SANE (Linux)
- **Source** : Vitre (glass), Chargeur (feeder), Recto-verso (duplex)
- **Résolution** : DPI (par défaut: 300)

## Flux de travail

1. L'utilisateur ouvre la modal avec `open()`
2. L'utilisateur configure les paramètres de scan
3. L'utilisateur clique sur "Scanner"
4. Le component crée une session de scan via l'API
5. Le scan démarre et le statut est vérifié périodiquement
6. Une fois terminé, le PDF s'affiche en aperçu
7. L'utilisateur clique sur "Valider"
8. Le PDF est injecté dans l'input file cible
9. L'événement `scan-completed` est émis
10. La modal se ferme automatiquement

## API Backend

Le component communique avec l'API scanner via les endpoints suivants :

- `POST /api/scans` : Créer une session de scan
- `POST /api/scans/{id}/start` : Démarrer un scan
- `GET /api/scans/{id}` : Récupérer le statut d'un scan
- `GET /api/scans/{id}/pdf` : Télécharger le PDF scanné
- `DELETE /api/scans/{id}` : Annuler un scan

## Compatibilité

- Navigateurs modernes supportant les Web Components (Custom Elements API)
- Shadow DOM pour l'isolation complète
- Pas de dépendances externes

## Notes

- Le component utilise Shadow DOM pour isoler complètement les styles
- La modal se ferme avec Escape, clic sur overlay, ou bouton X
- Le polling du statut s'arrête automatiquement quand le scan est terminé ou en erreur
- Le PDF est converti en File object pour l'injection dans l'input file

