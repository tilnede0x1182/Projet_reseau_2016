#!/bin/bash

# Répertoire de base
PROJECT_DIR="."
OUTPUT_FILE="$PROJECT_DIR/projet.txt"
SCRIPT_NAME="script.sh"

# Extensions autorisées
EXTENSIONS=("java" "txt")

# Vider le fichier de sortie
> "$OUTPUT_FILE"

# Boucle sur chaque extension
for ext in "${EXTENSIONS[@]}"; do
  find "$PROJECT_DIR" -type f -name "*.${ext}" | while read -r file; do
    # Exclure le script lui-même
    if [[ "$(basename "$file")" != "$SCRIPT_NAME" ]]; then
      echo "# $file" >> "$OUTPUT_FILE"
      echo >> "$OUTPUT_FILE"
      cat "$file" >> "$OUTPUT_FILE"
      echo -e "\n" >> "$OUTPUT_FILE"
    fi
  done
done

# Message de fin
echo "Fichier '$OUTPUT_FILE' généré avec succès."
