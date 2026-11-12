package vv.oracles;

import java.util.ArrayList;
import java.util.List;

/**
 * Oracle différentiel : compare la sortie du solveur à la référence BigDecimal (ReferenceExacte).
 *
 *  - N : erreur mixte par racine, après appariement des listes triées (slide 66, « ensembles ») ;
 *  - Tol : 16·κ·ε, calculée racine par racine à partir du conditionnement exact (slide 67) ;
 *  - zone mal conditionnée (Δ ≈ 0) : 0, 1 ou 2 racines acceptées, chacune à moins de
 *    4 demi-largeurs de zone de −b/2a (on ne garde que ~√ε en relatif, slide 67).
 */
public final class OracleDifferentiel {

    private OracleDifferentiel() {
    }

    /** @return la liste des violations (vide = l'oracle accepte). */
    public static List<String> verifier(double a, double b, double c, List<Double> y) {
        ReferenceExacte ref = ReferenceExacte.de(a, b, c);
        List<String> v = new ArrayList<>();
        if (ref.malConditionne) {
            if (y.size() > 2) v.add("plus de deux racines : " + y);
            double centre = ref.centre(), largeur = 4 * ref.demiLargeurZone();
            for (double r : y) {
                if (!MesureErreur.proche(r, centre, 0, largeur + MesureErreur.ATOL)) {
                    v.add("Δ ≈ 0 : la racine " + r + " est trop loin de −b/2a = " + centre + " (largeur " + largeur + ")");
                }
            }
            return v;
        }
        if (y.size() != ref.racines.size()) {
            v.add("nombre de racines : " + y.size() + " au lieu de " + ref.racines.size()
                    + " (Δ exact " + (ref.delta.signum() > 0 ? "> 0" : ref.delta.signum() < 0 ? "< 0" : "= 0")
                    + ") ; obtenu " + y + ", attendu " + ref.racines);
            return v;
        }
        for (int i = 0; i < y.size(); i++) {
            double attendu = ref.racines.get(i);
            double kappa = ref.kappa(i);
            double rtol = Double.isNaN(kappa) ? 0 : MesureErreur.tolerance(kappa);
            if (!MesureErreur.proche(y.get(i), attendu, rtol, MesureErreur.ATOL)) {
                v.add(String.format(java.util.Locale.ROOT,
                        "racine %d : obtenu %s, référence %s, erreur relative %.3g > Tol = 16·κ·ε = %.3g (κ = %.3g)",
                        i + 1, y.get(i), attendu, MesureErreur.relative(y.get(i), attendu), rtol, kappa));
            }
        }
        return v;
    }
}
