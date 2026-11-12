package vv.pairwise;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.params.provider.ArgumentsSource;

import vv.classes.ClassesEquivalence;

/**
 * LE DÉCORATEUR pairwise pour JUnit 5.
 *
 * Posé sur un @ParameterizedTest, il :
 *   1. découvre par réflexivité les fonctions @ClasseEquivalence de {@link #classes()} (les 9 classes) ;
 *   2. génère un tableau de couverture 2 à 2 sur {@link #parametres()} paramètres (a, b, c) ;
 *   3. fournit chaque ligne au test, chaque argument portant le code de sa classe (GN, Z, PP...).
 *
 * Exemple :
 * <pre>
 *   &#64;ParameterizedTest(name = "a∈{0}  b∈{1}  c∈{2}")
 *   &#64;PairwiseSource
 *   void monTest(double a, double b, double c) { ... }
 * </pre>
 *
 * Une classe de test annotée {@link Domaine} remplace {@link #classes()} : c'est ainsi qu'une sous-classe
 * rejoue les mêmes méthodes de test sur un autre domaine (entiers machine).
 *
 * JUnit 5 ne fournit pas de générateur pairwise ; c'est le point d'extension standard
 * {@code @ArgumentsSource} qui permet de brancher le nôtre comme une annotation ordinaire.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@ArgumentsSource(PairwiseArgumentsProvider.class)
public @interface PairwiseSource {

    /** Classe qui contient les fonctions @ClasseEquivalence. */
    Class<?> classes() default ClassesEquivalence.class;

    /** Nombre de paramètres du test (3 : a, b, c). */
    int parametres() default 3;

    /** Force de la couverture : 2 = toutes les paires ; une valeur >= parametres donne le test exhaustif. */
    int force() default 2;
}
