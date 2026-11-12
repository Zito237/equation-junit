package vv.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import vv.modele.CasDeTest;
import vv.modele.TypeOracle;
import vv.oracles.OracleDifferentiel;
import vv.reflexion.SolveurReflexif;

/**
 * Tests BOÎTE BLANCHE (séance 5) : chaque cas vise une branche que le rapport JaCoCo montrait non couverte
 * par les 81 cas pairwise. Les entrées sont choisies en lisant le code ; le solveur reste chargé par réflexivité.
 *
 * Branches visées (SolveurRobuste, Etendu) :
 *   - b = 0 et −c/a < 0 : aucune racine réelle ;
 *   - coefficient sous-normal : calcul de l'exposant d'un sous-normal ;
 *   - |√Δ| plus de 2^1100 fois plus grand que |b| : l'addition néglige le petit terme ;
 *   - racine au-delà de 2^2000 ou en deçà de 2^-2000 : retour aux double par ±∞ et ±0 ;
 *   - coefficient NaN ou infini : refus explicite.
 */
@Tag("boite-blanche")
@DisplayName("Boîte blanche : branches non couvertes par le pairwise (JaCoCo)")
class BoiteBlancheTest {

    static Stream<Arguments> branches() {
        double min = Double.MIN_VALUE, max = Double.MAX_VALUE;
        return Stream.of(
                Arguments.of("b = 0 et −c/a < 0 : pas de racine", 3.0, 0.0, 3.0),
                Arguments.of("coefficients sous-normaux : racines ±1", min, 0.0, -min),
                Arguments.of("sous-normal pour a, b ≠ 0 : racine énorme", min, 1.0, -1.0),
                Arguments.of("√Δ ≫ |b| (2^1100) : b négligé, racines ±1", 1e300, min, -1e300),
                Arguments.of("−b/a > 2^2000 : racine −∞", min, max, 0.0),
                Arguments.of("−b/a < 2^-2000 : racine −0", max, min, 0.0));
    }

    @ParameterizedTest(name = "{index} → {0} : a = {1}, b = {2}, c = {3}")
    @MethodSource("branches")
    @DisplayName("branches extrêmes jugées par l'oracle différentiel")
    @CasDeTest(scenario = "chaque entrée force une branche du solveur que le pairwise n'atteint pas",
            donnees = "6 cas choisis d'après le rapport JaCoCo (@MethodSource)", oracle = TypeOracle.DIFFERENTIEL,
            resultatAttendu = "racines exactes en BigDecimal, arrondies ; ±∞ ou ±0 hors des double",
            mesure = "erreur mixte |y − ŷ| <= Tol·|ŷ| + 2·MIN_VALUE", tolerance = "16·κ·ε par racine")
    void brancheExtreme(String branche, double a, double b, double c) {
        List<Double> y = SolveurReflexif.courant().racines(a, b, c);
        List<String> v = OracleDifferentiel.verifier(a, b, c, y);
        assertTrue(v.isEmpty(), () -> branche + " : obtenu " + y + " ; " + String.join(" ; ", v));
    }

    /**
     * SOLUTIONS MANUFACTURÉES (séance 6) : données exactement représentables, racines connues exactement.
     * Ajoutées après le test de mutation : chacune tue des mutants que les oracles à tolérance laissaient vivre.
     */
    static Stream<Arguments> solutionsExactes() {
        double min = Double.MIN_VALUE;
        return Stream.of(
                // Δ = 0 exactement : une seule racine (tue « d == 0 » supprimé et « d < 0 » devenu « d <= 0 »)
                Arguments.of("Δ = 0 exactement : (x − 1)²", 1.0, -2.0, 1.0, List.of(1.0)),
                Arguments.of("Δ = 0 exactement : (x + 2)²", 1.0, 4.0, 4.0, List.of(-2.0)),
                // b = c = 0 : racine double 0 (tue le test « b == 0 » supprimé dans la branche c = 0)
                Arguments.of("b = c = 0 : racine double 0", 2.0, 0.0, 0.0, List.of(0.0)),
                // exemple de Kahan : b² ≈ 3,6e16 > 2^53, l'arrondi de b² efface Δ = 7,5625 ; √Δ = 2,75 exactement.
                // Racines exactes : 1 et 94906268,375 / 94906265,625 (tue les mutants du discriminant précis)
                Arguments.of("Kahan : Δ = 7,5625 caché par l'arrondi de b²", 94906265.625, -189812534.0, 94906268.375,
                        List.of(1.0, 94906268.375 / 94906265.625)),
                // tout est sous-normal : sans normalisation de la mantisse, b² et 4ac valent 0 (tue « exposant » muté)
                Arguments.of("coefficients sous-normaux : (x − 1)(x − 2)", min, -3 * min, 2 * min, List.of(1.0, 2.0)));
    }

