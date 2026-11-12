package vv.oracles;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Lecture des résultats attendus (RA) figés dans src/test/resources/resultats-attendus.csv.
 * C'est la « sortie de référence » de l'oracle par référence : N(y, RA) <= Tol.
 */
public final class ResultatsAttendus {

    /** Un RA : exception attendue, ou liste de racines avec une tolérance relative par racine. */
    public record RA(String classes, double a, double b, double c, boolean exception,
                     List<Double> racines, List<Double> tolerances, boolean racineDouble, String justification) {
    }

    private static final List<RA> TOUS = charger();

    private ResultatsAttendus() {
    }

    public static Optional<RA> pour(double a, double b, double c) {
        for (RA r : TOUS) {
            if (Double.compare(r.a, a) == 0 && Double.compare(r.b, b) == 0 && Double.compare(r.c, c) == 0) {
                return Optional.of(r);
            }
        }
        return Optional.empty();
    }

    private static List<RA> charger() {
        List<RA> liste = new ArrayList<>();
        try (InputStream in = ResultatsAttendus.class.getResourceAsStream("/resultats-attendus.csv")) {
            if (in == null) return liste;
            BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String ligne;
            while ((ligne = r.readLine()) != null) {
                if (ligne.isBlank() || ligne.startsWith("#")) continue;
                String[] f = ligne.split(" ; ", -1);
                boolean exception = f[4].trim().equals("EXCEPTION");
                List<Double> racines = nombres(exception ? "" : f[4]);
                List<Double> tol = nombres(f[5]);
                liste.add(new RA(f[0].trim(), Double.parseDouble(f[1]), Double.parseDouble(f[2]), Double.parseDouble(f[3]),
                        exception, racines, tol, f[6].contains("racine double"), f[6].trim()));
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return List.copyOf(liste);
    }

    private static List<Double> nombres(String s) {
        List<Double> l = new ArrayList<>();
        String t = s.replace("[", "").replace("]", "").trim();
        if (t.isEmpty()) return l;
        for (String x : t.split(",")) l.add(Double.parseDouble(x.trim()));
        return l;
    }
}
