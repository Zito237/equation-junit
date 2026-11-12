package vv.oracles;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Calcul de référence en BigDecimal, indépendant des solveurs testés.
 *
 * new BigDecimal(double) reprend la valeur binaire EXACTE du double ; additions et multiplications
 * de BigDecimal sont exactes : Δ = b² − 4ac est donc calculé sans aucune erreur, et son signe est certain.
 * Les racines sont calculées avec la formule du lycée (comme au slide 81), mais avec assez de chiffres
 * pour que l'élimination catastrophique ne puisse plus se produire : 60 chiffres de plus que l'écart
 * entre les ordres de grandeur de b² et de 4ac.
 */
public final class ReferenceExacte {

    /** ε = ulp(1) = 2^-52, la précision machine du cours. */
    public static final double EPS = Math.ulp(1.0);

    /**
     * Zone mal conditionnée : |Δ| <= 16·ε·(b² + |4ac|). Une perturbation des données de l'ordre de ε
     * (l'arrondi de a, b ou c) peut y changer le nombre de racines : 0, 1 ou 2 racines sont alors
     * toutes des réponses défendables (cours : « racine double : κ très grand »).
     */
    public static final BigDecimal ZONE = new BigDecimal(16 * EPS);

    private static final BigDecimal DEUX = BigDecimal.valueOf(2), QUATRE = BigDecimal.valueOf(4);

    public final double a, b, c;
    public final BigDecimal A, B, C, delta, echelleDelta;
    public final boolean malConditionne;
    /** Racines exactes (à la précision de calcul), triées. */
    public final List<BigDecimal> racinesExactes;
    /** Les mêmes, arrondies au double le plus proche (±Infinity si trop grandes, 0 si trop petites). */
    public final List<Double> racines;
    private final MathContext mc;

    private ReferenceExacte(double a, double b, double c) {
        if (a == 0 || !Double.isFinite(a) || !Double.isFinite(b) || !Double.isFinite(c)) {
            throw new IllegalArgumentException("référence définie pour a ≠ 0 et des coefficients finis");
        }
        this.a = a;
        this.b = b;
        this.c = c;
        A = new BigDecimal(a);
        B = new BigDecimal(b);
        C = new BigDecimal(c);
        BigDecimal b2 = B.multiply(B), ac4 = QUATRE.multiply(A).multiply(C);
        delta = b2.subtract(ac4);
        echelleDelta = b2.add(ac4.abs());
        malConditionne = delta.abs().compareTo(ZONE.multiply(echelleDelta)) <= 0;

        int ecart = Math.abs(ordre(b2) - ordre(ac4));
        mc = new MathContext(Math.min(4000, Math.max(60 + ecart, 2 * B.precision() + 20)), RoundingMode.HALF_EVEN);

        List<BigDecimal> r = new ArrayList<>();
        BigDecimal deuxA = DEUX.multiply(A);
        int s = delta.signum();
        if (C.signum() == 0 && B.signum() != 0) {
            r.add(BigDecimal.ZERO);
            r.add(B.negate().divide(A, mc));
        } else if (s == 0) {
            r.add(B.negate().divide(deuxA, mc));
        } else if (s > 0) {
            BigDecimal racineDelta = delta.sqrt(mc);
            r.add(B.negate().subtract(racineDelta).divide(deuxA, mc));
            r.add(B.negate().add(racineDelta).divide(deuxA, mc));
        }
        Collections.sort(r);
        racinesExactes = List.copyOf(r);
        List<Double> d = new ArrayList<>();
        for (BigDecimal x : r) d.add(x.doubleValue());
        racines = List.copyOf(d);
    }

    public static ReferenceExacte de(double a, double b, double c) {
        return new ReferenceExacte(a, b, c);
    }

    private static int ordre(BigDecimal x) {
        return x.signum() == 0 ? 0 : x.precision() - x.scale();
    }

    /**
     * Conditionnement relatif de la i-ème racine par rapport aux coefficients :
     *   κ = (|a|r² + |b||r| + |c|) / (|r| · |2ar + b|),  avec |2ar + b| = √Δ pour une racine simple.
     * Une perturbation relative ε des coefficients déplace la racine d'environ κ·ε (en relatif).
     */
    public double kappa(int i) {
        BigDecimal r = racinesExactes.get(i);
        if (r.signum() == 0) return Double.NaN;
        BigDecimal num = A.abs().multiply(r.pow(2)).add(B.abs().multiply(r.abs())).add(C.abs());
        BigDecimal den = r.abs().multiply(DEUX.multiply(A).multiply(r).add(B).abs());
        if (den.signum() == 0) return Double.POSITIVE_INFINITY;
        return num.divide(den, MathContext.DECIMAL64).doubleValue();
    }

    /** Centre de la zone mal conditionnée : −b/(2a). */
    public double centre() {
        return B.negate().divide(DEUX.multiply(A), mc).doubleValue();
    }

    /** Demi-largeur de la zone : √(16ε(b² + |4ac|)) / |2a|, l'écart des racines quand Δ est au bord de la zone. */
    public double demiLargeurZone() {
        BigDecimal w = ZONE.multiply(echelleDelta).sqrt(MathContext.DECIMAL64);
        return w.divide(DEUX.multiply(A).abs(), MathContext.DECIMAL64).doubleValue();
    }
}
