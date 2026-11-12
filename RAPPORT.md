# Rapport — justification des mesures d'erreur et des tolérances

Notations du cours : ε = ulp(1) = 2⁻⁵² ≈ 2,2·10⁻¹⁶ ; κ = conditionnement ; Tol = c·κ·ε (slide 67).

## Conditionnement d'une racine

Pour une racine simple r de a·x² + b·x + c, une perturbation relative ε des coefficients
déplace r d'environ κ·ε, avec :

    κ(r) = (|a|·r² + |b|·|r| + |c|) / (|r| · |2ar + b|)        et |2ar + b| = √Δ

Il est calculé en BigDecimal (`ReferenceExacte.kappa`), donc sans débordement.

## Tolérances par oracle

| Oracle | N (mesure) | Tol | Justification |
|---|---|---|---|
| Référence (81 cas pairwise par domaine) | erreur relative \|y − RA\| / \|RA\|, mixte sous MIN_NORMAL | 16·κ·ε, écrite dans resultats-attendus.csv pour chaque racine | un solveur stable fait une douzaine d'arrondis, chacun ≤ ε/2 ; c = 16 couvre ce cumul. Ex. (−2,5 ; −2,5 ; −1e-8) : κ ≈ 2, Tol ≈ 7·10⁻¹⁵ ; la formule naïve (1,2·10⁻⁸) est rejetée |
| Référence, racine double (GN, Z, Z) | erreur mixte | 16·√ε ≈ 2,4·10⁻⁷ | κ infini : seule la moitié des chiffres est garantie (slide 67) ; 1 ou 2 racines acceptées |
| Différentiel | mixte : \|y − ŷ\| ≤ 16·κ·ε·\|ŷ\| + 2·MIN_VALUE | 16·κ·ε par racine | la partie absolue 2·4,9·10⁻³²⁴ ne sert que sous MIN_NORMAL, où les double sont espacés de façon fixe (slide 66) ; ŷ = ±∞ ⇒ y doit être le même infini |
| Différentiel, zone Δ ≈ 0 | distance à −b/2a | 4·√(16ε(b² + \|4ac\|)) / \|2a\| | si \|Δ\| ≤ 16ε(b² + \|4ac\|), l'arrondi des données peut changer le nombre de racines : 0, 1 ou 2 sont défendables |
| Propriété : résidu | \|ar² + br + c\| / (\|a\|r² + \|b\|\|r\| + \|c\|) | 8·ε | erreur inverse d'un algorithme stable (slide 78) ; calcul exact en BigDecimal |
| Propriété : Viète | \|a·r1·r2 − c\| / \|c\| ; \|a(r1 + r2) + b\| / (\|a\|(\|r1\| + \|r2\|) + \|b\|) | 8·ε | r1 = q/a et r2 = c/q : le produit ne dépend pas de l'erreur sur q (2 arrondis) |
| Homothétie | égalité numérique stricte | 0 | multiplier par 2¹⁰ ou 2⁻¹⁰ ne change que l'exposant : aucun arrondi (slide 76). Avec k = 10, il faudrait une tolérance |
| Métamorphique opposé, signe | égalité stricte | 0 | la négation est exacte |
| Métamorphique inverse | erreur relative | 2·16·κ·ε + 2ε | 1/r ajoute un arrondi ; κ estimé sur les racines OBTENUES (pas de RA) |
| Construction | mixte | 16·κ·ε ; racine double : 16·√ε | b = −(r1 + r2) et c = r1·r2 sont arrondis (slide 80) : κ des racines choisies absorbe cette perturbation |

## Domaine de validité et verdict INCONCLUSIF

Un oracle n'est appliqué que là où il a un sens. Hors de ce domaine, le verdict est INCONCLUSIF
(`Assumptions.assumeTrue`), pas PASS :

- Homothétie : k·a, k·b ou k·c déborde ou devient sous-normal ;
- Métamorphique inverse : c = 0, ou une racine nulle, infinie ou sous-normale ;
- Résidu : racine infinie ou sous-normale, ou racine 0 alors que c ≠ 0. Il peut s'agir d'un sous-dépassement
  légitime ou d'une absorption, et la propriété seule ne peut pas trancher ;
- Construction : r1·r2 déborde ou sous-dépasse.

## Le cas où l'oracle par homothétie n'est pas intéressant

Sur les 81 cas pairwise, la formule naïve est fausse dans 29 cas sans produire de NaN. Par exemple, sur
(−2,5 ; −2,5 ; −1e-8), elle rend −4,0000000645·10⁻⁹ au lieu de −4,000000016·10⁻⁹, ce qui représente une
erreur relative de 1,2·10⁻⁸ pour une tolérance de 7·10⁻¹⁵. Multipliée par 1024, l'équation conduit exactement aux
mêmes opérations, à l'exposant près, donc à la même erreur. L'homothétie est respectée et l'oracle répond PASS
sur un résultat faux, dans les 29 cas. Il ne détecte la formule naïve que lorsqu'elle produit NaN.

