package vv.tests;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;

import vv.modele.CasDeTest;
import vv.modele.TypeOracle;
import vv.oracles.OracleDifferentiel;
import vv.oracles.OracleMetamorphique;
import vv.oracles.OracleProprietes;
import vv.pairwise.PairwiseSource;
import vv.reflexion.SolveurReflexif;

/**
 * Campagne pairwise : 9 classes d'équivalence × 3 coefficients.
 *
 * Exhaustif : 9³ = 729 cas. Pairwise : 81 cas (le minimum possible, 9 × 9), qui couvrent
 * les 3 × 81 = 243 paires de classes. Chaque méthode ci-dessous reçoit ces 81 cas grâce au décorateur
 * @PairwiseSource, et juge le résultat avec un oracle différent. Aucune n'a de résultat attendu écrit
 * à la main : pour 1e-200·x² + 1e200·x + 3 = 0, personne ne connaît la réponse de tête.
 *
 * Domaine : flottants IEEE 754 ({@link vv.classes.ClassesEquivalence}). La sous-classe
 * {@link PairwiseEntiersTest} rejoue les mêmes oracles sur le domaine des entiers machine.
 */
@Tag("pairwise")
@DisplayName("Pairwise : 9 classes × (a, b, c)")
class PairwiseTest {

    private static final String NOM = "{index} → a∈{0}  b∈{1}  c∈{2}";

    // ------------------------------------------------------------------ oracle implicite

