package vv.modele;

/** Familles d'oracles (taxonomie de Barr et al., 2015, reprise dans le cours). */
public enum TypeOracle {
    REFERENCE("par référence : N(y, RA) <= Tol"),
    PROPRIETE("par propriété (spécifié) : résidu, Viète"),
    HOMOTHETIQUE("par homothétie : g(k·a, k·b, k·c) = g(a, b, c)"),
    METAMORPHIQUE("dérivé métamorphique : relation entre deux exécutions"),
    DIFFERENTIEL("dérivé différentiel : comparaison à une implémentation en BigDecimal"),
    CONSTRUCTION("par construction : on choisit les racines puis on fabrique l'équation"),
    IMPLICITE("implicite : pas de plantage, contrat de forme respecté");

    public final String description;

    TypeOracle(String description) {
        this.description = description;
    }
}
