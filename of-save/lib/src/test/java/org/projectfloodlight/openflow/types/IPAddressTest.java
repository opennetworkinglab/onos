package org.projectfloodlight.openflow.types;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.InetAddress;
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
            IPAddress.of((String) null);
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
            IPAddress.of((String) null);
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
    public void testOfString() {
        IPAddress<?> ip0 = IPAddress.of("1.2.3.4");
        IPAddress<?> ip1 = IPAddress.of("abcd::1234");
        assertTrue(ip0 instanceof IPv4Address);
        assertTrue(ip1 instanceof IPv6Address);
        assertEquals(ip0, IPv4Address.of("1.2.3.4"));
        assertEquals(ip1, IPv6Address.of("abcd::1234"));
    }

    @Test
    public void testOfInetAddress() throws Exception {
        InetAddress ia0 = InetAddress.getByName("192.168.1.123");
        InetAddress ia1 = InetAddress.getByName("fd00::4321");
        IPAddress<?> ip0 = IPAddress.of(ia0);
        IPAddress<?> ip1 = IPAddress.of(ia1);
        assertTrue(ip0 instanceof IPv4Address);
        assertTrue(ip1 instanceof IPv6Address);
        assertEquals(ip0, IPv4Address.of(ia0));
        assertEquals(ip1, IPv6Address.of(ia1));
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testFromInetAddressException() throws UnknownHostException {
        try {
            IPAddress.fromInetAddress(null);
            fail("Should have thrown NullPointerException");
        } catch (NullPointerException e) {
            assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testContains() {

        // Test IPv4 Mask
        IPAddressWithMask<?> mask = IPAddressWithMask.of("1.2.3.4/24");

        IPAddress<?> validIp = IPAddress.of("1.2.3.5");
        assertTrue(mask.contains(validIp));

        IPAddress<?> invalidIp = IPAddress.of("1.2.5.5");
        assertFalse(mask.contains(invalidIp));

        IPAddress<?> invalidIpv6 = IPAddress.of("10:10::ffff");
        assertFalse(mask.contains(invalidIpv6));

        // Test IPv6 Mask
        mask = IPAddressWithMask.of("10:10::1/112");

        validIp = IPAddress.of("10:10::f");
        assertTrue(mask.contains(validIp));

        invalidIp = IPAddress.of("11:10::f");
        assertFalse(mask.contains(invalidIp));

        IPAddress<?> invalidIpv4 = IPAddress.of("10.0.0.1");
        assertFalse(mask.contains(invalidIpv4));
    }

    @Test 
    public void testContainsException() {
        try {
            IPAddressWithMask<?> mask = IPAddressWithMask.of("1.2.3.4/24");
            mask.contains(null);
            fail("Should have thrown NullPointerException");
        } catch (NullPointerException e) {
            assertNotNull(e.getMessage());
        }
    }

}
