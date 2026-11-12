package vv.classes;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marque une fonction qui fournit le représentant d'une classe d'équivalence.
 * Le générateur pairwise retrouve ces fonctions par réflexivité et les trie par {@link #ordre()}.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ClasseEquivalence {

    /** Position de la classe (1 à 9) : fixe l'ordre, donc les lettres a..i de jenny. */
    int ordre();

    /** Code court affiché dans le nom de chaque cas de test, ex. « GN ». */
    String code();

    /** Libellé, ex. « grand négatif ». */
    String libelle();

    /** Pourquoi ce représentant : quel défaut il vise. */
    String justification();
}
