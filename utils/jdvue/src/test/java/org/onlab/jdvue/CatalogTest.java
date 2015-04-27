package org.onlab.jdvue;

import org.junit.Test;
import org.onlab.jdvue.Catalog;
import org.onlab.jdvue.JavaPackage;
import org.onlab.jdvue.JavaSource;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Unit test for the source catalog.
 *
 * @author Thomas Vachuska
 */
public class CatalogTest {

    @Test
    public void basics() throws IOException {
        Catalog cat = new Catalog();
        cat.load("src/test/resources/catalog.db");
        cat.analyze();

        assertEquals("incorrect package count", 12, cat.getPackages().size());
        assertEquals("incorrect source count", 14, cat.getSources().size());

        JavaPackage pkg = cat.getPackage("k");
        assertNotNull("package should be found", pkg);

        JavaSource src = cat.getSource("k.K");
        assertNotNull("source should be found", src);

        assertEquals("incorrect package source count", 1, pkg.getSources().size());
        assertEquals("incorrect package dependency count", 1, pkg.getDependencies().size());
        assertEquals("incorrect package cycle count", 3, cat.getPackageCycles(pkg).size());

        assertEquals("incorrect segment count", 11, cat.getCycleSegments().size());
        assertEquals("incorrect cycle count", 5, cat.getCycles().size());
    }

}
