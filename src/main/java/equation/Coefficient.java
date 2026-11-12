package equation;

/**
 * Conversion d'un coefficient donné sous forme de texte (caractère, chaîne saisie, contenu d'un fichier).
 *
 * Un texte qui n'est pas un nombre fini (« x », « abc », « », « 2,5x », « NaN », « Infinity ») lève
 * NumberFormatException avec le message « a = « abc » : ce n'est pas un nombre, veuillez entrer un nombre ».
 * La virgule décimale est acceptée : « 2,5 » = 2.5.
 *
 * Pourquoi une entrée texte : en Java, un char passé à racines(double, double, double) est converti
 * sans erreur en son code ('a' devient 97.0). Seule une entrée texte permet de refuser un caractère.
 */
public final class Coefficient {

    private Coefficient() {
    }

    /** @param nom « a », « b » ou « c », repris dans le message d'erreur */
    public static double lire(String nom, String texte) {
        NumberFormatException pasUnNombre = new NumberFormatException(
                nom + " = « " + texte + " » : ce n'est pas un nombre, veuillez entrer un nombre");
        if (texte == null) throw pasUnNombre;
        double x;
        try {
            x = Double.parseDouble(texte.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            throw pasUnNombre;
        }
        if (!Double.isFinite(x)) throw pasUnNombre; // parseDouble accepte « NaN » et « Infinity »
        return x;
    }
}
