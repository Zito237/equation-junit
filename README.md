# Devoir 2 (version JUnit) — Équation du second degré : pairwise, réflexivité et oracles

Ce projet reprend le devoir pairwise avec jenny et ajoute les demandes du professeur et le cours
INF563 (pages 30 à 84, puis les séances sur la couverture et les mutations, pages 85 à 141).

## 1. Ce que demande le professeur, et où le trouver

| Demande | Réalisation |
|---|---|
| 9 classes : grand ±, zéro, très petit (tp−, tp+), normal (n−, n+), petit (p−, p+) | `vv/classes/ClassesEquivalence.java` : **une fonction par classe**, chacune annotée `@ClasseEquivalence` |
| 9 paramètres critiques par domaine : entiers machine et flottants IEEE 754 | flottants : `ClassesEquivalence` ; entiers : `vv/classes/ClassesEntiers.java` (MIN, MIN + 1, −7, −1, 0, 1, 5, MAX − 1, MAX). `PairwiseEntiersTest` et `ReferenceEntiersTest` rejouent les oracles sur les entiers grâce à `@Domaine` |
| Tests formalisés avec JUnit, décorateur, outil pairwise | `@PairwiseSource` (`vv/pairwise`) : annotation JUnit 5 qui découvre les 9 fonctions par réflexivité et génère 81 cas couvrant les 243 paires, avec **notre propre générateur All-Pairs en Java pur** (`GenerateurPairwise` : carré latin, IPOG). JUnit 5 fournit `@ParameterizedTest`, pas de générateur pairwise. **Tous les tests du solveur reçoivent leurs données de ce décorateur** : aucun cas écrit à la main |
| Voir tous les cas exécutés : entrées et sorties | `JournalCas` : à la fin de chaque exécution, tableau de **chaque cas** (classes, a, b, c, sortie du solveur, résultat attendu, verdict) et couverture réelle (243/243 paires), écrit dans `target/journal-cas-<solveur>.md` |
| Réponses sous forme d'ArrayList | les trois solveurs renvoient `ArrayList<Double>` ; le chargeur vérifie ce contrat |
| Chaque cas de test porte son annotation | `@CasDeTest` sur chaque méthode : modèle formel SC, DE, O, RA, N, Tol (slide 59) ; chaque cas généré porte le code de ses classes (`a∈GN(-1.0E200) b∈Z(0.0) c∈PP(1.0E-8)`) |
| Tester par réflexivité, sans connaître la classe, instanciation dynamique | `vv/reflexion/SolveurReflexif.java` : `Class.forName`, `newInstance`, `Method.invoke` ; `ArchitectureTest` vérifie qu'aucun test n'importe un solveur |
| Oracle par homothétie | `OracleHomothetique` + `HomothetieTest` : g(k·a, k·b, k·c) = g(a, b, c), k = 2¹⁰, égalité stricte |
| Oracle métamorphique | `OracleMetamorphique` : opposé, signe, inverse |
| Validation par reconstruction polynomiale (Viète) | `OracleProprietes` : a·r1·r2 = c et a·(r1 + r2) = −b en calcul exact ; `ConstructionTest` : équation fabriquée à partir de (x − r1)(x − r2) |
| Cas où l'oracle n'est pas intéressant | `HomothetieTest.casNonInteressant`, sur les 81 cas pairwise : dans 29 cas, la formule naïve est fausse et l'homothétie la déclare pourtant correcte |
| a = 0, b et c ≠ 0 → exception | `IllegalArgumentException("a = 0 : ce n'est pas une équation du second degré")` ; les 9 cas pairwise où a = 0 (dont 8 avec b ou c non nul) vérifient l'exception **et son message** |
| a, b ou c est un caractère ou une chaîne → exception | chaque solveur a aussi `racines(String, String, String)` ; `Coefficient.lire` lève `NumberFormatException("a = « abc » : ce n'est pas un nombre, veuillez entrer un nombre")`. `CaracteresTest` place 11 textes invalides (« a », « x », « ? », « abc », « deux », « 2,5x », « 1..2 », vide, blanc, NaN, Infinity) tour à tour en a, b et c : 34 cas, sans aucune saisie au clavier |
| Grand positif inclus | classe GP = 1e200 : 9 fois sur a, 9 fois sur b, 9 fois sur c dans les 81 cas |
| Principes ISTQB | `RAPPORT.md`, section « Correspondance avec l'ISTQB » |

