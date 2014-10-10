package org.onlab.onos.net;

import static org.junit.Assert.assertEquals;
import static org.onlab.onos.net.PortNumber.portNumber;

import org.junit.Test;

import com.google.common.testing.EqualsTester;

/**
 * Test of the port number.
 */
public class PortNumberTest {

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


}
