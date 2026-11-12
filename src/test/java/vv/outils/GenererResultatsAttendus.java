package vv.outils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import vv.classes.ClassesEntiers;
import vv.classes.ClassesEquivalence;
import vv.classes.ClassesEquivalence.Classe;
import vv.oracles.MesureErreur;
import vv.oracles.ReferenceExacte;
import vv.pairwise.GenerateurPairwise;

/**
 * Écrit le fichier des RÉSULTATS ATTENDUS (RA) des cas pairwise de chaque domaine
 * (81 flottants IEEE 754, 81 entiers machine) : src/test/resources/resultats-attendus.csv.
 *
 * Les RA sont calculés UNE FOIS en BigDecimal (60 chiffres au moins), avec leur tolérance 16·κ·ε,
 * puis figés dans le fichier : l'oracle par référence compare ensuite chaque solveur à ces valeurs écrites,
 * sans rien recalculer. À relancer seulement si les classes d'équivalence changent.
 */
public final class GenererResultatsAttendus {

    /** Les domaines, dans l'ordre du fichier. */
    static final Class<?>[] DOMAINES = {ClassesEquivalence.class, ClassesEntiers.class};

    public static void main(String[] args) throws IOException {
        List<String> out = new ArrayList<>();
        out.add("# Résultats attendus (RA) des 81 cas pairwise de chaque domaine, calculés en BigDecimal puis figés.");
        out.add("# Colonnes : classes ; a ; b ; c ; RA ; Tol (relative, par racine) ; justification");
        int n = 0;
        for (Class<?> domaine : DOMAINES) {
            out.add("# Domaine : " + domaine.getSimpleName());
            List<Classe> cl = ClassesEquivalence.decouvrir(domaine);
            for (int[] l : GenerateurPairwise.paires(new int[]{cl.size(), cl.size(), cl.size()})) {
                out.add(ligne(cl.get(l[0]), cl.get(l[1]), cl.get(l[2])));
                n++;
            }
        }
        Path f = Path.of("src", "test", "resources", "resultats-attendus.csv");
        Files.writeString(f, String.join(System.lineSeparator(), out) + System.lineSeparator(), StandardCharsets.UTF_8);
        System.out.println(n + " résultats attendus écrits dans " + f);
    }

    private static String ligne(Classe ca, Classe cb, Classe cc) {
        double a = ca.valeur(), b = cb.valeur(), c = cc.valeur();
        String classes = ca.info().code() + "," + cb.info().code() + "," + cc.info().code();
        String ra, tol, pourquoi;
        if (a == 0) {
            ra = "EXCEPTION";
            tol = "";
            pourquoi = "a = 0 : pas une équation du second degré";
        } else {
            ReferenceExacte r = ReferenceExacte.de(a, b, c);
            List<String> racines = new ArrayList<>(), tols = new ArrayList<>(), kappas = new ArrayList<>();
            for (int i = 0; i < r.racines.size(); i++) {
                racines.add(Double.toString(r.racines.get(i)));
                double k = r.kappa(i);
                double t = r.malConditionne ? 16 * Math.sqrt(ReferenceExacte.EPS)
                        : Double.isNaN(k) ? 0 : MesureErreur.tolerance(k);
                tols.add(String.format(Locale.ROOT, "%.3e", t));
                kappas.add(String.format(Locale.ROOT, "%.3g", k));
            }
            ra = "[" + String.join(", ", racines) + "]";
            tol = String.join(", ", tols);
            pourquoi = r.racines.isEmpty() ? "Δ < 0 : pas de racine réelle"
                    : r.malConditionne ? "Δ ≈ 0 : racine double, Tol = 16·√ε"
                    : "Tol = 16·κ·ε, κ = " + String.join(", ", kappas);
        }
        return String.join(" ; ", classes, Double.toString(a), Double.toString(b), Double.toString(c), ra, tol, pourquoi);
    }

    private GenererResultatsAttendus() {
    }
}
