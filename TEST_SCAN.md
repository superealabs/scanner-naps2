# Test de Scan NAPS2

Ce fichier explique comment tester le scanner avec NAPS2.

## Fichier de test

Le fichier `TestNASP2.java` permet de tester un scan A4 en couleur avec NAPS2.

## Prérequis

1. **NAPS2 installé** et accessible dans le PATH
2. **Java 17** ou supérieur
3. **Scanner connecté** et prêt à l'emploi
4. **Document A4** placé dans le scanner

## Compilation et exécution

### Windows

```bash
# Compiler
javac TestNASP2.java

# Exécuter
java TestNASP2
```

### Linux

Modifiez la ligne dans `TestNASP2.java` :
```java
String naps2Command = "naps2"; // Au lieu de "naps2.console.exe"
```

Puis compilez et exécutez :
```bash
javac TestNASP2.java
java TestNASP2
```

## Ce que fait le test

1. **Vérifie NAPS2** : S'assure que NAPS2 est disponible
2. **Construit la commande** : Prépare la commande avec options A4 couleur (300 DPI)
3. **Lance le scan** : Exécute NAPS2 pour scanner le document
4. **Vérifie le résultat** : Contrôle que le PDF a été généré

## Paramètres du scan

- **Résolution** : 300 DPI
- **Mode couleur** : Couleur
- **Taille** : A4
- **Format de sortie** : PDF

## Résultat attendu

Le test génère un fichier PDF dans le répertoire `./test-scans/` avec un nom unique basé sur la date/heure.

Exemple : `test-scans/test-20240115-163045.pdf`

## Dépannage

### NAPS2 non trouvé

Si vous obtenez "NAPS2 n'est pas disponible", vérifiez :

1. NAPS2 est installé
2. Le chemin est dans le PATH, ou modifiez `naps2Command` dans le code avec le chemin complet :
   ```java
   String naps2Command = "C:\\Program Files\\NAPS2\\naps2.console.exe";
   ```

### Erreur de scan

- Vérifiez que le scanner est allumé et connecté
- Vérifiez qu'un document est placé dans le scanner
- Vérifiez les options NAPS2 CLI avec : `naps2.console.exe --help`

### Options NAPS2 CLI

Les options exactes peuvent varier selon la version de NAPS2. Pour voir les options disponibles :

```bash
naps2.console.exe --help
```

## Notes

- Le test attend 3 secondes avant de lancer le scan (temps pour placer le document)
- Le timeout est de 5 minutes maximum
- Les sorties de NAPS2 sont affichées en temps réel

