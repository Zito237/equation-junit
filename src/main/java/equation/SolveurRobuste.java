package equation;

import java.util.ArrayList;

/**
 * Version robuste : corrige les défauts trouvés par les tests sur les deux autres versions.
 *
 *  1. Pas d'élimination catastrophique : q = -(b + signe(b)·√Δ)/2, x1 = q/a, x2 = c/q.
 *  2. Pas de débordement ni de sous-dépassement intermédiaire, même avec a = 1e-200 et c = 1e200 :
 *     tous les calculs se font en exposant étendu (classe Etendu), chaque coefficient gardant
 *     son propre exposant. La version du cours, qui met tout à l'échelle du plus grand, transforme
 *     1e-200 en 0 dans ce cas.
 *  3. Discriminant précis (méthode de Kahan) : b² et 4ac sont calculés avec leur erreur d'arrondi
 *     exacte (Math.fma), ce qui donne le bon signe de Δ même quand b² ≈ 4ac.
 *  4. Coefficient NaN ou infini refusé (IllegalArgumentException) au lieu d'un résultat absurde.
 *
 * Même contrat que les autres versions : a = 0 -> exception ; résultat = ArrayList triée de 0, 1 ou 2 racines.
 * Une racine dont la valeur exacte dépasse le plus grand double est rendue comme ±Infinity,
 * une racine plus petite que le plus petit double comme 0 : c'est l'arrondi correct.
 */
public class SolveurRobuste {

    /** Coefficients donnés en texte : un caractère ou une chaîne lève NumberFormatException « ce n'est pas un nombre ». */
    public ArrayList<Double> racines(String a, String b, String c) {
        return racines(Coefficient.lire("a", a), Coefficient.lire("b", b), Coefficient.lire("c", c));
    }

    public ArrayList<Double> racines(double a, double b, double c) {
        verifierFini(a, "a");
        verifierFini(b, "b");
        verifierFini(c, "c");
        if (a == 0) {
            throw new IllegalArgumentException("a = 0 : ce n'est pas une équation du second degré");
        }
        ArrayList<Double> r = new ArrayList<>(2);
        Etendu A = Etendu.of(a), B = Etendu.of(b), C = Etendu.of(c);

        if (c == 0) {
            // x·(a·x + b) = 0
            if (b == 0) {
                r.add(0.0); // racine double 0
                return r;
            }
            double x = B.oppose().divise(A).enDouble();
            r.add(Math.min(0.0, x));
            r.add(Math.max(0.0, x));
            return r;
        }

        if (b == 0) {
            // a·x² + c = 0 : x = ±√(−c/a), calculé une seule fois pour que les deux racines soient opposées exactement
            Etendu t = C.oppose().divise(A);
            if (t.signe() < 0) {
                return r;
            }
            double s = t.racineCarree().enDouble();
            r.add(-s);
            r.add(s);
            return r;
        }

        // Δ = b² - 4ac, calculé mantisse par mantisse avec les erreurs d'arrondi exactes (Kahan)
        double p = B.m * B.m;
        double dp = Math.fma(B.m, B.m, -p);
        double quatreAm = 4 * A.m;            // exact
        double q = quatreAm * C.m;
        double dq = Math.fma(quatreAm, C.m, -q);
        int e1 = 2 * B.e, e2 = A.e + C.e;
        int emax = Math.max(e1, e2);
        // un terme 2^2000 fois plus petit que l'autre est négligeable : on borne le décalage
        int s1 = Math.max(e1 - emax, -2000), s2 = Math.max(e2 - emax, -2000);
        double d = (Math.scalb(p, s1) - Math.scalb(q, s2)) + (Math.scalb(dp, s1) - Math.scalb(dq, s2)); // Δ = d · 2^emax

        if (d < 0) {
            return r; // pas de racine réelle
        }
        if (d == 0) {
            r.add(B.oppose().divise(A).foisPuissanceDeDeux(-1).enDouble()); // -b / (2a)
            return r;
        }

        Etendu racineDelta = Etendu.of(d, emax).racineCarree();
        // q = -(b + signe(b)·√Δ) / 2 : deux termes de même signe, aucune soustraction
        Etendu signeB = (b > 0) ? racineDelta : racineDelta.oppose();
        Etendu Q = B.plus(signeB).oppose().foisPuissanceDeDeux(-1);
        double r1 = Q.divise(A).enDouble();
        double r2 = C.divise(Q).enDouble(); // Viète
        r.add(Math.min(r1, r2));
        r.add(Math.max(r1, r2));
        return r;
    }

    private static void verifierFini(double v, String nom) {
        if (!Double.isFinite(v)) {
            throw new IllegalArgumentException("Le coefficient " + nom + " doit être un réel fini (reçu " + v + ")");
        }
    }
}
