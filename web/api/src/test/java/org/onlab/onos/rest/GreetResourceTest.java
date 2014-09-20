package org.onlab.onos.rest;

import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.test.framework.JerseyTest;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Simple example on how to write a JAX-RS unit test using Jersey test framework.
 * A base class should/will be created to provide further assistance for testing.
 */
public class GreetResourceTest extends JerseyTest {

    public GreetResourceTest() {
        super("org.onlab.onos.rest");
    }

    @BeforeClass
    public static void classSetUp() {
//        ServiceDirectory testDirectory =
//                new TestServiceDirectory().add(GreetService.class, new GreetManager());
//        GreetResource.setServiceDirectory(testDirectory);
    }

    @Ignore
    @Test
    public void basics() {
        WebResource rs = resource();
        String response = rs.path("greet").get(String.class);
        assertTrue("incorrect response", response.contains("Whazup "));
    }

}
