package org.onlab.jdvue;

import org.junit.Test;
import org.onlab.jdvue.DependencyCycle;
import org.onlab.jdvue.JavaPackage;

import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * Unit test for the dependency cycle entity.
 *
 * @author Thomas Vachuska
 */
public class DependencyCycleTest {

    @Test
    public void normalize() {
        JavaPackage x = new JavaPackage("x");
        JavaPackage y = new JavaPackage("y");
        JavaPackage z = new JavaPackage("z");

        DependencyCycle a = new DependencyCycle(Arrays.asList(new JavaPackage[] {x, y, z}), x);
        DependencyCycle b = new DependencyCycle(Arrays.asList(new JavaPackage[] {y, z, x}), y);
        DependencyCycle c = new DependencyCycle(Arrays.asList(new JavaPackage[] {z, x, y}), z);

        assertEquals("incorrect normalization", a, b);
        assertEquals("incorrect normalization", a, c);
    }

    @Test
    public void testToString() {
        JavaPackage x = new JavaPackage("x");
        JavaPackage y = new JavaPackage("y");
        JavaPackage z = new JavaPackage("z");

        DependencyCycle a = new DependencyCycle(Arrays.asList(new JavaPackage[] {x, y, z}), x);
        assertEquals("incorrect toString", "[x, y, z]", a.toShortString());
        assertEquals("incorrect toString",
                     "DependencyCycle{cycle=[" +
                             "JavaPackage{name=x, sources=0, dependencies=0}, " +
                             "JavaPackage{name=y, sources=0, dependencies=0}, " +
                             "JavaPackage{name=z, sources=0, dependencies=0}]}",
                     a.toString());
    }
}
