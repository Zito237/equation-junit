package vv.oracles;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Oracle par HOMOTHÉTIE (slides 76-77, 79) : multiplier les trois coefficients par un même k ≠ 0
 * ne change pas l'équation, donc pas ses racines :
 *
 *      g(k·a, k·b, k·c) = g(a, b, c)
 *
 * Choix de k : une puissance de 2 (2^10 = 1024, 2^-10). Multiplier un double par 2^n ne change que
 * son exposant : AUCUN arrondi. Tout bon solveur doit donc rendre exactement les mêmes double :
 * l'égalité est STRICTE (tolérance 0). Avec k = 10, des différences au dernier bit seraient normales
 * et il faudrait une tolérance (slide 79).
 *
 * « Strict » au sens numérique (==) : 0.0 == −0.0 est vrai, NaN == NaN est faux (un NaN est un défaut).
 *
 * LIMITE (cas où cet oracle n'est pas intéressant) : il ne voit que les solveurs INCOHÉRENTS.
 * Un solveur faux mais cohérent, comme la formule naïve sur x² + 1e8·x + 1 = 0 (25 % d'erreur),
 * fait la même erreur sur l'équation multipliée par 1024 : la relation est respectée, le bug passe.
 */
public final class OracleHomothetique {

    /** Facteurs testés : 2^10 et 2^-10 (exacts en binaire). */
    public static final double[] FACTEURS = {1024, 1.0 / 1024};

    /** Toute fonction (a, b, c) -> racines : le solveur sous test, ou un mutant écrit dans un test. */
    @FunctionalInterface
    public interface Solveur {
        List<Double> racines(double a, double b, double c);
    }

    private OracleHomothetique() {
    }

    /**
     * @return vide si hors domaine (k·a, k·b ou k·c déborde, ou sous-dépasse en perdant des chiffres) ;
     *         sinon la liste des violations (vide = relation respectée).
     */
    public static Optional<List<String>> verifier(Solveur g, double a, double b, double c, double k) {
        if (!exact(a, k) || !exact(b, k) || !exact(c, k)) return Optional.empty();
        List<Double> y = g.racines(a, b, c);
        List<Double> yk = g.racines(k * a, k * b, k * c);
        List<String> v = new ArrayList<>();
        boolean ok = y.size() == yk.size();
        for (int i = 0; ok && i < y.size(); i++) {
            ok = y.get(i).doubleValue() == yk.get(i).doubleValue();
        }
        if (!ok) v.add("homothétie k = " + k + " : g(a, b, c) = " + y + " mais g(k·a, k·b, k·c) = " + yk);
        return Optional.of(v);
    }

    /** k·x doit être exact : ni débordement, ni passage en sous-normal (où l'on perd des bits). */
    private static boolean exact(double x, double k) {
        double kx = k * x;
        return Double.isFinite(kx) && (kx == 0 ? x == 0 : Math.abs(kx) >= Double.MIN_NORMAL) && kx / k == x;
    }
}