    @ParameterizedTest(name = "{index} → {0} : a = {1}, b = {2}, c = {3} → {4}")
    @MethodSource("solutionsExactes")
    @DisplayName("solutions manufacturées : racines exactes attendues")
    @CasDeTest(scenario = "données exactes dont les racines sont connues exactement",
            donnees = "5 équations fabriquées (@MethodSource), dont l'exemple de Kahan", oracle = TypeOracle.CONSTRUCTION,
            resultatAttendu = "la liste exacte des racines, un seul arrondi par racine",
            mesure = "égalité de listes", tolerance = "0 : chaque racine exacte est un double ou un quotient correctement arrondi")
    void solutionExacte(String cas, double a, double b, double c, List<Double> attendu) {
        List<Double> y = SolveurReflexif.courant().racines(a, b, c);
        assertEquals(attendu, y, cas);
    }

    static Stream<Arguments> nonFinis() {
        double nan = Double.NaN, inf = Double.POSITIVE_INFINITY;
        return Stream.of(
                Arguments.of("a", nan, 1.0, 1.0), Arguments.of("b", 1.0, nan, 1.0), Arguments.of("c", 1.0, 1.0, nan),
                Arguments.of("a", inf, 1.0, 1.0), Arguments.of("b", 1.0, -inf, 1.0), Arguments.of("c", 1.0, 1.0, inf));
    }

    @ParameterizedTest(name = "{index} → {0} non fini : a = {1}, b = {2}, c = {3}")
    @MethodSource("nonFinis")
    @DisplayName("un coefficient NaN ou infini est refusé")
    @CasDeTest(scenario = "un coefficient qui n'est pas un réel fini est refusé au lieu de donner un résultat absurde",
            donnees = "NaN, +∞ ou −∞ tour à tour en a, b, c (@MethodSource)", oracle = TypeOracle.IMPLICITE,
            resultatAttendu = "IllegalArgumentException qui nomme le coefficient")
    void coefficientNonFini(String nom, double a, double b, double c) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> SolveurReflexif.courant().racines(a, b, c));
        assertTrue(String.valueOf(e.getMessage()).contains(nom), "le message doit nommer " + nom + " : " + e.getMessage());
    }

    @ParameterizedTest(name = "{index} → texte null pour {0}")
    @MethodSource("textesNull")
    @DisplayName("un coefficient texte absent (null) n'est pas un nombre")
    @CasDeTest(scenario = "l'absence de texte est refusée comme un texte invalide",
            donnees = "null tour à tour en a, b, c (@MethodSource)", oracle = TypeOracle.IMPLICITE,
            resultatAttendu = "NumberFormatException « ce n'est pas un nombre »")
    void texteNull(String nom, String a, String b, String c) {
        NumberFormatException e = assertThrows(NumberFormatException.class,
                () -> SolveurReflexif.courant().racines(a, b, c));
        assertTrue(String.valueOf(e.getMessage()).contains("pas un nombre"), e.getMessage());
    }

    static Stream<Arguments> textesNull() {
        return Stream.of(Arguments.of("a", null, "1", "1"), Arguments.of("b", "1", null, "1"), Arguments.of("c", "1", "1", null));
    }
}
