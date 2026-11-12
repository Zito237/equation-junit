package vv.oracles;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Oracle par propriété : aucun résultat attendu, on vérifie ce que TOUTE bonne réponse satisfait.
 *
 *  1. Résidu (erreur inverse, slide 78) : |a·r² + b·r + c| <= 8·ε·(|a|r² + |b||r| + |c|).
 *  2. Viète : produit  |a·r1·r2 − c| <= 8·ε·|c|  ;  somme  |a·(r1 + r2) + b| <= 8·ε·(|a|(|r1| + |r2|) + |b|).
 *  3. Cohérence avec le signe exact de Δ : 0 racine ⇒ Δ < 0 ; 1 racine ⇒ Δ ≈ 0 ; 2 racines ⇒ Δ > 0
 *     (hors zone mal conditionnée).
 *
 * Tous ces calculs se font en BigDecimal sur les double obtenus : ils sont exacts, sans débordement.
 *
 * Domaine de validité : une racine infinie, sous-normale, ou nulle alors que c ≠ 0, ne peut pas être
 * jugée par le résidu (la vraie racine est peut-être hors des double). La vérification est alors
 * « hors domaine » ; si plus rien n'est vérifiable, le verdict est INCONCLUSIF.
 */
public final class OracleProprietes {

    public static final double K = 8;

    /** Résultat d'un oracle partiel : violations, vérifications faites, vérifications impossibles. */
    public record Resultat(List<String> violations, int verifiees, List<String> horsDomaine) {
    }

    private OracleProprietes() {
    }

    public static Resultat verifier(double a, double b, double c, List<Double> y) {
        List<String> viol = new ArrayList<>();
        List<String> hors = new ArrayList<>();
        int faites = 0;
        ReferenceExacte delta = ReferenceExacte.de(a, b, c); // seul le signe exact de Δ est utilisé
        BigDecimal A = delta.A, B = delta.B, C = delta.C;
        BigDecimal k = new BigDecimal(K * ReferenceExacte.EPS);

        // 3. nombre de racines cohérent avec le signe de Δ
        int s = delta.delta.signum();
        if (!delta.malConditionne) {
            faites++;
            if (y.isEmpty() && s > 0) viol.add("aucune racine alors que b² − 4ac > 0");
            if (y.size() == 1 && s != 0) viol.add("une seule racine annoncée alors que Δ n'est pas nul");
            if (y.size() == 2 && s < 0) viol.add("deux racines réelles alors que b² − 4ac < 0");
        }

        // 1. résidu
        List<BigDecimal> verifiables = new ArrayList<>();
        for (double r : y) {
            if (Double.isNaN(r)) {
                viol.add("racine NaN");
                continue;
            }
            if (Double.isInfinite(r)) {
                hors.add("racine infinie " + r + " : résidu non évaluable");
                continue;
            }
            if (r != 0 && Math.abs(r) < Double.MIN_NORMAL) {
                hors.add("racine sous-normale " + r + " : précision relative perdue");
                continue;
            }
            if (r == 0 && c != 0) {
                hors.add("racine 0 alors que c ≠ 0 : sous-dépassement légitime ou absorption, indécidable sans RA");
                continue;
            }
            BigDecimal R = new BigDecimal(r);
            BigDecimal residu = A.multiply(R).multiply(R).add(B.multiply(R)).add(C).abs();
            BigDecimal echelle = A.abs().multiply(R).multiply(R).add(B.abs().multiply(R.abs())).add(C.abs());
            faites++;
            if (residu.compareTo(k.multiply(echelle)) > 0) {
                viol.add("résidu de " + r + " : " + residu.doubleValue() + " > 8ε·" + echelle.doubleValue()
                        + " (relatif " + (echelle.signum() == 0 ? "∞" : residu.divide(echelle, java.math.MathContext.DECIMAL64).doubleValue()) + ")");
            }
            verifiables.add(R);
        }

        // 2. Viète, si les deux racines sont jugeables
        boolean sousDepassement = y.size() == 2 && b != 0 && y.get(0) == 0 && y.get(1) == 0;
        if (sousDepassement) {
            hors.add("deux racines nulles alors que b ≠ 0 : −b/a sous-dépasse, Viète somme non évaluable");
        }
        if (y.size() == 2 && verifiables.size() == 2 && !sousDepassement) {
            BigDecimal r1 = verifiables.get(0), r2 = verifiables.get(1);
            BigDecimal produit = A.multiply(r1).multiply(r2).subtract(C).abs();
            faites++;
            if (produit.compareTo(k.multiply(C.abs())) > 0) {
                viol.add("Viète produit : a·r1·r2 − c = " + produit.doubleValue() + " (c = " + c + ")");
            }
            BigDecimal somme = A.multiply(r1.add(r2)).add(B).abs();
            BigDecimal ech = A.abs().multiply(r1.abs().add(r2.abs())).add(B.abs());
            faites++;
            if (somme.compareTo(k.multiply(ech)) > 0) {
                viol.add("Viète somme : a·(r1 + r2) + b = " + somme.doubleValue() + " (b = " + b + ")");
            }
        }
        return new Resultat(viol, faites, hors);
    }
}
