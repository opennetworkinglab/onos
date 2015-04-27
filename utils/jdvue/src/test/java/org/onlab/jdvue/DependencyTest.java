package org.onlab.jdvue;

import com.google.common.testing.EqualsTester;
import org.junit.Test;
import org.onlab.jdvue.Dependency;
import org.onlab.jdvue.JavaPackage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * Unit test for the dependency entity.
 *
 * @author Thomas Vachuska
 */
public class DependencyTest {

    @Test
    public void basics() {
        JavaPackage x = new JavaPackage("x");
        JavaPackage y = new JavaPackage("y");

        new EqualsTester()
                .addEqualityGroup(new Dependency(x, y), new Dependency(x, y))
                .addEqualityGroup(new Dependency(y, x), new Dependency(y, x))
                .testEquals();
    }

}
