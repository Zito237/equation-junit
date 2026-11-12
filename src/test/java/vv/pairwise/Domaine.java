package vv.pairwise;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Domaine d'entrée d'une classe de test : la classe qui contient ses fonctions @ClasseEquivalence.
 *
 * Posée sur une sous-classe de test, elle remplace les classes de {@link PairwiseSource} pour toutes les
 * méthodes héritées : la même suite d'oracles est rejouée sur un autre domaine, sans dupliquer le code.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Domaine {

    /** Classe qui contient les fonctions @ClasseEquivalence du domaine. */
    Class<?> value();
}