Conclusion du cours (slide 84) : une version fausse mais cohérente passe les relations métamorphiques. Il faut
combiner plusieurs oracles, et ici, seuls le différentiel, la référence, le résidu et Viète mesurent l'exactitude.

## Couverture de code et tests de mutation (séances des pages 85 à 141)

Les tests sont exécutés avec `SolveurRobuste` par défaut. Le projet utilise JaCoCo (séance sur la couverture,
p. 130-132) et PIT (tests de mutation, p. 138-139) pour mesurer deux aspects différents de la suite JUnit :
le code exécuté et la capacité des assertions à détecter des modifications du programme.

Commandes à lancer à la racine avec JDK 17 et Maven 3.9+ :

```bash
mvn clean verify
mvn clean test-compile org.pitest:pitest-maven:mutationCoverage
```

Le rapport JaCoCo est dans `target/site/jacoco/index.html`. Son entrée `SolveurRobuste` indique
48/50 lignes couvertes (96 %) et 16/18 branches couvertes (88,9 %). Les seuils du quality gate,
90 % des lignes et 80 % des branches, sont donc satisfaits. Le rapport global peut afficher un taux
plus bas car il inclut les solveurs alternatifs, non exécutés quand la configuration par défaut est utilisée.

Le rapport PIT est dans `target/pit-reports/index.html` et couvre `SolveurRobuste`, `Etendu` et `Coefficient`.
Les résultats présents sont 50 mutants tués sur 101 (score de mutation : 50 %) et 50 mutants tués sur
86 mutants effectivement couverts (force des tests : 58 %). Les 15 mutants non couverts ne sont pas dans
le dénominateur de la force des tests. Ce score révèle que plusieurs modifications échappent encore aux
tests : une couverture de lignes élevée ne suffit donc pas à établir leur pouvoir de détection. Les rapports
HTML, CSV et XML servent à examiner chaque mutant survivant et à ajouter des tests ciblés.

Il n'y a pas de seuil PIT imposé : le but de cette étape est d'obtenir le rapport et d'évaluer le score
observé, pas de faire échouer la génération du rapport sur un objectif de mutation qui n'a pas été demandé.
Ces chiffres proviennent des rapports déjà présents dans `target/` et doivent être actualisés avec les
commandes ci-dessus après toute modification du code ou des tests.

## Correspondance avec l'ISTQB

Vocabulaire du Syllabus ISTQB Foundation Level (v4.0) et du syllabus Test Analyst (niveau avancé),
rapporté à ce qui est réalisé dans le projet.

| Notion ISTQB | Réalisation dans le projet |
|---|---|
| Test unitaire (test de composant), boîte noire | les solveurs sont testés par leur seule interface `racines(a, b, c)`, chargée par réflexivité : les tests ignorent l'implémentation |
| Partitions d'équivalence | 9 classes par domaine : `ClassesEquivalence` (flottants IEEE 754) et `ClassesEntiers` (int) ; un représentant par partition, justifié dans `@ClasseEquivalence` |
| Analyse des valeurs limites | entiers : Integer.MIN_VALUE, MIN + 1, −1, 0, +1, MAX − 1, MAX (limites et voisines, méthode à 2 et 3 valeurs) ; flottants : limites de débordement (1e200, x² > MAX_VALUE) et de sous-dépassement (1e-200, x² < MIN_VALUE), frontière a = 0 |
| Test combinatoire, couverture des paires (pairwise) | `@PairwiseSource` + `GenerateurPairwise` (carré latin, IPOG) : 81 cas au lieu de 729, 243/243 paires couvertes ; la couverture est recomptée à chaque exécution et affichée dans le journal |
| Conditions de test, cas de test, données de test | `@CasDeTest` écrit sur chaque méthode le scénario (SC), les données (DE), l'oracle (O), le résultat attendu (RA), la mesure (N) et la tolérance (Tol) |
| Oracle de test | référence (RA figés), différentiel (implémentation BigDecimal), propriétés (résidu, relations de Viète), implicite (pas de plantage, contrat) |
| Test métamorphique | MR1 opposé, MR2 signe, MR3 inverse, homothétie ; et le cas où l'oracle n'est pas intéressant (formule naïve fausse mais cohérente) |
| Résultat attendu et résultat obtenu | `JournalCas` : pour chaque cas, entrées, sortie obtenue, RA et verdict, dans `target/journal-cas-<solveur>.md` |
| Verdict, rapport de test | 4 verdicts (PASS, FAIL, ERROR, INCONCLUSIF) agrégés par `RapportVerdicts` ; `TableauDetection` compare les trois solveurs |
| Test de régression, automatisation | toute la suite se rejoue contre n'importe quel solveur avec `mvn test -Dsolveur.classe=...`, sans modifier les tests |
| Principe « les tests montrent la présence de défauts » | défauts trouvés : formule naïve (flottants et entiers), version stable du cours sur (1e-200, 0, 1e200), et un écart d'un ulp dans notre solveur robuste, corrigé |
| Principe « les tests exhaustifs sont impossibles » | 729 combinaisons de classes par domaine (et 2⁹⁶ triplets d'int) : le pairwise est la réduction choisie, justifiée par la couverture des paires |
| Principe « regroupement des défauts » | les défauts se concentrent dans les classes extrêmes (GN, GP, TPN, TPP, IMIN, IMAX) : c'est là que les oracles échouent |

## Couverture et mutation

### Couverture JaCoCo (`mvn clean verify`, solveur robuste)

| Classe | Lignes, pairwise seul | Branches, pairwise seul | Lignes, avec `BoiteBlancheTest` | Branches, avec `BoiteBlancheTest` |
|---|---|---|---|---|
| SolveurRobuste | 48/50 (96 %) | 16/18 (89 %) | **50/50 (100 %)** | **18/18 (100 %)** |
| Coefficient | 8/8 | 3/4 | **8/8** | **4/4** |
| Etendu | 35/37 | 19/34 (56 %) | 36/37 | 25/34 (74 %) |

Le rapport JaCoCo a montré 4 branches de `SolveurRobuste` que les 81 cas pairwise n'atteignent jamais :
b = 0 avec −c/a < 0 (pas de racine), un coefficient sous-normal, un coefficient NaN ou infini, et le
retour aux double au-delà de 2^±2000. `BoiteBlancheTest` les atteint avec des entrées choisies en lisant
le code (boîte blanche), toujours par réflexivité et avec `@MethodSource`.

Les 9 branches restantes d'`Etendu` sont **inatteignables depuis le solveur** : tests de zéro dans `plus`,
`foisPuissanceDeDeux`, `racineCarree` et `enDouble`, division par zéro, racine d'un négatif, et la méthode
`fois`, que le solveur n'appelle jamais. Ce sont des gardes défensives d'une classe utilitaire. C'est la
limite de la couverture : 100 % des lignes du solveur ne prouve pas que les tests sont bons. D'où la mutation.

### Test de mutation PIT (`mvn test-compile org.pitest:pitest-maven:mutationCoverage`)

PIT modifie le bytecode du solveur robuste, d'`Etendu` et de `Coefficient` (mutateurs par défaut : bornes de
comparaison, négation de conditions, opérateurs arithmétiques, valeurs de retour…), puis rejoue la suite JUnit
sur chaque mutant. Un mutant est **tué** si au moins un test échoue.

