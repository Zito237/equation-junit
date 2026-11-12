package vv.tests;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;

import vv.modele.CasDeTest;
import vv.modele.TypeOracle;
import vv.oracles.OracleDifferentiel;
import vv.oracles.OracleHomothetique;
import vv.pairwise.PairwiseSource;
import vv.reflexion.SolveurReflexif;

/**
 * Oracle par homothétie : g(k·a, k·b, k·c) = g(a, b, c) avec k = 2^10 et 2^-10, égalité stricte.
 * Aucun résultat attendu n'est nécessaire. Les deux tests reçoivent les 81 cas pairwise.
 */
@Tag("homothetie")
@DisplayName("Oracle par homothétie : g(k·a, k·b, k·c) = g(a, b, c)")
class HomothetieTest {

    private static final String NOM = "{index} → a∈{0}  b∈{1}  c∈{2}";

    @ParameterizedTest(name = NOM)
    @PairwiseSource
    @DisplayName("Homothétie sur les 81 cas pairwise")
    @CasDeTest(scenario = "multiplier l'équation par 2^10 puis par 2^-10 ne change pas les racines",
            donnees = "81 cas pairwise et leurs transformés", oracle = TypeOracle.HOMOTHETIQUE,
            resultatAttendu = "aucun : on compare g(x) à g(k·x)",
            mesure = "égalité numérique stricte (==), racine par racine",
            tolerance = "0 : la multiplication par une puissance de 2 est exacte")
    void pairwise(double a, double b, double c) {
        SolveurReflexif g = SolveurReflexif.courant();
        if (a == 0) {
            // g(x) lève l'exception : g(k·x) doit la lever aussi (k·0 = 0)
            assertThrows(IllegalArgumentException.class, () -> g.racines(a, b, c));
            assertThrows(IllegalArgumentException.class, () -> g.racines(1024 * a, 1024 * b, 1024 * c));
            return;
        }
        List<String> violations = new ArrayList<>();
        int appliques = 0;
        for (double k : OracleHomothetique.FACTEURS) {
            Optional<List<String>> r = OracleHomothetique.verifier(g::racines, a, b, c, k);
            if (r.isPresent()) {
                appliques++;
                violations.addAll(r.get());
            }
        }
        assertTrue(violations.isEmpty(), () -> String.join(" ; ", violations));
        assumeTrue(appliques > 0, "hors domaine : k·a, k·b ou k·c n'est pas exact en double");
    }

    /**
     * CAS OÙ L'ORACLE PAR HOMOTHÉTIE N'EST PAS INTÉRESSANT, sur les 81 cas pairwise.
     *
     * La formule naïve (slide 51) est écrite ici, dans le test, comme un mutant volontaire : la démonstration
     * ne dépend donc pas du solveur configuré. Sur chaque cas pairwise où elle est FAUSSE (l'oracle différentiel
     * la rejette) sans produire de NaN, on vérifie que l'homothétie la déclare pourtant CORRECTE : la formule
     * naïve refait exactement la même erreur sur l'équation multipliée par 2^10.
     *
     * Verdicts : PASS = « l'homothétie laisse passer ce résultat faux » (la limite est démontrée sur ce cas) ;
     * INCONCLUSIF = la formule naïve est juste sur ce cas, ou rend un NaN que l'homothétie, elle, voit.
     */
    @ParameterizedTest(name = NOM)
    @PairwiseSource
    @DisplayName("Cas où l'homothétie n'est pas intéressante : formule naïve fausse mais cohérente")
    @CasDeTest(scenario = "un solveur faux mais cohérent passe l'oracle par homothétie",
            donnees = "81 cas pairwise, formule naïve du slide 51 comme mutant",
            oracle = TypeOracle.HOMOTHETIQUE,
            resultatAttendu = "sur chaque cas où la formule naïve est fausse, l'homothétie répond quand même « respectée »",
            mesure = "égalité stricte (homothétie) / erreur relative (différentiel témoin)", tolerance = "0 / 16·κ·ε")
    void casNonInteressant(double a, double b, double c) {
        assumeTrue(a != 0, "a = 0 : la formule naïve lève l'exception, rien à démontrer");
        OracleHomothetique.Solveur naif = HomothetieTest::formuleNaive;
        List<Double> y = naif.racines(a, b, c);
        assumeTrue(y.stream().noneMatch(r -> Double.isNaN(r)), "la formule naïve rend NaN : l'homothétie le détecte (NaN ≠ NaN)");
        List<String> differentiel = OracleDifferentiel.verifier(a, b, c, y);
        assumeTrue(!differentiel.isEmpty(), "la formule naïve est juste sur ce cas");

        Optional<List<String>> homothetie = OracleHomothetique.verifier(naif, a, b, c, 1024);
        assumeTrue(homothetie.isPresent(), "hors domaine : 1024·a, 1024·b ou 1024·c n'est pas exact");
        assertTrue(homothetie.get().isEmpty(), () -> "ici l'homothétie détecte l'erreur : " + homothetie.get());
        System.out.println("Homothétie respectée alors que la formule naïve est fausse : (" + a + ", " + b + ", " + c
                + ") → " + y + " ; " + differentiel.get(0));
    }

    /** Formule du lycée (slide 51), écrite dans le test comme mutant de démonstration. */
    private static List<Double> formuleNaive(double a, double b, double c) {
        if (a == 0) throw new IllegalArgumentException("a = 0 : ce n'est pas une équation du second degré");
        List<Double> r = new ArrayList<>();
        double d = b * b - 4 * a * c;
        if (d < 0) return r;
        if (d == 0) {
            r.add(-b / (2 * a));
            return r;
        }
        double r1 = (-b - Math.sqrt(d)) / (2 * a), r2 = (-b + Math.sqrt(d)) / (2 * a);
        r.add(Math.min(r1, r2));
        r.add(Math.max(r1, r2));
        return r;
    }
}
