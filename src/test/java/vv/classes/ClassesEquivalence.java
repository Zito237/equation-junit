package vv.classes;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Les 9 classes d'équivalence demandées, appliquées à chaque coefficient a, b et c :
 *
 *   grand négatif | normal négatif | petit négatif | très petit négatif | zéro
 *   | très petit positif | petit positif | normal positif | grand positif
 *
 * c'est-à-dire : grand (±), zéro, très petit (tp−, tp+), normal (n−, n+), petit (p−, p+).
 *
 * Une fonction par classe, chacune portant son annotation @ClasseEquivalence.
 * Le décorateur @PairwiseSource les découvre par réflexivité et les combine deux à deux.
 */
public final class ClassesEquivalence {

    private ClassesEquivalence() {
    }

    @ClasseEquivalence(ordre = 1, code = "GN", libelle = "grand négatif",
            justification = "|x|² = 1e400 dépasse Double.MAX_VALUE (1,8e308) : débordement de b² ou de 4ac")
    public static double grandNegatif() {
        return -1e200;
    }

    @ClasseEquivalence(ordre = 2, code = "NN", libelle = "normal négatif",
            justification = "valeur courante : cas nominal, racines vérifiables à la main")
    public static double normalNegatif() {
        return -2.5;
    }

    @ClasseEquivalence(ordre = 3, code = "PN", libelle = "petit négatif",
            justification = "|x| = 1e-8 : b² et 4ac d'ordres très différents, élimination catastrophique")
    public static double petitNegatif() {
        return -1e-8;
    }

    @ClasseEquivalence(ordre = 4, code = "TPN", libelle = "très petit négatif",
            justification = "|x|² = 1e-400 passe sous Double.MIN_VALUE (4,9e-324) : sous-dépassement, absorption")
    public static double tresPetitNegatif() {
        return -1e-200;
    }

    @ClasseEquivalence(ordre = 5, code = "Z", libelle = "zéro",
            justification = "frontière : a = 0 doit lever une exception ; b = 0 ou c = 0 simplifient l'équation")
    public static double zero() {
        return 0.0;
    }

    @ClasseEquivalence(ordre = 6, code = "TPP", libelle = "très petit positif",
            justification = "symétrique de TPN : sous-dépassement, absorption")
    public static double tresPetitPositif() {
        return 1e-200;
    }

    @ClasseEquivalence(ordre = 7, code = "PP", libelle = "petit positif",
            justification = "symétrique de PN : élimination catastrophique")
    public static double petitPositif() {
        return 1e-8;
    }

    @ClasseEquivalence(ordre = 8, code = "NP", libelle = "normal positif",
            justification = "valeur courante ; 3 et non 2,5 pour ne pas créer de symétries artificielles avec NN")
    public static double normalPositif() {
        return 3.0;
    }

    @ClasseEquivalence(ordre = 9, code = "GP", libelle = "grand positif",
            justification = "symétrique de GN : débordement")
    public static double grandPositif() {
        return 1e200;
    }

    // ------------------------------------------------------------------

    /** Une classe découverte : son annotation et sa valeur. */
    public record Classe(ClasseEquivalence info, double valeur) {
        public String etiquette() {
            return info.code() + "(" + valeur + ")";
        }
    }

    /**
     * Découvre par réflexivité toutes les fonctions @ClasseEquivalence d'une classe,
     * les trie par ordre et les appelle pour obtenir les représentants.
     */
    public static List<Classe> decouvrir(Class<?> conteneur) {
        List<Method> fonctions = new ArrayList<>();
        for (Method m : conteneur.getDeclaredMethods()) {
            if (m.isAnnotationPresent(ClasseEquivalence.class)) {
                if (!Modifier.isStatic(m.getModifiers()) || m.getParameterCount() != 0 || m.getReturnType() != double.class) {
                    throw new IllegalStateException(m + " : une classe d'équivalence doit être « static double f() »");
                }
                fonctions.add(m);
            }
        }
        fonctions.sort(Comparator.comparingInt(m -> m.getAnnotation(ClasseEquivalence.class).ordre()));
        List<Classe> classes = new ArrayList<>();
        for (Method m : fonctions) {
            try {
                classes.add(new Classe(m.getAnnotation(ClasseEquivalence.class), (double) m.invoke(null)));
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
        }
        return classes;
    }
}
