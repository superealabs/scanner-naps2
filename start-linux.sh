#!/bin/bash

echo "========================================"
echo "Agent de Scan Local"
echo "========================================"
echo ""

# Vérifier Java 17
if ! command -v java &> /dev/null; then
    echo "ERREUR: Java n'est pas installé ou non accessible dans le PATH"
    echo "Veuillez installer Java 17 ou plus récent"
    exit 1
fi

# Vérifier version Java
JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | sed '/^1\./s///' | cut -d'.' -f1)
echo "Version Java détectée: $JAVA_VERSION"
echo ""

# Configuration par défaut
PORT=7070
CONFIG_FILE=""

# Parser les arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --port)
            PORT="$2"
            shift 2
            ;;
        --config)
            CONFIG_FILE="$2"
            shift 2
            ;;
        --help)
            echo "Usage: ./start-linux.sh [options]"
            echo ""
            echo "Options:"
            echo "  --port PORT        Port HTTP (défaut: 7070)"
            echo "  --config PATH      Chemin vers fichier de configuration"
            echo "  --help             Afficher cette aide"
            echo ""
            exit 0
            ;;
        *)
            echo "Option inconnue: $1"
            echo "Utilisez --help pour voir les options disponibles"
            exit 1
            ;;
    esac
done

echo "Démarrage de l'agent de scan..."
echo "Port: $PORT"
if [ -n "$CONFIG_FILE" ]; then
    echo "Fichier de configuration: $CONFIG_FILE"
    java -jar target/scanner-cin-1.0.0.jar --port=$PORT --config="$CONFIG_FILE"
else
    java -jar target/scanner-cin-1.0.0.jar --port=$PORT
fi

EXIT_CODE=$?
if [ $EXIT_CODE -ne 0 ]; then
    echo ""
    echo "ERREUR: L'agent de scan s'est arrêté avec une erreur"
    exit $EXIT_CODE
fi

