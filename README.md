# Agent de Scan Local

Agent de scan local indépendant en Java 17, utilisant NAPS2 pour scanner des documents et générer des PDFs.

## Caractéristiques

- **Standalone** : Fonctionne indépendamment, sans backend
- **API REST locale** : Expose une API REST sur `http://localhost:7070`
- **Interface web** : Interface web locale accessible via `/ui`
- **Base de données SQLite** : Stockage embarqué des sessions de scan
- **Intégration NAPS2** : Utilise NAPS2 via CLI pour les scans
- **Cross-platform** : Windows et Linux

## Prérequis

- **Java 17** ou supérieur
- **NAPS2** installé et accessible dans le PATH
- Windows ou Linux

## Installation

1. Compiler le projet :
```bash
mvn clean package
```

2. Le JAR exécutable sera généré dans `target/scanner-cin-1.0.0.jar`

## Utilisation

### Démarrage

#### Windows
```batch
start-windows.bat
```

Ou avec options :
```batch
start-windows.bat --port=8080
```

#### Linux
```bash
chmod +x start-linux.sh
./start-linux.sh
```

Ou avec options :
```bash
./start-linux.sh --port=8080
```

### Configuration

La configuration par défaut est dans `src/main/resources/application.properties`. Vous pouvez créer un fichier de configuration externe :

```properties
# Serveur HTTP
server.port=7070
server.host=127.0.0.1

# Base de données
database.path=./data/scanner.db

# NAPS2
naps2.command=naps2
naps2.timeout.seconds=300

# Stockage PDF
storage.base.path=./scans
storage.cleanup.enabled=false
storage.cleanup.days=30

# Logging
logging.level=INFO
logging.file.enabled=false
logging.file.path=./logs/scanner-agent.log
```

Puis démarrer avec :
```bash
java -jar scanner-agent.jar --config=/chemin/vers/config.properties
```

## API REST

Base URL : `http://localhost:7070/api`

### Endpoints

#### POST /api/scans
Créer une nouvelle session de scan.

**Request Body** :
```json
{
  "scannerName": "string (optionnel)",
  "options": {
    "resolution": 300,
    "colorMode": "Color|Grayscale|BlackAndWhite",
    "pageSize": "A4|Letter|..."
  }
}
```

**Response 201** :
```json
{
  "scanId": "uuid",
  "status": "PENDING",
  "createdAt": "2024-01-01T12:00:00Z"
}
```

#### POST /api/scans/{scanId}/start
Démarrer le scan pour une session.

**Response 200** :
```json
{
  "scanId": "uuid",
  "status": "RUNNING",
  "startedAt": "2024-01-01T12:00:01Z"
}
```

#### GET /api/scans/{scanId}
Récupérer le statut d'une session.

**Response 200** :
```json
{
  "scanId": "uuid",
  "status": "COMPLETED|RUNNING|FAILED|CANCELLED|PENDING",
  "createdAt": "2024-01-01T12:00:00Z",
  "startedAt": "2024-01-01T12:00:01Z",
  "completedAt": "2024-01-01T12:00:30Z",
  "errorMessage": "string (si FAILED)",
  "pdfPath": "string (si COMPLETED)",
  "pdfSize": 12345
}
```

#### GET /api/scans/{scanId}/pdf
Télécharger le PDF généré.

**Response 200** : Stream binaire PDF

#### DELETE /api/scans/{scanId}
Annuler un scan en cours.

#### GET /api/scans
Lister l'historique des scans.

**Query Parameters** :
- `limit` (optionnel, défaut: 50)
- `offset` (optionnel, défaut: 0)
- `status` (optionnel): Filtrer par statut

#### GET /api/health
Health check du système.

**Response 200** :
```json
{
  "status": "UP|DEGRADED",
  "naps2Available": true,
  "databaseConnected": true
}
```

#### GET /api/devices
Lister les périphériques de scan disponibles.

