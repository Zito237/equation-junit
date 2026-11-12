package vv.modele;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;

import vv.classes.ClassesEquivalence;
import vv.pairwise.Domaine;
import vv.reflexion.SolveurReflexif;

/**
 * Extension JUnit 5 : journal de TOUS les cas exécutés, avec leurs entrées (a, b, c), la sortie du solveur,
 * le résultat attendu et le verdict. En fin de campagne, un tableau par classe de test est imprimé et écrit
 * dans target/journal-cas-&lt;solveur&gt;.md, précédé de la couverture pairwise réellement exécutée
 * (paires de classes vues sur le nombre de paires possibles).
 *
 * Le test appelle {@link #noter} une fois la sortie obtenue ; le verdict est ajouté quand le test se termine.
 */
public class JournalCas implements TestWatcher {

    private static final ExtensionContext.Namespace NS = ExtensionContext.Namespace.create(JournalCas.class);
    private static final ThreadLocal<Entree> EN_COURS = new ThreadLocal<>();

    record Entree(String classes, double a, double b, double c, String sortie, String attendu) {
    }

    /** Enregistre le cas en cours ; {@code classes} est le code des classes de a, b et c, ex. « GN,Z,PP ». */
    public static void noter(String classes, double a, double b, double c, String sortie, String attendu) {
        EN_COURS.set(new Entree(classes, a, b, c, sortie, attendu));
    }

    @Override
    public void testSuccessful(ExtensionContext ctx) {
        clore(ctx, Verdict.PASS);
    }

    @Override
    public void testFailed(ExtensionContext ctx, Throwable cause) {
        clore(ctx, cause instanceof AssertionError ? Verdict.FAIL : Verdict.ERROR);
    }

    @Override
    public void testAborted(ExtensionContext ctx, Throwable cause) {
        clore(ctx, Verdict.INCONCLUSIF);
    }

    private void clore(ExtensionContext ctx, Verdict v) {
        Entree e = EN_COURS.get();
        EN_COURS.remove();
        if (e == null) return;
        Class<?> classe = ctx.getRequiredTestClass();
        DisplayName nom = classe.getAnnotation(DisplayName.class);
        Domaine domaine = classe.getAnnotation(Domaine.class);
        Journal j = ctx.getRoot().getStore(NS).getOrComputeIfAbsent("journal", k -> new Journal(), Journal.class);
        j.ajouter(nom != null ? nom.value() : classe.getSimpleName(),
                domaine != null ? domaine.value() : ClassesEquivalence.class, e, v);
    }

    /** Formate un nombre : les entiers sans exposant (2147483647 et non 2.147483647E9). */
    public static String nombre(double x) {
        if (x != 0 && x == Math.rint(x) && Math.abs(x) < 1e16) return Long.toString((long) x);
        return Double.toString(x);
    }

    public static String liste(List<Double> l) {
        List<String> s = new ArrayList<>();
        for (double x : l) s.add(nombre(x));
        return "[" + String.join(", ", s) + "]";
    }

    static final class Journal implements ExtensionContext.Store.CloseableResource {

        private static final class Section {
            Class<?> domaine;
            final List<Entree> entrees = new ArrayList<>();
            final List<Verdict> verdicts = new ArrayList<>();
        }

        private final Map<String, Section> sections = new LinkedHashMap<>();

        synchronized void ajouter(String titre, Class<?> domaine, Entree e, Verdict v) {
            Section s = sections.computeIfAbsent(titre, k -> new Section());
            s.domaine = domaine;
            s.entrees.add(e);
            s.verdicts.add(v);
        }

        @Override
        public synchronized void close() {
            String solveur;
            try {
                solveur = SolveurReflexif.courant().nomCourt();
            } catch (RuntimeException ex) {
                solveur = "inconnu";
            }
            List<String> out = new ArrayList<>();
            out.add("");
            out.add("# Journal des cas exécutés — solveur testé : " + solveur);
            for (Map.Entry<String, Section> e : sections.entrySet()) {
                Section s = e.getValue();
                int n = ClassesEquivalence.decouvrir(s.domaine).size();
                out.add("");
                out.add("## " + e.getKey() + " (domaine " + s.domaine.getSimpleName() + ")");
                out.add("");
                out.add("Couverture : " + s.entrees.size() + " cas exécutés (exhaustif : " + n * n * n + ") ; "
                        + pairesVues(s.entrees) + "/" + 3 * n * n + " paires de classes couvertes ; "
                        + compte(s.verdicts, Verdict.PASS) + " PASS, " + compte(s.verdicts, Verdict.FAIL) + " FAIL, "
                        + compte(s.verdicts, Verdict.ERROR) + " ERROR");
                out.add("");
                out.add("| # | Classes (a, b, c) | a | b | c | Sortie du solveur | Résultat attendu (RA) | Verdict |");
                out.add("|---|---|---|---|---|---|---|---|");
                for (int i = 0; i < s.entrees.size(); i++) {
                    Entree x = s.entrees.get(i);
                    out.add("| " + (i + 1) + " | " + x.classes() + " | " + nombre(x.a()) + " | " + nombre(x.b()) + " | "
                            + nombre(x.c()) + " | " + x.sortie() + " | " + x.attendu() + " | " + s.verdicts.get(i) + " |");
                }
            }
            String texte = String.join(System.lineSeparator(), out);
            if (!Boolean.getBoolean("rapport.silencieux")) {
                System.out.println(texte);
            }
            try {
                Path dir = Path.of("target");
                Files.createDirectories(dir);
                Files.writeString(dir.resolve("journal-cas-" + solveur + ".md"), texte, StandardCharsets.UTF_8);
            } catch (IOException ignored) {
                // le journal console suffit si target/ n'est pas accessible
            }
        }

        /** Paires (position i, classe, position j, classe) présentes dans les cas exécutés. */
        private static int pairesVues(List<Entree> entrees) {
            Set<String> vues = new HashSet<>();
            for (Entree x : entrees) {
                String[] c = x.classes().split(",");
                for (int i = 0; i < c.length; i++)
                    for (int j = i + 1; j < c.length; j++) vues.add(i + ":" + c[i] + "|" + j + ":" + c[j]);
            }
            return vues.size();
        }

        private static long compte(List<Verdict> l, Verdict v) {
            return l.stream().filter(v::equals).count();
        }
    }
}
