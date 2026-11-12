package vv.outils;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectPackage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;

import vv.modele.Verdict;
import vv.reflexion.SolveurReflexif;

/**
 * Tableau « détecte / ne détecte pas » du TP : exécute TOUTE la suite de tests contre chaque solveur,
 * en changeant seulement la propriété solveur.classe (réflexivité), puis affiche une ligne par cas de test.
 *
 *   mvn -q test-compile exec:java          (ou la configuration VS Code « Tableau de détection »)
 *
 * Le tableau est aussi écrit dans target/tableau-detection.md.
 */
public final class TableauDetection {

    /** Les solveurs comparés, désignés uniquement par leur nom. */
    static final String[] SOLVEURS = {"equation.SolveurNaif", "equation.SolveurStableCours", "equation.SolveurRobuste"};

    public static void main(String[] args) throws IOException {
        String[] solveurs = args.length > 0 ? args : SOLVEURS;
        System.setProperty("rapport.silencieux", "true");
        Map<String, Map<String, EnumMap<Verdict, Integer>>> resultats = new LinkedHashMap<>();
        Map<String, String> premierEchec = new LinkedHashMap<>();

        for (String s : solveurs) {
            System.setProperty(SolveurReflexif.PROPRIETE_CLASSE, s);
            Collecteur col = new Collecteur(s);
            Launcher launcher = LauncherFactory.create();
            LauncherDiscoveryRequest req = LauncherDiscoveryRequestBuilder.request().selectors(selectPackage("vv.tests")).build();
            launcher.execute(req, col);
            col.comptes.forEach((cas, v) -> resultats.computeIfAbsent(cas, k -> new LinkedHashMap<>()).put(s, v));
            col.premierEchec.forEach((cas, msg) -> premierEchec.putIfAbsent(cas + "|" + s, msg));
            System.out.println("Exécuté contre " + s);
        }

        List<String> out = new ArrayList<>();
        StringBuilder entete = new StringBuilder("| Cas de test |");
        StringBuilder sep = new StringBuilder("|---|");
        for (String s : solveurs) {
            entete.append(' ').append(court(s)).append(" |");
            sep.append("---|");
        }
        out.add("# Tableau de détection (FAIL + ERROR sur nombre d'exécutions ; I = inconclusifs)");
        out.add("");
        out.add(entete.toString());
        out.add(sep.toString());
        for (Map.Entry<String, Map<String, EnumMap<Verdict, Integer>>> e : resultats.entrySet()) {
            StringBuilder l = new StringBuilder("| " + e.getKey() + " |");
            for (String s : solveurs) {
                EnumMap<Verdict, Integer> v = e.getValue().getOrDefault(s, new EnumMap<>(Verdict.class));
                int echecs = v.getOrDefault(Verdict.FAIL, 0) + v.getOrDefault(Verdict.ERROR, 0);
                int total = v.values().stream().mapToInt(Integer::intValue).sum();
                int inconcl = v.getOrDefault(Verdict.INCONCLUSIF, 0);
                l.append(' ').append(echecs > 0 ? "**détecte** " + echecs + "/" + total : "— 0/" + total)
                        .append(inconcl > 0 ? " (I " + inconcl + ")" : "").append(" |");
            }
            out.add(l.toString());
        }
        out.add("");
        out.add("Premier échec par cas et par solveur :");
        premierEchec.forEach((k, msg) -> out.add("- " + k.replace("|", " / ") + " : " + msg));
        String texte = String.join(System.lineSeparator(), out);
        System.out.println();
        System.out.println(texte);
        Files.createDirectories(Path.of("target"));
        Files.writeString(Path.of("target", "tableau-detection.md"), texte, StandardCharsets.UTF_8);
    }

    private static String court(String s) {
        return s.substring(s.lastIndexOf('.') + 1);
    }

    /** Écoute l'exécution et compte les verdicts par cas de test (méthode). */
    static final class Collecteur implements TestExecutionListener {
        final String solveur;
        final Map<String, EnumMap<Verdict, Integer>> comptes = new LinkedHashMap<>();
        final Map<String, String> premierEchec = new LinkedHashMap<>();
        private TestPlan plan;

        Collecteur(String solveur) {
            this.solveur = solveur;
        }

        @Override
        public void testPlanExecutionStarted(TestPlan testPlan) {
            this.plan = testPlan;
        }

        @Override
        public void executionFinished(TestIdentifier id, TestExecutionResult r) {
            if (!id.isTest()) return;
            TestIdentifier methode = id;
            Optional<TestIdentifier> parent = plan.getParent(id);
            if (parent.isPresent() && parent.get().isContainer() && parent.get().getSource().map(src -> src.toString().contains("MethodSource")).orElse(false)) {
                methode = parent.get();
            }
            String classe = plan.getParent(methode).map(TestIdentifier::getDisplayName).orElse("?");
            String cle = classe + " · " + methode.getDisplayName();
            Verdict v = switch (r.getStatus()) {
                case SUCCESSFUL -> Verdict.PASS;
                case ABORTED -> Verdict.INCONCLUSIF;
                case FAILED -> r.getThrowable().map(t -> t instanceof AssertionError).orElse(false) ? Verdict.FAIL : Verdict.ERROR;
            };
            comptes.computeIfAbsent(cle, k -> new EnumMap<>(Verdict.class)).merge(v, 1, Integer::sum);
            if ((v == Verdict.FAIL || v == Verdict.ERROR) && !premierEchec.containsKey(cle)) {
                String msg = r.getThrowable().map(Throwable::getMessage).orElse("").replace('\n', ' ');
                if (msg.length() > 160) msg = msg.substring(0, 160) + "…";
                premierEchec.put(cle, id.getDisplayName() + " → " + msg);
            }
        }
    }

    private TableauDetection() {
    }
}
