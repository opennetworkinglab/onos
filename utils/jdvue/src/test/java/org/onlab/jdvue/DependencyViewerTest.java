package org.onlab.jdvue;

import org.junit.Test;
import org.onlab.jdvue.DependencyViewer;

import java.io.FileInputStream;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.onlab.jdvue.DependencyViewer.slurp;

/**
 * Unit test for the dependency viewer.
 *
 * @author Thomas Vachuska
 */
public class DependencyViewerTest {

    @Test
    public void basics() throws IOException {
        DependencyViewer.main(new String[]{"src/test/resources/catalog"});

        String expected = slurp(new FileInputStream("src/test/resources/expected.html"));
        String actual = slurp(new FileInputStream("src/test/resources/catalog.html"));

        // FIXME: add more manageable assertions here
//        assertEquals("incorrect html", expected, actual);
    }

}
