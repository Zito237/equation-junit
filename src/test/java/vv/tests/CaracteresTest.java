package vv.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import vv.modele.CasDeTest;
import vv.modele.TypeOracle;
import vv.reflexion.SolveurReflexif;

/**
 * Coefficients qui ne sont PAS des nombres : a, b ou c est un caractère ou une chaîne.
 *
 * Le solveur est appelé par réflexivité avec des coefficients en texte, racines(String, String, String).
 * Résultat attendu : NumberFormatException dont le message nomme le coefficient fautif et dit
 * « ce n'est pas un nombre, veuillez entrer un nombre ».
 *
 * Données : chaque texte invalide est placé tour à tour en a, en b et en c (les deux autres valant 1, −3, 2),
 * plus le cas où les trois sont des caractères. Rien n'est saisi au clavier : tout est dans le test.
 */
@Tag("caracteres")
@DisplayName("Coefficients non numériques : caractère ou chaîne pour a, b, c")
class CaracteresTest {

    /** Partitions du texte invalide : caractère, mot, nombre mal formé, vide, blanc, valeurs spéciales. */
    static final String[] INVALIDES = {"a", "x", "?", "abc", "deux", "2,5x", "1..2", "", " ", "NaN", "Infinity"};

    private static final String[] VALIDES = {"1", "-3", "2"};
    private static final String[] NOMS = {"a", "b", "c"};

    static Stream<Arguments> textesInvalides() {
        List<Arguments> cas = new ArrayList<>();
        for (String t : INVALIDES) {
            for (int p = 0; p < 3; p++) {
                String[] abc = VALIDES.clone();
                abc[p] = t;
                cas.add(Arguments.of(abc[0], abc[1], abc[2], NOMS[p]));
            }
        }
        cas.add(Arguments.of("a", "b", "c", "a")); // les trois sont des caractères : a est refusé le premier
        return cas.stream();
    }

    @ParameterizedTest(name = "{index} → a = «{0}»  b = «{1}»  c = «{2}»  (refus attendu sur {3})")
    @MethodSource("textesInvalides")
    @DisplayName("un caractère ou une chaîne lève « ce n'est pas un nombre »")
    @CasDeTest(scenario = "a, b ou c n'est pas un nombre : le solveur refuse avec une exception explicite",
            donnees = "11 textes invalides × 3 positions + (« a », « b », « c »)", oracle = TypeOracle.REFERENCE,
            resultatAttendu = "NumberFormatException « <coef> = « texte » : ce n'est pas un nombre, veuillez entrer un nombre »")
    void texteRefuse(String a, String b, String c, String fautif) {
        SolveurReflexif g = SolveurReflexif.courant();
        NumberFormatException e = assertThrows(NumberFormatException.class, () -> g.racines(a, b, c),
                "NumberFormatException attendue pour a = «" + a + "», b = «" + b + "», c = «" + c + "»");
        String msg = String.valueOf(e.getMessage());
        assertTrue(msg.contains("ce n'est pas un nombre"), "le message doit dire « ce n'est pas un nombre », reçu : " + msg);
        assertTrue(msg.startsWith(fautif + " ="), "le message doit nommer le coefficient " + fautif + ", reçu : " + msg);
    }

    @Test
    @DisplayName("des nombres écrits en texte sont acceptés (« 1 », « -3 », « 2 » et « 2,5 », « -7,5 », « 5 »)")
    @CasDeTest(scenario = "un texte qui est un nombre, y compris avec une virgule décimale, est accepté",
            donnees = "(« 1 », « -3 », « 2 ») et (« 2,5 », « -7,5 », « 5 »)", oracle = TypeOracle.REFERENCE,
            resultatAttendu = "[1.0, 2.0] dans les deux cas")
    void nombresEnTexteAcceptes() {
        SolveurReflexif g = SolveurReflexif.courant();
        assertEquals(List.of(1.0, 2.0), g.racines("1", "-3", "2"));
        assertEquals(List.of(1.0, 2.0), g.racines("2,5", "-7,5", "5"));
    }

    @Test
    @DisplayName("« 0 » pour a est un nombre : c'est l'exception du second degré, pas « ce n'est pas un nombre »")
    @CasDeTest(scenario = "a = « 0 » est bien un nombre ; l'erreur est l'absence de terme en x²",
            donnees = "(« 0 », « 1 », « 2 »)", oracle = TypeOracle.REFERENCE,
            resultatAttendu = "IllegalArgumentException « ce n'est pas une équation du second degré »")
    void zeroEnTexte() {
        SolveurReflexif g = SolveurReflexif.courant();
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> g.racines("0", "1", "2"));
        assertFalse(e instanceof NumberFormatException, "« 0 » est un nombre : " + e.getMessage());
        assertTrue(String.valueOf(e.getMessage()).contains("second degré"), e.getMessage());
    }
}
