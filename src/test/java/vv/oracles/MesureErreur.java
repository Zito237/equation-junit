package vv.oracles;

/**
 * Mesures d'erreur N du cours (slide 66) et tolérance Tol = c·κ·ε (slide 67).
 */
public final class MesureErreur {

    /** Constante c de Tol = c·κ·ε : une douzaine d'arrondis dans le solveur, arrondie à 16. */
    public static final double C = 16;

    /**
     * Partie absolue de la mesure mixte : sous Double.MIN_NORMAL (2,2e-308), les double sont espacés
     * de Double.MIN_VALUE (4,9e-324) quelle que soit la valeur : l'erreur relative n'a plus de sens,
     * l'erreur absolue est d'au moins un demi-pas. On accepte 2 pas.
     */
    public static final double ATOL = 2 * Double.MIN_VALUE;

    private MesureErreur() {
    }

    /** Tol = C · κ · ε. */
    public static double tolerance(double kappa) {
        return C * kappa * ReferenceExacte.EPS;
    }

    /** Erreur relative |y − ŷ| / |ŷ|. */
    public static double relative(double y, double attendu) {
        return Math.abs(y - attendu) / Math.abs(attendu);
    }

    /**
     * Mesure mixte |y − ŷ| <= rtol·|ŷ| + atol, avec les cas spéciaux traités à part :
     * ŷ infini (la vraie racine dépasse le plus grand double) -> y doit être le même infini ;
     * NaN n'est jamais acceptable.
     */
    public static boolean proche(double y, double attendu, double rtol, double atol) {
        if (Double.isNaN(y) || Double.isNaN(attendu)) return false;
        if (Double.isInfinite(attendu) || Double.isInfinite(y)) return y == attendu;
        return Math.abs(y - attendu) <= rtol * Math.abs(attendu) + atol;
    }
}
