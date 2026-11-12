package vv.tests;

import org.junit.jupiter.api.DisplayName;

import vv.classes.ClassesEntiers;
import vv.pairwise.Domaine;

/** L'oracle par référence de {@link ReferenceTest}, sur les 81 cas pairwise du domaine des entiers machine. */
@Domaine(ClassesEntiers.class)
@DisplayName("Oracle par référence, entiers machine : RA et Tol des 81 cas pairwise")
class ReferenceEntiersTest extends ReferenceTest {
}
