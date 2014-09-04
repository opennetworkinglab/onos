package org.onlab.onos.net;

import com.google.common.testing.EqualsTester;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.onlab.onos.net.PortNumber.portNumber;

/**
 * Test of the port number.
 */
public class PortNumberTest extends ElementIdTest {

    @Test
    public void basics() {
        new EqualsTester()
                .addEqualityGroup(portNumber(123),
                                  portNumber("123"))
                .addEqualityGroup(portNumber(321))
                .testEquals();
    }

    @Test
    public void number() {
        assertEquals("incorrect long value", 12345, portNumber(12345).toLong());
    }

    @Test(expected = IllegalArgumentException.class)
    public void negative() {
        portNumber(-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void tooBig() {
        portNumber((2L * Integer.MAX_VALUE) + 2);
    }
}
