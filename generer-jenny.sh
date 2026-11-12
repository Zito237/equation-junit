#!/usr/bin/env bash
# Pour comparaison seulement : les tests JUnit utilisent le décorateur @PairwiseSource.
# Ce script produit les cas pairwise du même modèle (3 paramètres × 9 classes) avec l'outil externe jenny.
set -e
cd "$(dirname "$0")"
gcc -O2 -o outils/jenny outils/jenny.c
./outils/jenny 9 9 9 > outils/jenny-9-9-9.txt
echo "jenny : $(wc -l < outils/jenny-9-9-9.txt) cas (le décorateur JUnit en produit 81, le minimum)"