**Query Parameters** :
- `driver` (optionnel) : Filtrer par driver (`wia`, `twain`, `escl`, `sane`, `apple`)

**Response 200** :
```json
{
  "devices": [
    {
      "name": "HP ScanJet Pro 2000 s2 (USB)",
      "driver": "wia",
      "id": "wia_hp_scanjet_pro_2000_s2_usb"
    },
    {
      "name": "Canon MP495",
      "driver": "twain",
      "id": "twain_canon_mp495"
    }
  ],
  "count": 2
}
```

**Response 503** : NAPS2 non disponible

**Response 500** : Erreur serveur

## Interface Web

L'interface web est accessible sur `http://localhost:7070/ui`

Fonctionnalités :
- Créer et démarrer des scans
- Visualiser l'historique des scans
- Télécharger les PDFs générés
- Vérifier le statut du système

## Documentation API (Swagger)

La documentation interactive de l'API est disponible via Swagger UI :

**URL** : `http://localhost:7070/swagger-ui`

Fonctionnalités :
- Documentation interactive de tous les endpoints
- Tester les endpoints directement depuis le navigateur
- Voir les schémas de requêtes et réponses
- Spécification OpenAPI disponible sur `/api/openapi.json`

## Composant Web Embarqué

Le projet inclut un composant web réutilisable dans le dossier `components/` qui permet d'intégrer facilement la fonctionnalité de scan dans vos applications web.

### Structure

```
components/
├── index.html              # Page de test avec input file
├── scan-component.js       # Web component principal (modal)
├── scan-api.js            # Client API pour communiquer avec le scanner
├── scan-styles.css        # Styles isolés du component
└── README.MD              # Documentation détaillée du composant
```

### Utilisation rapide

1. Copier les fichiers du dossier `components/` dans votre projet web
2. Inclure les scripts dans votre page HTML :

```html
<script src="scan-api.js"></script>
<script src="scan-component.js"></script>
```

3. Utiliser le composant :

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

### Fonctionnalités du composant

- **Chargement dynamique des scanners** : Liste automatiquement les scanners disponibles via l'endpoint `/api/devices`
- **Synchronisation driver ↔ scanners** : Recharge automatiquement la liste des scanners quand le driver change
- **Configuration complète** : Taille de papier, mode couleur, résolution, source, driver
- **Aperçu PDF** : Visualisation du PDF scanné avant validation
- **Injection automatique** : Injection du PDF dans un input file cible
- **Isolation complète** : Utilise Shadow DOM pour éviter les conflits de styles

Pour plus de détails, consultez le fichier `components/README.MD`.

## Dépannage

### NAPS2 non détecté

Vérifiez que NAPS2 est installé et accessible :
```bash
naps2 --version
```

Si NAPS2 n'est pas dans le PATH, configurez le chemin dans `application.properties` :
```properties
naps2.command=/chemin/vers/naps2
```

### Port déjà utilisé

Changez le port dans la configuration ou via l'argument CLI :
```bash
java -jar scanner-agent.jar --port=8080
```

### Erreurs de base de données

Vérifiez les permissions d'écriture dans le répertoire de la base de données (par défaut `./data/`).

## Architecture

- **ScannerAgent** : Point d'entrée principal
- **HttpServer** : Serveur HTTP embarqué (Jetty)
- **ScanController** : Contrôleur REST API pour les scans
- **DeviceController** : Contrôleur REST API pour la liste des périphériques
- **HealthController** : Contrôleur REST API pour le health check
- **ScanService** : Logique métier des scans
- **Naps2Service** : Intégration avec NAPS2 CLI (scans et liste des périphériques)
- **ScanSessionRepository** : Accès à la base de données SQLite
- **components/** : Composant web réutilisable pour intégration dans des applications web

## Limitations

- Un seul scan peut être exécuté à la fois
- Nécessite NAPS2 installé localement
- Pas d'authentification (localhost uniquement)
- Pas de communication avec un backend distant

## Licence

Propriétaire - Tous droits réservés

