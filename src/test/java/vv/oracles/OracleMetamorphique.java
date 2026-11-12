package vv.oracles;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import vv.reflexion.SolveurReflexif;

/**
 * Oracles métamorphiques (T.Y. Chen, 1998) : on ne connaît pas y = g(x), mais on sait comment y
 * DOIT changer quand x change. Une relation = une transformation T des entrées + une relation R
 * entre g(x) et g(T(x)).
 *
 *  (L'homothétie g(k·a, k·b, k·c) = g(a, b, c) a sa propre classe : OracleHomothetique.)
 *
 *  MR1 opposé   : g(a, −b, c) = −g(a, b, c) (liste renversée car triée). STRICT : la négation est exacte.
 *  MR2 signe    : g(−a, −b, −c) = g(a, b, c). Strict.
 *  MR3 inverse  : g(c, b, a) = { 1/r : r ∈ g(a, b, c) } (diviser par x² : c·(1/x)² + b·(1/x) + a = 0).
 *                 Non exacte : 1/r est arrondi ; Tol = 2·16·κ·ε + 2ε, avec κ estimé sur les racines
 *                 OBTENUES (aucun résultat attendu).
 *
 * « Strict » se lit au sens numérique (==) : 0.0 == −0.0 est vrai, NaN == NaN est faux.
 * Une relation dont la transformation sortirait du domaine (c = 0 pour MR3, 1/r non représentable) est hors domaine.
 */
public final class OracleMetamorphique {

    private OracleMetamorphique() {
    }

    public static Optional<List<String>> oppose(SolveurReflexif g, double a, double b, double c) {
        List<Double> r = g.racines(a, b, c);
        List<Double> attendu = new ArrayList<>();
        for (int i = r.size() - 1; i >= 0; i--) attendu.add(-r.get(i));
        return Optional.of(egal("MR1 opposé", attendu, g.racines(a, -b, c)));
    }

    public static Optional<List<String>> signe(SolveurReflexif g, double a, double b, double c) {
        return Optional.of(egal("MR2 signe", g.racines(a, b, c), g.racines(-a, -b, -c)));
    }

    public static Optional<List<String>> inverse(SolveurReflexif g, double a, double b, double c) {
        if (c == 0) return Optional.empty(); // g(c, b, a) n'est plus du second degré
        List<Double> r = g.racines(a, b, c);
        for (double x : r) {
            if (x == 0 || !Double.isFinite(x) || Math.abs(x) < Double.MIN_NORMAL || Math.abs(x) > 1 / Double.MIN_NORMAL) {
                return Optional.empty(); // 1/x non représentable ou imprécis
            }
        }
        List<Double> attendu = new ArrayList<>();
        for (double x : r) attendu.add(1 / x);
        Collections.sort(attendu);
        List<Double> s = g.racines(c, b, a);
        List<String> v = new ArrayList<>();
        if (s.size() != attendu.size()) {
            v.add("MR3 inverse : " + s.size() + " racine(s) pour (c, b, a) au lieu de " + attendu.size() + " : " + s + " vs " + attendu);
            return Optional.of(v);
        }
        for (int i = 0; i < s.size(); i++) {
            double kappa = kappaObserve(a, b, c, r.get(r.size() - 1 - i));
            double tol = 2 * MesureErreur.tolerance(kappa) + 2 * ReferenceExacte.EPS;
            if (!MesureErreur.proche(s.get(i), attendu.get(i), tol, MesureErreur.ATOL)) {
                v.add(String.format(java.util.Locale.ROOT, "MR3 inverse : %s au lieu de 1/r = %s (erreur relative %.3g > %.3g)",
                        s.get(i), attendu.get(i), MesureErreur.relative(s.get(i), attendu.get(i)), tol));
            }
        }
        return Optional.of(v);
    }

    /** κ estimé à partir d'une racine OBTENUE (pas d'un résultat attendu). */
    static double kappaObserve(double a, double b, double c, double r) {
        BigDecimal A = new BigDecimal(a), B = new BigDecimal(b), C = new BigDecimal(c), R = new BigDecimal(r);
        BigDecimal num = A.abs().multiply(R.pow(2)).add(B.abs().multiply(R.abs())).add(C.abs());
        BigDecimal den = R.abs().multiply(BigDecimal.valueOf(2).multiply(A).multiply(R).add(B).abs());
        if (den.signum() == 0) return Double.POSITIVE_INFINITY;
        return num.divide(den, MathContext.DECIMAL64).doubleValue();
    }

    private static List<String> egal(String nom, List<Double> attendu, List<Double> obtenu) {
        List<String> v = new ArrayList<>();
        boolean ok = attendu.size() == obtenu.size();
        for (int i = 0; ok && i < attendu.size(); i++) {
            ok = attendu.get(i).doubleValue() == obtenu.get(i).doubleValue();
        }
        if (!ok) v.add(nom + " : attendu " + attendu + ", obtenu " + obtenu);
        return v;
    }
}
