package equation;

import java.util.ArrayList;

/**
 * Version stable du cours (slide 54) : mise à l'échelle par une puissance de 2
 * puis formule q = -(b + signe(b)·√Δ)/2, x1 = q/a, x2 = c/q (Viète).
 *
 * Elle corrige l'élimination catastrophique et le débordement de b²,
 * mais garde un défaut : quand les coefficients ont des ordres de grandeur trop éloignés
 * (par exemple a = 1e-200 et c = 1e200), la mise à l'échelle par le plus grand
 * fait passer le plus petit sous 4,9e-324 : il devient 0. Le test pairwise le révèle.
 */
public class SolveurStableCours {

    /** Coefficients donnés en texte : un caractère ou une chaîne lève NumberFormatException « ce n'est pas un nombre ». */
    public ArrayList<Double> racines(String a, String b, String c) {
        return racines(Coefficient.lire("a", a), Coefficient.lire("b", b), Coefficient.lire("c", c));
    }

    public ArrayList<Double> racines(double a, double b, double c) {
        if (a == 0) {
            throw new IllegalArgumentException("a = 0 : ce n'est pas une équation du second degré");
        }
        // mise à l'échelle exacte par une puissance de 2
        double m = Math.max(Math.abs(a), Math.max(Math.abs(b), Math.abs(c)));
        int e = Math.getExponent(m);
        a = Math.scalb(a, -e);
        b = Math.scalb(b, -e);
        c = Math.scalb(c, -e);

        ArrayList<Double> r = new ArrayList<>();
        double d = b * b - 4 * a * c;
        if (d < 0) {
            return r;
        }
        if (d == 0) {
            r.add(-b / (2 * a));
            return r;
        }
        double q = -0.5 * (b + Math.copySign(Math.sqrt(d), b));
        double r1 = q / a;
        double r2 = c / q; // Viète : r1 * r2 = c / a
        r.add(Math.min(r1, r2));
        r.add(Math.max(r1, r2));
        return r;
    }
}