    @ParameterizedTest(name = NOM)
    @PairwiseSource
    @DisplayName("Oracle implicite : pas de plantage, contrat respecté")
    @CasDeTest(scenario = "le solveur ne plante pas et respecte la forme du résultat",
            donnees = "81 cas pairwise", oracle = TypeOracle.IMPLICITE,
            resultatAttendu = "a = 0 → IllegalArgumentException « pas une équation du second degré » ; sinon ArrayList de 0 à 2 racines, triée, sans NaN")
    void implicite(double a, double b, double c) {
        SolveurReflexif g = SolveurReflexif.courant();
        if (a == 0) {
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> g.racines(a, b, c));
            assertTrue(String.valueOf(e.getMessage()).contains("second degré"),
                    "message attendu « ce n'est pas une équation du second degré », reçu : " + e.getMessage());
            return;
        }
        List<Double> y = g.racines(a, b, c);
        assertNotNull(y);
        assertTrue(y.size() <= 2, "plus de deux racines : " + y);
        for (double r : y) assertFalse(Double.isNaN(r), "racine NaN : " + y);
        if (y.size() == 2) assertTrue(y.get(0) <= y.get(1), "racines non triées : " + y);
    }

    // ------------------------------------------------------------------ oracle différentiel

    @ParameterizedTest(name = NOM)
    @PairwiseSource
    @DisplayName("Oracle différentiel : comparaison à BigDecimal")
    @CasDeTest(scenario = "les racines sont celles du calcul exact, arrondies",
            donnees = "81 cas pairwise", oracle = TypeOracle.DIFFERENTIEL,
            resultatAttendu = "calculé : Δ exact, racines en BigDecimal (60 chiffres + écart des ordres de grandeur)",
            mesure = "erreur mixte |y − ŷ| <= Tol·|ŷ| + 2·MIN_VALUE ; ±∞ si la vraie racine dépasse MAX_VALUE",
            tolerance = "16·κ·ε par racine ; Δ ≈ 0 : 0, 1 ou 2 racines à 4·√(16ε(b²+|4ac|))/|2a| de −b/2a")
    void differentiel(double a, double b, double c) {
        SolveurReflexif g = SolveurReflexif.courant();
        if (a == 0) {
            assertThrows(IllegalArgumentException.class, () -> g.racines(a, b, c));
            return;
        }
        List<String> v = OracleDifferentiel.verifier(a, b, c, g.racines(a, b, c));
        assertTrue(v.isEmpty(), () -> String.join(" ; ", v));
    }

    // ------------------------------------------------------------------ oracle par propriété

    @ParameterizedTest(name = NOM)
    @PairwiseSource
    @DisplayName("Oracle par propriété : résidu, Viète, signe de Δ")
    @CasDeTest(scenario = "chaque racine annule le polynôme ; produit et somme de Viète",
            donnees = "81 cas pairwise", oracle = TypeOracle.PROPRIETE,
            mesure = "résidu relatif |a·r² + b·r + c| / (|a|r² + |b||r| + |c|), calculé exactement",
            tolerance = "8·ε (erreur inverse d'un algorithme stable, slide 78)")
    void proprietes(double a, double b, double c) {
        SolveurReflexif g = SolveurReflexif.courant();
        if (a == 0) {
            assertThrows(IllegalArgumentException.class, () -> g.racines(a, b, c));
            return;
        }
        OracleProprietes.Resultat r = OracleProprietes.verifier(a, b, c, g.racines(a, b, c));
        assertTrue(r.violations().isEmpty(), () -> String.join(" ; ", r.violations()));
        assumeTrue(r.verifiees() > 0, () -> "aucune propriété évaluable : " + r.horsDomaine());
    }

    // ------------------------------------------------------------------ oracles métamorphiques

    @ParameterizedTest(name = NOM)
    @PairwiseSource
    @DisplayName("MR1 opposé : racines(a, −b, c) = −racines(a, b, c)")
    @CasDeTest(scenario = "changer b en −b change x en −x",
            donnees = "81 cas pairwise et leurs transformés", oracle = TypeOracle.METAMORPHIQUE,
            mesure = "égalité numérique stricte, liste renversée", tolerance = "0 : la négation est exacte")
    void mr1Oppose(double a, double b, double c) {
        metamorphique(a, b, c, a == 0 ? null : OracleMetamorphique.oppose(SolveurReflexif.courant(), a, b, c), true);
    }

    @ParameterizedTest(name = NOM)
    @PairwiseSource
    @DisplayName("MR2 signe : racines(−a, −b, −c) = racines(a, b, c)")
    @CasDeTest(scenario = "multiplier l'équation par −1 ne change pas les racines",
            donnees = "81 cas pairwise et leurs transformés", oracle = TypeOracle.METAMORPHIQUE,
            mesure = "égalité numérique stricte", tolerance = "0 : la négation est exacte")
    void mr2Signe(double a, double b, double c) {
        metamorphique(a, b, c, a == 0 ? null : OracleMetamorphique.signe(SolveurReflexif.courant(), a, b, c), true);
    }

    @ParameterizedTest(name = NOM)
    @PairwiseSource
    @DisplayName("MR3 inverse : racines(c, b, a) = 1 / racines(a, b, c)")
    @CasDeTest(scenario = "diviser par x² : les racines de c·u² + b·u + a sont les inverses",
            donnees = "cas pairwise avec a ≠ 0 et c ≠ 0", oracle = TypeOracle.METAMORPHIQUE,
            mesure = "erreur relative après appariement",
            tolerance = "2·16·κ·ε + 2ε, κ estimé sur les racines obtenues (pas de RA)")
    void mr3Inverse(double a, double b, double c) {
        assumeTrue(a != 0, "hors domaine : a = 0, l'équation de départ n'est pas du second degré");
        metamorphique(a, b, c, OracleMetamorphique.inverse(SolveurReflexif.courant(), a, b, c), false);
    }

    private static void metamorphique(double a, double b, double c, Optional<List<String>> resultat, boolean aNulCoherent) {
        if (a == 0 && aNulCoherent) {
            // g(x) lève une exception : g(T(x)) doit en lever une aussi
            SolveurReflexif g = SolveurReflexif.courant();
            assertThrows(IllegalArgumentException.class, () -> g.racines(a, b, c));
            assertThrows(IllegalArgumentException.class, () -> g.racines(-a, -b, -c));
            return;
        }
        assumeTrue(resultat.isPresent(), "hors domaine : la transformation n'est pas représentable en double");
        List<String> v = resultat.get();
        assertTrue(v.isEmpty(), () -> String.join(" ; ", v));
    }
}
