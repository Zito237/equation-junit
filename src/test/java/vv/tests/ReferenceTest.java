package vv.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;

import vv.modele.CasDeTest;
import vv.modele.JournalCas;
import vv.modele.TypeOracle;
import vv.oracles.MesureErreur;
import vv.oracles.ResultatsAttendus;
import vv.oracles.ResultatsAttendus.RA;
import vv.pairwise.PairwiseSource;
import vv.reflexion.SolveurReflexif;

/**
 * Oracle PAR RÉFÉRENCE sur les 81 cas pairwise : N(y, RA) <= Tol.
 *
 * Chaque cas pairwise a son résultat attendu écrit dans resultats-attendus.csv (calculé une fois en BigDecimal,
 * puis figé) avec sa tolérance Tol = 16·κ·ε. Les 9 cas où a = 0 attendent l'exception
 * « ce n'est pas une équation du second degré », y compris quand b et c sont non nuls.
 *
 * Chaque cas est noté dans le {@link JournalCas} : entrées a, b, c, sortie du solveur, RA et verdict.
 * Domaine : flottants IEEE 754 ; {@link ReferenceEntiersTest} rejoue l'oracle sur les entiers machine.
 */
@Tag("reference")
@ExtendWith(JournalCas.class)
@DisplayName("Oracle par référence : RA et Tol des 81 cas pairwise")
class ReferenceTest {

    @ParameterizedTest(name = "{index} → a∈{0}  b∈{1}  c∈{2}")
    @PairwiseSource
    @DisplayName("racines(a, b, c) proches du résultat attendu")
    @CasDeTest(scenario = "chaque cas pairwise comparé à son résultat attendu écrit",
            donnees = "81 cas pairwise (@PairwiseSource)", oracle = TypeOracle.REFERENCE,
            resultatAttendu = "resultats-attendus.csv : racines, liste vide, ou exception si a = 0",
            mesure = "erreur relative |y − RA| / |RA| par racine (listes triées), mixte sous MIN_NORMAL",
            tolerance = "16·κ·ε par racine ; racine double : 16·√ε")
    void parReference(double a, double b, double c) {
        RA ra = ResultatsAttendus.pour(a, b, c)
                .orElseThrow(() -> new IllegalStateException("pas de RA pour (" + a + ", " + b + ", " + c
                        + ") : relancer vv.outils.GenererResultatsAttendus"));
        SolveurReflexif g = SolveurReflexif.courant();

        List<Double> y = null;
        Throwable exception = null;
        try {
            y = g.racines(a, b, c);
        } catch (RuntimeException | AssertionError e) { // AssertionError : contrat ArrayList<Double> violé
            exception = e;
        }
        JournalCas.noter(ra.classes(), a, b, c,
                exception != null ? "exception : " + exception.getMessage() : JournalCas.liste(y), attendu(ra));

        if (ra.exception()) {
            IllegalArgumentException e = assertInstanceOf(IllegalArgumentException.class, exception,
                    "IllegalArgumentException attendue pour a = 0, obtenu " + (y != null ? y : exception));
            String msg = String.valueOf(e.getMessage());
            assertTrue(msg.contains("pas") && msg.contains("second degré"),
                    "le message doit dire que ce n'est pas une équation du second degré, reçu : « " + msg + " »");
            return;
        }
        if (exception instanceof RuntimeException e) throw e;
        if (exception instanceof AssertionError e) throw e;

        if (ra.racineDouble()) {
            // κ infini : une ou deux racines très proches de la racine double sont correctes
            assertTrue(y.size() == 1 || y.size() == 2, "1 ou 2 racines attendues, obtenu " + y);
            for (double r : y) {
                assertTrue(MesureErreur.proche(r, ra.racines().get(0), ra.tolerances().get(0), MesureErreur.ATOL),
                        r + " trop loin de la racine double " + ra.racines().get(0));
            }
            return;
        }
        assertEquals(ra.racines().size(), y.size(), "nombre de racines : obtenu " + y + ", attendu " + ra.racines());
        for (int i = 0; i < y.size(); i++) {
            double attendu = ra.racines().get(i), tol = ra.tolerances().get(i);
            assertTrue(MesureErreur.proche(y.get(i), attendu, tol, MesureErreur.ATOL),
                    "racine " + (i + 1) + " : " + y.get(i) + " au lieu de " + attendu
                            + " (erreur relative " + MesureErreur.relative(y.get(i), attendu) + " > Tol " + tol + ")");
        }
    }

    /** Le RA tel qu'affiché dans le journal. */
    private static String attendu(RA ra) {
        if (ra.exception()) return "exception (a = 0)";
        if (ra.racines().isEmpty()) return "[] (Δ < 0)";
        List<String> tol = new ArrayList<>();
        for (double t : ra.tolerances()) tol.add(String.format(Locale.ROOT, "%.1e", t));
        return JournalCas.liste(ra.racines()) + (ra.racineDouble() ? " racine double" : "")
                + " ± " + String.join(", ", tol) + " (relatif)";
    }
}
