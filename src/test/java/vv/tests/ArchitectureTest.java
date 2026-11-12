package vv.tests;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import vv.modele.CasDeTest;
import vv.modele.TypeOracle;

/**
 * Vérifie l'exigence « tester sans connaître la classe qui résout l'équation » :
 * aucun fichier de test n'importe le paquetage equation ni n'instancie un solveur directement.
 */
@Tag("reflexivite")
@DisplayName("Réflexivité : les tests ne connaissent pas le solveur")
class ArchitectureTest {

    private static final Pattern INTERDIT = Pattern.compile(
            "^\\s*import\\s+(static\\s+)?equation\\.|new\\s+(equation\\.)?Solveur(Naif|StableCours|Robuste)\\s*\\(|Solveur(Naif|StableCours|Robuste)\\.class",
            Pattern.MULTILINE);

    @Test
    @DisplayName("Aucune référence statique aux classes de solveur dans src/test/java")
    @CasDeTest(scenario = "le code de test n'importe ni n'instancie aucune classe de solveur",
            donnees = "tous les fichiers .java de src/test/java", oracle = TypeOracle.PROPRIETE,
            resultatAttendu = "0 référence : le solveur n'est connu que par son nom, à l'exécution")
    void aucuneReferenceStatique() throws IOException {
        Path racine = Path.of("src", "test", "java");
        assumeTrue(Files.isDirectory(racine), "sources de test introuvables depuis " + Path.of("").toAbsolutePath());
        List<String> fautes = new ArrayList<>();
        try (Stream<Path> fichiers = Files.walk(racine)) {
            for (Path f : (Iterable<Path>) fichiers.filter(p -> p.toString().endsWith(".java"))::iterator) {
                if (INTERDIT.matcher(Files.readString(f, StandardCharsets.UTF_8)).find()) fautes.add(f.toString());
            }
        }
        assertTrue(fautes.isEmpty(), "références statiques à un solveur : " + fautes);
    }
}
