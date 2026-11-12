package vv.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;

import vv.modele.CasDeTest;
import vv.modele.TypeOracle;
import vv.oracles.MesureErreur;
import vv.oracles.ReferenceExacte;
import vv.pairwise.PairwiseSource;
import vv.reflexion.SolveurReflexif;

/**
 * Oracle par construction (slide 80) : on CHOISIT les racines r1, r2 dans les 9 classes,
 * on fabrique x² − (r1 + r2)·x + r1·r2 = 0, et on vérifie qu'on les retrouve.
 *
 * Le même décorateur @PairwiseSource sert ici avec 2 paramètres : toutes les paires de classes,
 * soit 81 couples (r1, r2).
 *
 * Attention (slide 80) : b = −(r1 + r2) et c = r1·r2 sont eux-mêmes arrondis. L'équation construite
 * n'a donc pas exactement r1 et r2 pour racines : la tolérance doit absorber cet arrondi, d'où
 * Tol = 16·κ·ε avec κ le conditionnement des racines choisies.
 */
@Tag("construction")
@DisplayName("Oracle par construction : racines choisies dans les 9 classes")
class ConstructionTest {

    @ParameterizedTest(name = "{index} → r1∈{0}  r2∈{1}")
    @PairwiseSource(parametres = 2)
    @DisplayName("Retrouver r1 et r2 à partir de (x − r1)(x − r2)")
    @CasDeTest(scenario = "les racines d'une équation fabriquée sont connues par construction",
            donnees = "81 couples (r1, r2) de classes", oracle = TypeOracle.CONSTRUCTION,
            resultatAttendu = "{min(r1, r2), max(r1, r2)}",
            mesure = "erreur mixte par racine",
            tolerance = "16·κ·ε (κ des racines choisies) ; r1 = r2 : 0 à 2 racines à 16·√ε (Δ ≈ 0)")
    void construction(double r1, double r2) {
        double b = -(r1 + r2), c = r1 * r2;
        assumeTrue(Double.isFinite(b) && Double.isFinite(c), "hors domaine : b ou c déborde (" + b + ", " + c + ")");
        assumeTrue(c != 0 || r1 == 0 || r2 == 0, "hors domaine : r1·r2 sous-dépasse vers 0, l'équation construite a changé");
        assumeTrue(Math.abs(c) >= Double.MIN_NORMAL || c == 0, "hors domaine : r1·r2 est sous-normal, imprécis");

        List<Double> y = SolveurReflexif.courant().racines(1, b, c);
        double petit = Math.min(r1, r2), grand = Math.max(r1, r2);

        if (r1 == r2 || ReferenceExacte.de(1, b, c).malConditionne) {
            // racine double (ou presque) : Δ ≈ 0, l'arrondi de c a pu le rendre légèrement négatif.
            // 0, 1 ou 2 racines sont défendables ; celles rendues doivent être à √ε près.
            assertTrue(y.size() <= 2, "au plus 2 racines attendues : " + y);
            double tol = 16 * Math.sqrt(ReferenceExacte.EPS);
            for (double r : y) {
                assertTrue(MesureErreur.proche(r, petit, tol, MesureErreur.ATOL)
                        || MesureErreur.proche(r, grand, tol, MesureErreur.ATOL), r + " loin de " + petit + " / " + grand);
            }
            return;
        }
        assertEquals(2, y.size(), "deux racines attendues : " + y);
        double[] attendu = {petit, grand};
        for (int i = 0; i < 2; i++) {
            double kappa = kappa(attendu[i], petit, grand);
            double tol = Double.isNaN(kappa) ? 0 : MesureErreur.tolerance(kappa);
            assertTrue(MesureErreur.proche(y.get(i), attendu[i], tol, MesureErreur.ATOL),
                    "racine " + (i + 1) + " : " + y.get(i) + " au lieu de " + attendu[i] + " (Tol " + tol + ")");
        }
    }

    /** κ de la racine r de (x − r1)(x − r2) : (r² + |r1 + r2||r| + |r1·r2|) / (|r|·|r1 − r2|). */
    private static double kappa(double r, double r1, double r2) {
        if (r == 0) return Double.NaN;
        java.math.BigDecimal R = new java.math.BigDecimal(r), R1 = new java.math.BigDecimal(r1), R2 = new java.math.BigDecimal(r2);
        java.math.BigDecimal num = R.pow(2).add(R1.add(R2).abs().multiply(R.abs())).add(R1.multiply(R2).abs());
        java.math.BigDecimal den = R.abs().multiply(R1.subtract(R2).abs());
        return num.divide(den, java.math.MathContext.DECIMAL64).doubleValue();
    }
}
