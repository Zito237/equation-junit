package vv.modele;

/**
 * Les quatre verdicts du cours et leur traduction en JUnit 5.
 *
 *   PASS        test réussi (l'oracle accepte, ou l'exception attendue est levée)
 *   FAIL        AssertionError : l'oracle rejette la sortie -> défaut probable du SUT
 *   ERROR       autre exception, imprévue -> défaut du SUT ou du test
 *   INCONCLUSIF hypothèse non satisfaite (Assumptions.assumeTrue) -> on ne peut pas conclure
 *               (ex. la transformation de l'oracle sort des double : hors domaine)
 */
public enum Verdict {
    PASS, FAIL, ERROR, INCONCLUSIF
}
