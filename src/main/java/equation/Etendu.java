package equation;

/**
 * Nombre à exposant étendu : valeur = m · 2^e, avec 1 <= |m| < 2 (ou m = 0) et e entier sans limite.
 *
 * Un double ne peut pas représenter 1e400 ni 1e-400, mais m · 2^e le peut : on garde la mantisse
 * en double (53 bits de précision) et l'exposant dans un int. Cela permet de calculer b², 4ac,
 * √Δ et les quotients sans débordement ni sous-dépassement intermédiaire ; on ne revient
 * aux double qu'à la toute fin, avec un seul arrondi.
 */
final class Etendu {

    static final Etendu ZERO = new Etendu(0.0, 0);

    final double m;
    final int e;

    private Etendu(double m, int e) {
        this.m = m;
        this.e = e;
    }

    /** Construit la forme normalisée de m · 2^e. */
    static Etendu of(double m, int e) {
        if (m == 0.0) return ZERO;
        int k = exposant(m);
        return new Etendu(Math.scalb(m, -k), e + k);
    }

    static Etendu of(double x) {
        return of(x, 0);
    }

    /** Exposant binaire de x, y compris pour un nombre sous-normal. */
    private static int exposant(double x) {
        if (Math.abs(x) >= Double.MIN_NORMAL) {
            return Math.getExponent(x);
        }
        return Math.getExponent(x * 0x1p54) - 54;
    }

    boolean estZero() {
        return m == 0.0;
    }

    int signe() {
        return m > 0 ? 1 : (m < 0 ? -1 : 0);
    }

    Etendu oppose() {
        return new Etendu(-m, e);
    }

    /** Multiplication par 2^k (exacte). */
    Etendu foisPuissanceDeDeux(int k) {
        return estZero() ? this : new Etendu(m, e + k);
    }

    Etendu fois(Etendu o) {
        return of(m * o.m, e + o.e);
    }

    Etendu divise(Etendu o) {
        if (o.estZero()) throw new ArithmeticException("division par zéro");
        return of(m / o.m, e - o.e);
    }

    /** Addition : on aligne sur le plus grand exposant ; un terme 2^1100 fois plus petit est négligeable. */
    Etendu plus(Etendu o) {
        if (estZero()) return o;
        if (o.estZero()) return this;
        if (e >= o.e) {
            int d = e - o.e;
            return d > 1100 ? this : of(m + Math.scalb(o.m, -d), e);
        }
        return o.plus(this);
    }

    Etendu racineCarree() {
        if (m < 0) throw new ArithmeticException("racine d'un négatif");
        if (estZero()) return ZERO;
        // e doit être pair pour diviser l'exposant par deux
        double mm = m;
        int ee = e;
        if ((ee & 1) != 0) {
            mm *= 2;
            ee -= 1;
        }
        return of(Math.sqrt(mm), ee / 2);
    }

    /** Retour aux double : un seul arrondi, ±Infinity si trop grand, 0 si trop petit. */
    double enDouble() {
        if (estZero()) return 0.0;
        if (e > 2000) return Math.copySign(Double.POSITIVE_INFINITY, m);
        if (e < -2000) return Math.copySign(0.0, m);
        return Math.scalb(m, e);
    }
}