## 2. Lancer dans VS Code

Prérequis : JDK 17+ et l'extension **Extension Pack for Java**, qui gère Maven et JUnit.

1. `Fichier > Ouvrir le dossier…` → `equation-junit`. Au premier chargement, VS Code télécharge JUnit 5.11.4 par Maven.
2. **Onglet Test** (fiole) → *Run Tests*. Chaque cas apparaît avec ses classes et ses valeurs, en vert ou en rouge.
   Pour choisir le solveur testé : flèche à côté de *Run* → *Select Configuration* → *Solveur robuste*, *Solveur naïf* ou *Solveur stable du cours*.
3. **Exécuter et déboguer** → *Tableau de détection (3 solveurs)* : exécute toute la suite contre les trois solveurs et écrit `target/tableau-detection.md`.

En terminal (Maven 3.9+ requis : [télécharger Maven](https://maven.apache.org/download.cgi), « Binary zip archive » ; `JAVA_HOME` doit pointer vers un JDK 17+) :

```bash
mvn test                       # solveur robuste (par défaut)
mvn test -Pnaif                # même suite, formule du lycée
mvn test -Pcours               # même suite, version stable du slide 54
mvn test -Dsolveur.classe=mon.paquet.MaClasse   # n'importe quelle classe, sans recompiler les tests
mvn test-compile exec:java     # tableau de détection
./generer-jenny.sh             # comparaison : cas de l'outil externe jenny (gcc requis), non utilisés par les tests
```

Chaque `mvn test` affiche :

- le **journal des cas** : pour chaque domaine, les 81 cas avec a, b, c, la sortie du solveur, le résultat attendu
  et le verdict, précédés de la couverture (« 81 cas exécutés (exhaustif : 729) ; 243/243 paires de classes couvertes ») ;
  écrit dans `target/journal-cas-<solveur>.md` ;
- le tableau des quatre verdicts (PASS, FAIL, ERROR, INCONCLUSIF) par cas de test, écrit dans
  `target/rapport-verdicts-<solveur>.md`.

Extrait du journal avec le solveur naïf, domaine entiers :

| # | Classes (a, b, c) | a | b | c | Sortie du solveur | Résultat attendu (RA) | Verdict |
|---|---|---|---|---|---|---|---|
| 1 | IMIN,IMIN,IMIN | -2147483648 | -2147483648 | -2147483648 | [] | [] (Δ < 0) | PASS |
| 19 | INN,IMIN,INN | -7 | -2147483648 | -7 | [-3.0678337828571427E8, -0.0] | [-3.0678337828571427E8, -3.259629011154175E-9] ± 7.1e-15, 7.1e-15 (relatif) | FAIL |

## 3. Organisation

```
src/main/java/equation/          LES SYSTÈMES SOUS TEST (le code de test ne les connaît pas)
  SolveurNaif.java               formule du lycée (slide 51)
  SolveurStableCours.java        version stable du cours (slide 54)
  SolveurRobuste.java            version corrigée (exposant étendu + discriminant de Kahan)
  Etendu.java                    nombres m·2^e sans limite d'exposant
src/test/java/vv/
  reflexion/SolveurReflexif      chargement et appel dynamiques
  classes/                       les 9 classes flottantes et les 9 classes entières (@ClasseEquivalence)
  pairwise/                      le décorateur @PairwiseSource, son générateur, @Domaine
  modele/                        @CasDeTest (modèle formel), 4 verdicts, rapport, journal des cas
  oracles/                       référence exacte, différentiel, propriétés, homothétie, métamorphique
  oracles/ResultatsAttendus      lecture des RA figés
  tests/                         les classes de test JUnit (toutes alimentées par @PairwiseSource)
  outils/TableauDetection        la suite contre les 3 solveurs
  outils/GenererResultatsAttendus  calcule une fois les RA des 2 × 81 cas (BigDecimal) et les fige
src/test/resources/              solveur.properties, resultats-attendus.csv
outils/                          jenny.c et jenny-9-9-9.txt, pour comparaison seulement
```

## 4. Les 9 classes de chaque domaine

Flottants IEEE 754 (`ClassesEquivalence`) :

| Ordre | Code | Classe | Valeur | Ce qu'elle vise |
|---|---|---|---|---|
| 1 | GN | grand négatif | −1e200 | x² = 1e400 > 1,8e308 : débordement |
| 2 | NN | normal négatif | −2,5 | cas nominal |
| 3 | PN | petit négatif | −1e-8 | élimination catastrophique |
| 4 | TPN | très petit négatif | −1e-200 | x² = 1e-400 < 4,9e-324 : absorption |
| 5 | Z | zéro | 0 | a = 0 → exception ; b = 0, c = 0 |
| 6 | TPP | très petit positif | 1e-200 | absorption |
| 7 | PP | petit positif | 1e-8 | élimination |
| 8 | NP | normal positif | 3 | cas nominal |
| 9 | GP | grand positif | 1e200 | débordement |

Entiers machine, int 32 bits (`ClassesEntiers`) :

| Ordre | Code | Classe | Valeur | Ce qu'elle vise |
|---|---|---|---|---|
| 1 | IMIN | Integer.MIN_VALUE | −2147483648 | borne basse : −MIN déborde en int ; b² = 2⁶² > 2⁵³ |
| 2 | IMIN1 | MIN + 1 | −2147483647 | voisine de la borne : b² a 62 bits, arrondi en double |
| 3 | INN | négatif normal | −7 | cas nominal |
| 4 | IM1 | −1 | −1 | frontière du signe |
| 5 | IZ | zéro | 0 | a = 0 → exception |
| 6 | IP1 | +1 | 1 | frontière du signe |
| 7 | INP | positif normal | 5 | cas nominal |
| 8 | IMAX1 | MAX − 1 | 2147483646 | voisine de la borne haute |
| 9 | IMAX | Integer.MAX_VALUE | 2147483647 | borne haute : 4ac ≈ 1,8·10¹⁹ > Long.MAX_VALUE |

Pour chaque domaine, exhaustif : 9³ = **729** cas. Pairwise : **81** cas, soit 89 % de moins. C'est le minimum
théorique (9 × 9), atteint par un carré latin. Jenny en produit **88** pour le même modèle.

## 5. Résultats : tableau de détection (exécution réelle)

Chaque ligne : nombre de cas pairwise en échec (FAIL + ERROR) sur 81.

Flottants IEEE 754 :

| Oracle (81 cas pairwise chacun) | Naïf | Stable du cours | Robuste |
|---|---|---|---|
| Référence : RA et Tol figés | **31/81** | **10/81** | 0/81 |
| Différentiel BigDecimal | **31/81** | **10/81** | 0/81 |
| Propriété : résidu, Viète, signe de Δ | **6/81** | **10/81** | 0/81 |
| Implicite (dont exception et message si a = 0) | **2/81** | **3/81** | 0/81 |
| Homothétie k = 2¹⁰ et 2⁻¹⁰ | 2/81 (NaN seulement) | 3/81 | 0/81 |
| Métamorphique opposé / signe | 2/81 | 3/81 | 0/81 |
| Métamorphique inverse | 2/81 | 1/81 | 0/81 |
| Construction (81 couples de racines) | **56/81** | **4/81** | 0/81 |
| Homothétie : cas non intéressant (formule naïve) | 29 cas démontrés | 29 | 29 |

Entiers machine :

| Oracle (81 cas pairwise chacun) | Naïf | Stable du cours | Robuste |
|---|---|---|---|
| Référence : RA et Tol figés | **17/81** | 0/81 | 0/81 |
| Différentiel BigDecimal | **17/81** | 0/81 | 0/81 |
| Propriété : résidu, Viète, signe de Δ | **8/81** | 0/81 | 0/81 |
| Implicite | 0/81 | 0/81 | 0/81 |
| Métamorphique opposé / signe | 0/81 | 0/81 | 0/81 |
| Métamorphique inverse | **8/81** | 0/81 | 0/81 |

Quatre constats à défendre :

1. **Le pairwise a trouvé un défaut dans la version « stable » du cours.** Sur (1e-200, 0, 1e200), elle
   divise tout par 1e200 et a devient 0 (sous 4,9e-324). Elle rend alors `[NaN]` au lieu d'une liste vide.
2. **L'homothétie n'est pas intéressante pour la formule naïve.** Elle ne la détecte que sur les NaN (NaN ≠ NaN).
   Sur 29 cas pairwise, la formule naïve est fausse, par exemple (−2,5 ; −2,5 ; −1e-8) avec une erreur de 1,2·10⁻⁸,
   ou (−2,5 ; −1e200 ; −2,5) qui donne −∞ au lieu de −4e199. Pourtant, l'homothétie la déclare correcte, parce que
   la formule naïve refait la même erreur sur l'équation multipliée par 1024.
3. **Les entiers machine suffisent à mettre la formule naïve en défaut.** Avec |b| ≈ 2³¹ et a, c petits,
   −b + √Δ soustrait deux nombres presque égaux : sur (−7 ; −2147483648 ; −7), elle rend −0,0 au lieu de
   −3,26·10⁻⁹. Aucun débordement n'est en cause, seulement l'élimination catastrophique.
4. **Les oracles métamorphiques ont trouvé un défaut dans mon propre solveur robuste** pendant le
   développement. Pour b = 0, ses deux racines ±√(−c/a) différaient d'un ulp. Il a été corrigé.

Deux vérifications ne testent pas l'équation mais l'outillage : le décorateur refuse de s'exécuter si une
paire n'est pas couverte, et `ArchitectureTest` vérifie qu'aucun fichier de test ne référence un solveur.

## 6. Environnement de vérification

Les chiffres ci-dessus viennent de `mvn test` et `mvn test-compile exec:java` (Maven 3.9.9, JDK 17.0.5,
JUnit 5.11.4). Si les classes d'équivalence changent, relancer `vv.outils.GenererResultatsAttendus`
pour régénérer `resultats-attendus.csv`.

## 7. Couverture de code et tests de mutation

Les tests unitaires ciblent `equation.SolveurRobuste` par défaut. Les outils présentés dans le cours
(JaCoCo, p. 130-132 ; PIT, p. 138-139) répondent à deux questions complémentaires : JaCoCo indique
quelles instructions et branches ont été exécutées ; PIT modifie le programme et mesure si les tests
détectent ces modifications.

Avec JDK 17 et Maven 3.9+, depuis la racine du projet :

```bash
mvn clean verify
```

Cette commande lance les tests, produit le rapport JaCoCo dans
`target/site/jacoco/index.html` et vérifie sur `SolveurRobuste` un minimum de 90 % des lignes et
80 % des branches. Lire la ligne `SolveurRobuste` dans le rapport pour évaluer ce solveur : le total
du rapport inclut aussi les solveurs naïf et du cours, qui ne sont pas exécutés par la configuration
par défaut.

```bash
mvn clean test-compile org.pitest:pitest-maven:mutationCoverage
```

Le rapport HTML, CSV et XML de PIT est créé dans `target/pit-reports/` (page d'accueil :
`target/pit-reports/index.html`). PIT cible `SolveurRobuste` et ses classes d'appui (`Etendu` et
`Coefficient`), et utilise les tests `vv.tests.*`. Le score de mutation n'est pas assorti d'un seuil
arbitraire : le rapport doit rester disponible pour analyser les mutants survivants et renforcer
les tests.

Derniers rapports présents dans `target/` :

| Outil / périmètre | Résultat | Interprétation |
|---|---:|---|
| JaCoCo — `SolveurRobuste` | 48/50 lignes (96 %) ; 16/18 branches (88,9 %) | Les seuils de 90 % des lignes et 80 % des branches sont atteints. |
| PIT — 3 classes ciblées | 50/101 mutants tués (50 %) ; force des tests : 50/86 (58 %) | Malgré la forte couverture des lignes (91/97, soit 94 %), plusieurs mutants survivent : l'exécution d'une ligne ne garantit pas que son comportement soit vérifié. |

Le résultat PIT met en évidence des tests à renforcer, notamment pour les conditions limites et les
branches des classes d'appui. Contrairement à JaCoCo, qui mesure l'exécution, PIT mesure la capacité
des assertions à distinguer le programme original d'une version mutée. Les rapports sont régénérés
par les commandes ci-dessus ; les résultats chiffrés sont ceux des rapports déjà présents dans
`target/`.

## 7. Couverture de code (JaCoCo) et test de mutation (PIT)

```bash
mvn clean verify                                          # tests + rapport JaCoCo + quality gate
mvn test-compile org.pitest:pitest-maven:mutationCoverage # test de mutation
```

| Rapport | Fichier |
|---|---|
| JaCoCo (HTML) | `target/site/jacoco/index.html` |
| PIT (HTML) | `target/pit-reports/index.html` |

`mvn verify` échoue si le solveur testé descend sous 90 % des lignes ou 80 % des branches
(quality gate `jacoco:check`). Résultats et analyse : voir `RAPPORT.md`, section « Couverture et mutation ».
