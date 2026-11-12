package vv.pairwise;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Générateur de tableau de couverture de force 2 (pairwise) par l'algorithme IPOG
 * (Lei et al., 2007), celui des outils ACTS (NIST) et PICT (Microsoft).
 *
 * Entrée : le nombre de valeurs de chaque paramètre, ici {9, 9, 9}.
 * Sortie : des lignes d'indices ; pour tout couple de paramètres (i, j) et tout couple de valeurs (vi, vj),
 *          au moins une ligne contient vi en position i et vj en position j.
 *
 * Pour 3 paramètres de même taille v, on utilise directement le carré latin (v² lignes, optimal) ;
 * IPOG sert dans le cas général (4 paramètres ou plus, tailles différentes).
 *
 * Principe d'IPOG :
 *   1. On écrit toutes les combinaisons des deux premiers paramètres (9 × 9 = 81 lignes).
 *   2. Pour chaque paramètre suivant :
 *      - croissance horizontale : on lui donne, ligne par ligne, la valeur qui couvre le plus de paires
 *        encore non couvertes ;
 *      - croissance verticale : pour chaque paire restante, on complète une ligne qui a une case libre,
 *        ou on ajoute une ligne.
 */
public final class GenerateurPairwise {

    private static final int LIBRE = -1;

    private GenerateurPairwise() {
    }

    public static List<int[]> paires(int[] tailles) {
        int k = tailles.length;
        List<int[]> lignes = new ArrayList<>();
        if (k == 0) return lignes;
        if (k == 3 && tailles[0] == tailles[1] && tailles[1] == tailles[2]) {
            return carreLatin(tailles[0]);
        }
        return ipogGeneral(tailles);
    }

    private static List<int[]> ipogGeneral(int[] tailles) {
        int k = tailles.length;
        List<int[]> lignes = new ArrayList<>();
        if (k == 1) {
            for (int v = 0; v < tailles[0]; v++) lignes.add(new int[]{v});
            return lignes;
        }
        for (int v0 = 0; v0 < tailles[0]; v0++) {
            for (int v1 = 0; v1 < tailles[1]; v1++) {
                int[] l = new int[k];
                Arrays.fill(l, LIBRE);
                l[0] = v0;
                l[1] = v1;
                lignes.add(l);
            }
        }
        for (int p = 2; p < k; p++) {
            Set<Long> nonCouvertes = new HashSet<>();
            for (int i = 0; i < p; i++)
                for (int vi = 0; vi < tailles[i]; vi++)
                    for (int vp = 0; vp < tailles[p]; vp++)
                        nonCouvertes.add(cle(i, vi, p, vp));

            // croissance horizontale
            for (int[] l : lignes) {
                int meilleure = 0, gainMax = -1;
                for (int vp = 0; vp < tailles[p]; vp++) {
                    int gain = 0;
                    for (int i = 0; i < p; i++)
                        if (l[i] != LIBRE && nonCouvertes.contains(cle(i, l[i], p, vp))) gain++;
                    if (gain > gainMax) {
                        gainMax = gain;
                        meilleure = vp;
                    }
                }
                l[p] = meilleure;
                for (int i = 0; i < p; i++)
                    if (l[i] != LIBRE) nonCouvertes.remove(cle(i, l[i], p, meilleure));
            }

            // croissance verticale
            for (int i = 0; i < p; i++) {
                for (int vi = 0; vi < tailles[i]; vi++) {
                    for (int vp = 0; vp < tailles[p]; vp++) {
                        if (!nonCouvertes.contains(cle(i, vi, p, vp))) continue;
                        int[] cible = null;
                        for (int[] l : lignes) {
                            if (l[p] == vp && l[i] == LIBRE) {
                                cible = l;
                                break;
                            }
                        }
                        if (cible == null) {
                            cible = new int[k];
                            Arrays.fill(cible, LIBRE);
                            cible[p] = vp;
                            lignes.add(cible);
                        }
                        cible[i] = vi;
                        nonCouvertes.remove(cle(i, vi, p, vp));
                    }
                }
            }
        }
        for (int[] l : lignes)
            for (int i = 0; i < k; i++)
                if (l[i] == LIBRE) l[i] = 0; // case sans contrainte : n'importe quelle valeur convient
        return lignes;
    }

    /**
     * Cas particulier optimal : 3 paramètres de v valeurs. Le carré latin cyclique (a, b, (a + b) mod v)
     * couvre toutes les paires en v² lignes, ce qui est le minimum (les v² paires de (a, b) exigent v² lignes) :
     * pour a fixé, quand b parcourt les v valeurs, c aussi ; de même pour b fixé.
     */
    static List<int[]> carreLatin(int v) {
        List<int[]> lignes = new ArrayList<>();
        for (int a = 0; a < v; a++)
            for (int b = 0; b < v; b++)
                lignes.add(new int[]{a, b, (a + b) % v});
        return lignes;
    }

    /** IPOG seul, sans le raccourci du carré latin (pour comparer). */
    public static List<int[]> ipog(int[] tailles) {
        if (tailles.length == 3) {
            // on désactive le raccourci en traitant le 3e paramètre par la croissance IPOG
            return ipogGeneral(tailles);
        }
        return paires(tailles);
    }

    /** Produit cartésien complet (test exhaustif), pour comparaison. */
    public static List<int[]> exhaustif(int[] tailles) {
        List<int[]> lignes = new ArrayList<>();
        int[] courant = new int[tailles.length];
        produit(tailles, 0, courant, lignes);
        return lignes;
    }

    private static void produit(int[] tailles, int pos, int[] courant, List<int[]> sortie) {
        if (pos == tailles.length) {
            sortie.add(courant.clone());
            return;
        }
        for (int v = 0; v < tailles[pos]; v++) {
            courant[pos] = v;
            produit(tailles, pos + 1, courant, sortie);
        }
    }

    /** Paires (i, vi, j, vj) non couvertes par les lignes ; vide si la couverture 2 à 2 est totale. */
    public static List<String> pairesManquantes(List<int[]> lignes, int[] tailles) {
        List<String> manquantes = new ArrayList<>();
        for (int i = 0; i < tailles.length; i++)
            for (int j = i + 1; j < tailles.length; j++)
                for (int vi = 0; vi < tailles[i]; vi++)
                    for (int vj = 0; vj < tailles[j]; vj++) {
                        boolean vue = false;
                        for (int[] l : lignes) {
                            if (l[i] == vi && l[j] == vj) {
                                vue = true;
                                break;
                            }
                        }
                        if (!vue) manquantes.add("(p" + i + "=" + vi + ", p" + j + "=" + vj + ")");
                    }
        return manquantes;
    }

    public static int nombreDePaires(int[] tailles) {
        int n = 0;
        for (int i = 0; i < tailles.length; i++)
            for (int j = i + 1; j < tailles.length; j++) n += tailles[i] * tailles[j];
        return n;
    }

    private static long cle(int i, int vi, int j, int vj) {
        return (((long) i * 1000 + vi) * 1000 + j) * 1000 + vj;
    }
}
