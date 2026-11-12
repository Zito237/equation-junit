package vv.reflexion;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Accès au système sous test (g) PAR RÉFLEXIVITÉ.
 *
 * Le code de test ne connaît pas la classe qui résout l'équation : il ne l'importe pas
 * et ne la nomme jamais. Le nom de la classe est lu à l'exécution :
 *
 *   1. propriété système  -Dsolveur.classe=equation.SolveurNaif   (prioritaire)
 *   2. sinon le fichier   src/test/resources/solveur.properties
 *
 * Puis, dynamiquement :
 *   - Class.forName(nom)                       charge la classe ;
 *   - recherche de la méthode : nom donné par solveur.methode (par défaut « racines »), ou à défaut
 *     n'importe quelle méthode publique (double, double, double) qui renvoie une List ;
 *   - getDeclaredConstructor().newInstance()   instancie la classe (sauf méthode static) ;
 *   - Method.invoke(...)                       appelle la méthode.
 *
 * On peut donc tester n'importe quelle implémentation, y compris une écrite après les tests,
 * sans recompiler ni modifier une seule ligne de test.
 */
public final class SolveurReflexif {

    public static final String PROPRIETE_CLASSE = "solveur.classe";
    public static final String PROPRIETE_METHODE = "solveur.methode";

    private static final Map<String, SolveurReflexif> CACHE = new ConcurrentHashMap<>();

    private final Class<?> classe;
    private final Method methode;
    private final Method methodeTexte; // même nom, (String, String, String) ; null si le solveur n'en a pas
    private final Object instance;

    private SolveurReflexif(String nomClasse, String nomMethode) {
        try {
            this.classe = Class.forName(nomClasse);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Classe de solveur introuvable : " + nomClasse
                    + " (vérifiez -D" + PROPRIETE_CLASSE + " ou solveur.properties)", e);
        }
        this.methode = trouverMethode(classe, nomMethode);
        Method texte;
        try {
            texte = classe.getMethod(methode.getName(), String.class, String.class, String.class);
        } catch (NoSuchMethodException e) {
            texte = null;
        }
        this.methodeTexte = texte;
        if (Modifier.isStatic(methode.getModifiers())) {
            this.instance = null;
        } else {
            try {
                this.instance = classe.getDeclaredConstructor().newInstance(); // instanciation dynamique
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Impossible d'instancier " + nomClasse
                        + " : un constructeur public sans argument est nécessaire", e);
            }
        }
    }

    /** Le solveur configuré pour cette exécution (relu à chaque appel : on peut en changer en cours de route). */
    public static SolveurReflexif courant() {
        String nomClasse = System.getProperty(PROPRIETE_CLASSE);
        String nomMethode = System.getProperty(PROPRIETE_METHODE);
        if (nomClasse == null || nomClasse.isBlank() || nomMethode == null) {
            Properties p = lireFichier();
            if (nomClasse == null || nomClasse.isBlank()) nomClasse = p.getProperty(PROPRIETE_CLASSE);
            if (nomMethode == null) nomMethode = p.getProperty(PROPRIETE_METHODE, "racines");
        }
        if (nomClasse == null || nomClasse.isBlank()) {
            throw new IllegalStateException("Aucun solveur configuré : -D" + PROPRIETE_CLASSE + "=...");
        }
        String nm = nomMethode;
        return CACHE.computeIfAbsent(nomClasse + "#" + nm, k -> new SolveurReflexif(k.substring(0, k.indexOf('#')), nm));
    }

    private static Properties lireFichier() {
        Properties p = new Properties();
        try (InputStream in = SolveurReflexif.class.getResourceAsStream("/solveur.properties")) {
            if (in != null) p.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("Lecture de solveur.properties impossible", e);
        }
        return p;
    }

    private static Method trouverMethode(Class<?> k, String nom) {
        Method parSignature = null;
        for (Method m : k.getMethods()) {
            Class<?>[] t = m.getParameterTypes();
            boolean signature = t.length == 3 && t[0] == double.class && t[1] == double.class && t[2] == double.class
                    && List.class.isAssignableFrom(m.getReturnType());
            if (!signature) continue;
            if (m.getName().equals(nom)) return m;
            if (parSignature == null) parSignature = m;
        }
        if (parSignature != null) return parSignature;
        throw new IllegalStateException(k.getName() + " n'a aucune méthode publique (double, double, double) -> List");
    }

    /**
     * y = g(a, b, c). Une exception levée par le solveur est relancée telle quelle
     * (et non enveloppée dans InvocationTargetException) pour que les tests puissent l'attendre.
     *
     * @throws AssertionError si le résultat ne respecte pas le contrat : une ArrayList de Double
     */
    public ArrayList<Double> racines(double a, double b, double c) {
        return appeler(methode, a, b, c);
    }

    /**
     * y = g(a, b, c) avec des coefficients en TEXTE (caractère, chaîne) : méthode de même nom et de
     * signature (String, String, String). Un texte qui n'est pas un nombre doit lever NumberFormatException.
     *
     * @throws AssertionError si le solveur n'accepte pas de coefficients en texte
     */
    public ArrayList<Double> racines(String a, String b, String c) {
        if (methodeTexte == null) {
            throw new AssertionError("Contrat : " + classe.getName() + " doit avoir une méthode publique "
                    + methode.getName() + "(String, String, String) qui refuse un texte qui n'est pas un nombre");
        }
        return appeler(methodeTexte, a, b, c);
    }

    private ArrayList<Double> appeler(Method m, Object a, Object b, Object c) {
        Object resultat;
        try {
            resultat = m.invoke(instance, a, b, c);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException re) throw re;
            if (cause instanceof Error err) throw err;
            throw new IllegalStateException(cause);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
        if (!(resultat instanceof ArrayList<?> liste)) {
            throw new AssertionError("Contrat : le solveur doit renvoyer une ArrayList, reçu "
                    + (resultat == null ? "null" : resultat.getClass().getName()));
        }
        ArrayList<Double> r = new ArrayList<>(liste.size());
        for (Object o : liste) {
            if (!(o instanceof Double d)) {
                throw new AssertionError("Contrat : la liste doit contenir des Double, reçu " + o);
            }
            r.add(d);
        }
        return r;
    }

    public String nomClasse() {
        return classe.getName();
    }

    public String nomCourt() {
        return classe.getSimpleName();
    }
}
