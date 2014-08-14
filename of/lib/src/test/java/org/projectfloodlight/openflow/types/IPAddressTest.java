package org.projectfloodlight.openflow.types;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.net.UnknownHostException;

import org.junit.Test;

/**
 * Most tests are in IPv4AddressTest and IPv6AddressTest
 * Just exception testing here
 * @author gregor
 *
 */
public class IPAddressTest {
    @Test
    public void testOfException() {
        try {
            IPAddress.of("Foobar");
            fail("Should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected
        }
        try {
            IPAddressWithMask.of("Foobar");
            fail("Should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected
        }
        try {
            IPAddress.of(null);
            fail("Should have thrown NullPointerException");
        } catch (NullPointerException e) {
            assertNotNull(e.getMessage());
        }
        try {
            IPAddressWithMask.of(null);
            fail("Should have thrown NullPointerException");
        } catch (NullPointerException e) {
            assertNotNull(e.getMessage());
        }
        try {
            IPAddress.of(null);
            fail("Should have thrown NullPointerException");
        } catch (NullPointerException e) {
            assertNotNull(e.getMessage());
        }
        try {
            IPAddressWithMask.of(null);
            fail("Should have thrown NullPointerException");
        } catch (NullPointerException e) {
            assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testFromInetAddressException() throws UnknownHostException {
        try {
            IPAddress.fromInetAddress(null);
            fail("Should have thrown NullPointerException");
        } catch (NullPointerException e) {
            assertNotNull(e.getMessage());
        }
    }

}
