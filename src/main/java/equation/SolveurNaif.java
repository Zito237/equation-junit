package equation;

import java.util.ArrayList;

/**
 * Version naïve : la formule du lycée, telle que présentée dans le cours (slide 51),
 * avec un résultat sous forme d'ArrayList.
 *
 * Contrat commun aux trois solveurs :
 *   - a = 0                 -> IllegalArgumentException (ce n'est pas une équation du second degré)
 *   - Δ < 0                 -> liste vide (pas de racine réelle)
 *   - Δ = 0                 -> une racine (double)
 *   - Δ > 0                 -> deux racines, triées par ordre croissant
 *
 * Défauts connus (que les tests doivent trouver) : élimination catastrophique quand b² ≫ 4ac,
 * débordement de b² et de 4ac, absorption pour des coefficients très petits.
 */
public class SolveurNaif {

    /** Coefficients donnés en texte : un caractère ou une chaîne lève NumberFormatException « ce n'est pas un nombre ». */
    public ArrayList<Double> racines(String a, String b, String c) {
        return racines(Coefficient.lire("a", a), Coefficient.lire("b", b), Coefficient.lire("c", c));
    }

    public ArrayList<Double> racines(double a, double b, double c) {
        if (a == 0) {
            throw new IllegalArgumentException("a = 0 : ce n'est pas une équation du second degré");
        }
        ArrayList<Double> r = new ArrayList<>();
        double d = b * b - 4 * a * c;
        if (d < 0) {
            return r;
        }
        if (d == 0) {
            r.add(-b / (2 * a));
            return r;
        }
        double r1 = (-b - Math.sqrt(d)) / (2 * a);
        double r2 = (-b + Math.sqrt(d)) / (2 * a);
        r.add(Math.min(r1, r2));
        r.add(Math.max(r1, r2));
        return r;
    }
}
