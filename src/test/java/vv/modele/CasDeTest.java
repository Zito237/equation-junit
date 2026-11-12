package vv.modele;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Annotation portée par CHAQUE cas de test : elle écrit le modèle formel du cours
 * (SC, DE, g, O, RA, N, Tol) directement sur la méthode.
 *
 * Elle est méta-annotée par @ExtendWith(RapportVerdicts.class) : poser @CasDeTest suffit
 * pour que le verdict (PASS, FAIL, ERROR, INCONCLUSIF) de chaque exécution soit enregistré
 * et résumé à la fin, avec ce modèle.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@ExtendWith(RapportVerdicts.class)
public @interface CasDeTest {

    /** SC : la situation vérifiée. */
    String scenario();

    /** DE : d'où viennent les données d'entrée. */
    String donnees();

    /** O : la famille d'oracle. */
    TypeOracle oracle();

    /** RA : résultat attendu, ou la raison de son absence. */
    String resultatAttendu() default "aucun : oracle sans résultat attendu";

    /** N : mesure d'erreur. */
    String mesure() default "—";

    /** Tol : tolérance et sa justification. */
    String tolerance() default "—";
}
