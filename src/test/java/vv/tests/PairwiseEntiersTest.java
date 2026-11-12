package vv.tests;

import org.junit.jupiter.api.DisplayName;

import vv.classes.ClassesEntiers;
import vv.pairwise.Domaine;

/**
 * Les oracles de {@link PairwiseTest} (implicite, différentiel, propriétés dont Viète, MR1, MR2, MR3),
 * rejoués sur le domaine des ENTIERS MACHINE : 81 cas pairwise des 9 classes de {@link ClassesEntiers}.
 */
@Domaine(ClassesEntiers.class)
@DisplayName("Pairwise entiers machine : 9 classes × (a, b, c)")
class PairwiseEntiersTest extends PairwiseTest {
}
