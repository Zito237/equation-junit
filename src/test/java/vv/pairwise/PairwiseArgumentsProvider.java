package vv.pairwise;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.support.AnnotationConsumer;

import vv.classes.ClassesEquivalence;
import vv.classes.ClassesEquivalence.Classe;

/** Implémentation du décorateur {@link PairwiseSource}. */
public class PairwiseArgumentsProvider implements ArgumentsProvider, AnnotationConsumer<PairwiseSource> {

    private PairwiseSource config;

    @Override
    public void accept(PairwiseSource annotation) {
        this.config = annotation;
    }

    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
        Domaine domaine = context.getRequiredTestClass().getAnnotation(Domaine.class);
        List<Classe> classes = ClassesEquivalence.decouvrir(domaine != null ? domaine.value() : config.classes());
        int n = config.parametres();
        int[] tailles = new int[n];
        Arrays.fill(tailles, classes.size());

        boolean exhaustif = config.force() >= n;
        List<int[]> lignes = exhaustif ? GenerateurPairwise.exhaustif(tailles) : GenerateurPairwise.paires(tailles);

        List<String> manquantes = GenerateurPairwise.pairesManquantes(lignes, tailles);
        if (!manquantes.isEmpty()) {
            throw new IllegalStateException("Couverture pairwise incomplète : " + manquantes);
        }
        int total = (int) Math.pow(classes.size(), n);
        context.publishReportEntry("pairwise", lignes.size() + " cas (exhaustif : " + total + "), "
                + GenerateurPairwise.nombreDePaires(tailles) + " paires couvertes");

        return lignes.stream().map(l -> {
            Object[] args = new Object[n];
            for (int i = 0; i < n; i++) {
                Classe cl = classes.get(l[i]);
                args[i] = Named.of(cl.etiquette(), cl.valeur()); // chaque argument porte le nom de sa classe
            }
            return Arguments.of(args);
        });
    }
}
