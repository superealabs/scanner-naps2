#!/usr/bin/env bash
# ============================================================================
#  env-java17.sh - Configure Java 17 pour le shell COURANT uniquement.
#
#  Usage :   source ./env-java17.sh        (ou)   . ./env-java17.sh
#
#  NE PAS executer (./env-java17.sh) : cela ne configurerait qu'un sous-shell
#  jetable, et le shell courant resterait inchange.
#
#  Resolution du JDK 17 (premier valide gagne) :
#      1. variable d'environnement JDK17_HOME
#      2. fichier jdk17.properties (cle JDK17_HOME=...)
#      3. auto-detection (/usr/lib/jvm, update-alternatives, ...)
# ============================================================================

# --- Garde : ce script doit etre SOURCE, pas execute ----------------------
if [ "${BASH_SOURCE[0]}" = "${0}" ]; then
    echo "ERROR: ce script doit etre SOURCE, pas execute." >&2
    echo "  Utilisez :  source ./env-java17.sh    (ou)   . ./env-java17.sh" >&2
    exit 1
fi

# A partir d'ici on est source -> utiliser 'return', jamais 'exit'.
# Pas de 'set -e' : cela tuerait le shell interactif de l'utilisateur.

_j17_validate() {
    # $1 = home candidat ; retourne 0 si c'est un JDK 17 valide.
    [ -n "$1" ] || return 1
    [ -x "$1/bin/java" ]  || return 1
    [ -x "$1/bin/javac" ] || return 1   # rejette les JRE seuls
    local major
    major=$("$1/bin/java" -version 2>&1 | head -1 | sed -E 's/.*version "([0-9]+).*/\1/')
    [ "$major" = "17" ]
}

_j17_resolve() {
    # Ecrit le home resolu sur stdout ; retourne 0 si trouve.
    local cfg_dir val c

    # 1) override via variable d'environnement
    if [ -n "$JDK17_HOME" ]; then
        if _j17_validate "$JDK17_HOME"; then printf '%s\n' "$JDK17_HOME"; return 0; fi
        echo "[WARN] JDK17_HOME=\"$JDK17_HOME\" invalide - ignore." >&2
    fi

    # 2) fichier de config (relatif au script)
    cfg_dir=$(cd "$(dirname "${BASH_SOURCE[0]}")" 2>/dev/null && pwd)
    if [ -n "$cfg_dir" ] && [ -f "$cfg_dir/jdk17.properties" ]; then
        val=$(grep -E '^[[:space:]]*JDK17_HOME[[:space:]]*=' "$cfg_dir/jdk17.properties" \
              | tail -1 | cut -d= -f2- | tr -d '\r' \
              | sed -E 's/^[[:space:]]+//; s/[[:space:]]+$//')
        if [ -n "$val" ]; then
            if _j17_validate "$val"; then printf '%s\n' "$val"; return 0; fi
            echo "[WARN] jdk17.properties : \"$val\" invalide - ignore." >&2
        fi
    fi

    # 3) auto-detection : dossiers d'install connus
    for c in \
        /usr/lib/jvm/java-17-* /usr/lib/jvm/java-1.17.0-* \
        /usr/lib/jvm/temurin-17* /usr/lib/jvm/zulu17* /usr/lib/jvm/zulu-17* \
        /usr/lib/jvm/jdk-17* /usr/lib/jvm/*-17-* /usr/lib/jvm/*17* \
        /opt/java/*17* "$HOME"/.sdkman/candidates/java/17* \
        /Library/Java/JavaVirtualMachines/*17*/Contents/Home ; do
        [ -d "$c" ] || continue
        if _j17_validate "$c"; then printf '%s\n' "$c"; return 0; fi
    done

    # update-java-alternatives (Debian/Ubuntu) : colonnes = nom priorite chemin
    if command -v update-java-alternatives >/dev/null 2>&1; then
        while read -r _name _prio path _rest; do
            [ -n "$path" ] || continue
            if _j17_validate "$path"; then printf '%s\n' "$path"; return 0; fi
        done < <(update-java-alternatives -l 2>/dev/null | grep -E '17')
    fi

    # update-alternatives : remonter .../bin/java -> home
    if command -v update-alternatives >/dev/null 2>&1; then
        for c in $(update-alternatives --list java 2>/dev/null); do
            c=$(dirname "$(dirname "$c")")
            if _j17_validate "$c"; then printf '%s\n' "$c"; return 0; fi
        done
    fi

    return 1
}

_j17_main() {
    local resolved
    if resolved=$(_j17_resolve); then
        [ -n "$_JDK17_ORIG_PATH" ] || export _JDK17_ORIG_PATH="$PATH"
        export JAVA_HOME="$resolved"
        export PATH="$JAVA_HOME/bin:$_JDK17_ORIG_PATH"
        echo "[OK] JAVA_HOME=$JAVA_HOME"
        "$JAVA_HOME/bin/java" -version
        return 0
    fi
    echo "[ERROR] Aucun JDK 17 trouve." >&2
    echo "  Solution : cp jdk17.properties.example jdk17.properties" >&2
    echo "             puis renseignez  JDK17_HOME=/chemin/vers/jdk-17" >&2
    echo "  (ou)   export JDK17_HOME=...   puis   source ./env-java17.sh" >&2
    return 1
}

_j17_main
_j17_status=$?

# Nettoyage des fonctions pour garder le shell propre.
unset -f _j17_validate _j17_resolve _j17_main 2>/dev/null

return $_j17_status
