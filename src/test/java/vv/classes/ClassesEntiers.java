package vv.classes;

/**
 * Les 9 classes d'équivalence du domaine ENTIERS MACHINE (int, 32 bits), appliquées à a, b et c :
 *
 *   MIN | MIN + 1 | négatif normal | −1 | zéro | +1 | positif normal | MAX − 1 | MAX
 *
 * Partitions d'équivalence et valeurs limites (ISTQB) : les deux bornes de int et leurs voisines,
 * la frontière du signe (−1, 0, +1), et un représentant nominal de chaque côté.
 *
 * Tout int est exact en double (|x| ≤ 2³¹ < 2⁵³) : la conversion vers le solveur ne perd rien.
 * Les défauts visés sont ceux des produits : b² et 4ac atteignent 2⁶² et 2⁶⁴, au-delà de 2⁵³
 * (plus grand entier exact en double) et de Long.MAX_VALUE ; un solveur qui calcule Δ en int ou en long déborde.
 */
public final class ClassesEntiers {

    private ClassesEntiers() {
    }

    @ClasseEquivalence(ordre = 1, code = "IMIN", libelle = "Integer.MIN_VALUE",
            justification = "borne basse de int : −MIN déborde (reste MIN) ; b² = 2⁶² dépasse 2⁵³")
    public static double minimum() {
        return Integer.MIN_VALUE;
    }

    @ClasseEquivalence(ordre = 2, code = "IMIN1", libelle = "Integer.MIN_VALUE + 1",
            justification = "voisine intérieure de la borne : b² = (2³¹ − 1)² a 62 bits significatifs, arrondi en double")
    public static double minimumPlusUn() {
        return Integer.MIN_VALUE + 1;
    }

    @ClasseEquivalence(ordre = 3, code = "INN", libelle = "entier négatif normal",
            justification = "cas nominal ; −7 et non −5 pour ne pas créer de symétrie artificielle avec INP")
    public static double negatifNormal() {
        return -7;
    }

    @ClasseEquivalence(ordre = 4, code = "IM1", libelle = "−1",
            justification = "frontière du signe, côté négatif : plus grand entier négatif")
    public static double moinsUn() {
        return -1;
    }

    @ClasseEquivalence(ordre = 5, code = "IZ", libelle = "zéro",
            justification = "frontière : a = 0 doit lever une exception ; b = 0 ou c = 0 simplifient l'équation")
    public static double zero() {
        return 0;
    }

    @ClasseEquivalence(ordre = 6, code = "IP1", libelle = "+1",
            justification = "frontière du signe, côté positif : élément neutre de la multiplication")
    public static double plusUn() {
        return 1;
    }

    @ClasseEquivalence(ordre = 7, code = "INP", libelle = "entier positif normal",
            justification = "cas nominal")
    public static double positifNormal() {
        return 5;
    }

    @ClasseEquivalence(ordre = 8, code = "IMAX1", libelle = "Integer.MAX_VALUE − 1",
            justification = "voisine intérieure de la borne haute")
    public static double maximumMoinsUn() {
        return Integer.MAX_VALUE - 1;
    }

    @ClasseEquivalence(ordre = 9, code = "IMAX", libelle = "Integer.MAX_VALUE",
            justification = "borne haute de int : MAX + 1 déborde ; 4ac ≈ 1,8·10¹⁹ dépasse Long.MAX_VALUE (9,2·10¹⁸)")
    public static double maximum() {
        return Integer.MAX_VALUE;
    }
}
