package vv.modele;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;

import vv.reflexion.SolveurReflexif;

/**
 * Extension JUnit 5 qui traduit chaque exécution en l'un des quatre verdicts du cours
 * et imprime, en fin de campagne, un tableau par cas de test avec son modèle formel.
 * Le tableau est aussi écrit dans target/rapport-verdicts-&lt;solveur&gt;.md.
 */
public class RapportVerdicts implements TestWatcher {

    private static final ExtensionContext.Namespace NS = ExtensionContext.Namespace.create(RapportVerdicts.class);

    @Override
    public void testSuccessful(ExtensionContext ctx) {
        enregistrer(ctx, Verdict.PASS, null);
    }

    @Override
    public void testFailed(ExtensionContext ctx, Throwable cause) {
        enregistrer(ctx, cause instanceof AssertionError ? Verdict.FAIL : Verdict.ERROR, cause);
    }

    @Override
    public void testAborted(ExtensionContext ctx, Throwable cause) {
        enregistrer(ctx, Verdict.INCONCLUSIF, cause);
    }

    private void enregistrer(ExtensionContext ctx, Verdict v, Throwable cause) {
        Method m = ctx.getRequiredTestMethod();
        CasDeTest cas = m.getAnnotation(CasDeTest.class);
        if (cas == null) return;
        Rapport r = ctx.getRoot().getStore(NS).getOrComputeIfAbsent("rapport", k -> new Rapport(), Rapport.class);
        // classe exécutée et non classe déclarante : une sous-classe @Domaine a sa propre ligne
        r.ajouter(ctx.getRequiredTestClass().getSimpleName() + "." + m.getName(), cas, v, ctx.getDisplayName(), cause);
    }

    /** Agrégat d'une campagne ; fermé (donc imprimé) par JUnit à la fin du plan de test. */
    static final class Rapport implements ExtensionContext.Store.CloseableResource {

        private static final class Ligne {
            CasDeTest cas;
            final Map<Verdict, Integer> comptes = new EnumMap<>(Verdict.class);
            String premierEchec;
        }

        private final Map<String, Ligne> lignes = new LinkedHashMap<>();

        synchronized void ajouter(String cle, CasDeTest cas, Verdict v, String nomCas, Throwable cause) {
            Ligne l = lignes.computeIfAbsent(cle, k -> new Ligne());
            l.cas = cas;
            l.comptes.merge(v, 1, Integer::sum);
            if ((v == Verdict.FAIL || v == Verdict.ERROR) && l.premierEchec == null) {
                String msg = cause == null ? "" : String.valueOf(cause.getMessage());
                msg = msg.replace('\n', ' ');
                if (msg.length() > 140) msg = msg.substring(0, 140) + "…";
                l.premierEchec = nomCas + " → " + msg;
            }
        }

        @Override
        public synchronized void close() {
            String solveur;
            try {
                solveur = SolveurReflexif.courant().nomCourt();
            } catch (RuntimeException e) {
                solveur = "inconnu";
            }
            List<String> out = new ArrayList<>();
            out.add("");
            out.add("# Verdicts — solveur testé : " + solveur);
            out.add("");
            out.add("| Cas de test | Oracle | PASS | FAIL | ERROR | INCONCLUSIF | Premier échec |");
            out.add("|---|---|---|---|---|---|---|");
            for (Map.Entry<String, Ligne> e : lignes.entrySet()) {
                Ligne l = e.getValue();
                out.add("| " + e.getKey() + " | " + l.cas.oracle() + " | "
                        + l.comptes.getOrDefault(Verdict.PASS, 0) + " | "
                        + l.comptes.getOrDefault(Verdict.FAIL, 0) + " | "
                        + l.comptes.getOrDefault(Verdict.ERROR, 0) + " | "
                        + l.comptes.getOrDefault(Verdict.INCONCLUSIF, 0) + " | "
                        + Optional.ofNullable(l.premierEchec).orElse("") + " |");
            }
            out.add("");
            out.add("Modèle formel de chaque cas (annotation @CasDeTest) :");
            for (Map.Entry<String, Ligne> e : lignes.entrySet()) {
                CasDeTest c = e.getValue().cas;
                out.add("- " + e.getKey() + " — SC : " + c.scenario() + " ; DE : " + c.donnees()
                        + " ; O : " + c.oracle().description + " ; RA : " + c.resultatAttendu()
                        + " ; N : " + c.mesure() + " ; Tol : " + c.tolerance());
            }
            String texte = String.join(System.lineSeparator(), out);
            if (!Boolean.getBoolean("rapport.silencieux")) {
                System.out.println(texte);
            }
            try {
                Path dir = Path.of("target");
                Files.createDirectories(dir);
                Files.writeString(dir.resolve("rapport-verdicts-" + solveur + ".md"), texte, StandardCharsets.UTF_8);
            } catch (IOException ignored) {
                // le rapport console suffit si target/ n'est pas accessible
            }
        }
    }
}