| Classe | Mutants | Tués avant | Tués après les solutions manufacturées | Survivants |
|---|---|---|---|---|
| SolveurRobuste | 36 | 27 (75 %) | **34 (94 %)** | 2, équivalents |
| Coefficient | 3 | 3 | 3 (100 %) | 0 |
| Etendu | 61 | 29 | 30 | 25 + 6 non couverts |
| **Total** | 100 | 59 % | **67 %** (force des tests 71 %) | |

**Ce que la mutation a appris que la couverture ne disait pas.** Avec 96 % de lignes couvertes, 9 mutants
du solveur survivaient. Les oracles à tolérance (16·κ·ε, zone Δ ≈ 0 où 0, 1 ou 2 racines sont acceptées)
étaient trop indulgents pour eux :

- `d == 0` supprimé ou `d < 0` changé en `d <= 0` : une racine double devient deux racines égales, ou disparaît ;
- `b == 0` supprimé dans la branche c = 0 : 0 devient [−0, 0] ;
- `+` changé en `−` dans Δ = (p − q) + (dp − dq), ou `*` changé en `/` dans b² et 4ac : la correction de
  Kahan compense partiellement l'erreur, invisible hors de la zone Δ ≈ 0.

Ils sont tués par 5 **solutions manufacturées** (séance 6) : des équations à coefficients exacts dont on
connaît les racines exactes, comparées sans tolérance (`BoiteBlancheTest.solutionExacte`) :
(x − 1)², (x + 2)², 2x², l'exemple de Kahan (94906265,625 ; −189812534 ; 94906268,375), où b² > 2^53 efface
Δ = 7,5625, et (x − 1)(x − 2) à coefficients sous-normaux. La version naïve en échoue 3, la version du cours 2.

**Mutants équivalents (non tuables).** Ils ne changent aucun résultat possible :

- `SolveurRobuste` l. 53, `t.signe() < 0` → `<= 0` : t = −c/a ne vaut jamais 0, car c ≠ 0 dans cette branche ;
- `SolveurRobuste` l. 84, `b > 0` → `b >= 0` : b = 0 est traité plus haut ;
- `Etendu.exposant` et `Etendu.of` : la normalisation de la mantisse ne sert qu'à éviter les débordements.
  Un exposant faux mais compensé (m·2^e garde la même valeur) donne le même résultat, d'où des mutants
  sur les calculs d'exposant ou sur les bornes (x = MIN_NORMAL) qui survivent sans être des défauts ;
- les gardes inatteignables d'`Etendu` vues plus haut : leurs mutants survivent ou ne sont pas couverts.

Score de mutation hors mutants équivalents et code inatteignable : 34/34 sur `SolveurRobuste`.
