package org.onlab.onos.net.trivial.impl;

import org.junit.Test;
import org.onlab.onos.net.GreetService;

import java.util.Iterator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * Example of a component &amp; service implementation unit test.
 */
public class GreetManagerTest {

    @Test
    public void basics() {
        GreetService service = new GreetManager();
        assertEquals("incorrect greeting", "Whazup dude?", service.yo("dude"));

        Iterator<String> names = service.names().iterator();
        assertEquals("incorrect name", "dude", names.next());
        assertFalse("no more names expected", names.hasNext());
    }

    @Test(expected = NullPointerException.class)
    public void nullArg() {
        new GreetManager().yo(null);
    }

}
