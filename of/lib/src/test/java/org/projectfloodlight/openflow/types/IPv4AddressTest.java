package org.projectfloodlight.openflow.types;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.hamcrest.CoreMatchers;
import org.jboss.netty.buffer.ChannelBuffers;
import org.junit.Test;
import org.projectfloodlight.openflow.exceptions.OFParseError;

public class IPv4AddressTest {
    byte[][] testAddresses = new byte[][] {
            {0x01, 0x02, 0x03, 0x04 },
            {127, 0, 0, 1},
            {(byte) 192, (byte) 168, 0, 100 },
            {(byte) 255, (byte) 255, (byte) 255, (byte) 255 }
    };

    String[] testStrings = {
            "1.2.3.4",
            "127.0.0.1",
            "192.168.0.100",
            "255.255.255.255"
    };

    int[] testInts = {
            0x01020304,
            0x7f000001,
            (192 << 24) | (168 << 16) | 100,
            0xffffffff
    };

    String[] invalidIPs = {
            "",
            ".",
            "1.2..3.4",
            "1.2.3.4.",
            "257.11.225.1",
            "256.11.225.1",
            "-1.2.3.4",
            "1.2.3.4.5",
            "1.x.3.4",
            "1.2x.3.4"
    };

    String[] ipsWithMask = {
                            "1.2.3.4/24",
                            "192.168.130.140/255.255.192.0",
                            "127.0.0.1/8",
                            "8.8.8.8",
                            "8.8.8.8/32",
                            "0.0.0.0/0",
                            "192.168.130.140/255.0.255.0",
                            "1.2.3.4/0.127.0.255"
    };

    boolean[] hasMask = {
                         true,
                         true,
                         true,
                         false,
                         false,
                         true,
                         true,
                         true
    };

    byte[][][] ipsWithMaskValues = {
                             new byte[][] { new byte[] { (byte)0x01, (byte)0x02, (byte)0x03, (byte)0x04 }, new byte[] { (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0x00 } },
                             new byte[][] { new byte[] { (byte)0xC0, (byte)0xA8, (byte)0x82, (byte)0x8C }, new byte[] { (byte)0xFF, (byte)0xFF, (byte)0xC0, (byte)0x00 } },
                             new byte[][] { new byte[] { (byte)0x7F, (byte)0x00, (byte)0x00, (byte)0x01 }, new byte[] { (byte)0xFF, (byte)0x00, (byte)0x00, (byte)0x00 } },
                             new byte[][] { new byte[] { (byte)0x08, (byte)0x08, (byte)0x08, (byte)0x08 }, new byte[] { (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF } },
                             new byte[][] { new byte[] { (byte)0x08, (byte)0x08, (byte)0x08, (byte)0x08 }, new byte[] { (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF } },
                             new byte[][] { new byte[] { (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00 }, new byte[] { (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00 } },
                             new byte[][] { new byte[] { (byte)0xC0, (byte)0xA8, (byte)0x82, (byte)0x8C }, new byte[] { (byte)0xFF, (byte)0x00, (byte)0xFF, (byte)0x00 } },
                             new byte[][] { new byte[] { (byte)0x01, (byte)0x02, (byte)0x03, (byte)0x04 }, new byte[] { (byte)0x00, (byte)0x7F, (byte)0x00, (byte)0xFF } }
    };

    int[] ipsWithMaskLengths = {
                                24,
                                18,
                                8,
                                32,
                                32,
                                0,
                                -1,
                                -1
    };

    String[] invalidIpsWithMask = {
                                   "asdf",
                                   "1.2.3.4/33",
                                   "1.2.3.4/34",
                                   "1.2.3.4/-1",
                                   "1.2.3.4/256.0.0.0",
                                   "1.256.3.4/255.255.0.0",
                                   "1.2.3.4/255.255.0.0.0",
    };

    @Test
    public void testLogicalOperatorsBroadcast() {
        assertTrue(IPv4Address.NO_MASK.not().equals(IPv4Address.FULL_MASK));
        assertTrue(IPv4Address.NO_MASK.or(IPv4Address.FULL_MASK).
                   equals(IPv4Address.NO_MASK));
        assertTrue(IPv4Address.NO_MASK.and(IPv4Address.FULL_MASK).
                   equals(IPv4Address.FULL_MASK));

        assertTrue(IPv4Address.NO_MASK.isBroadcast());
        assertTrue(!IPv4Address.FULL_MASK.isBroadcast());
    }

    @Test
    public void testMaskedSubnetBroadcast() {
        assertTrue(IPv4AddressWithMask.of("10.10.10.1/24")
                   .getSubnetBroadcastAddress()
                   .equals(IPv4Address.of("10.10.10.255")));
        assertTrue(IPv4AddressWithMask.of("10.10.10.1/24")
                   .isSubnetBroadcastAddress(IPv4Address.of("10.10.10.255")));
        assertTrue(!IPv4AddressWithMask.of("10.10.10.1/24")
                   .isSubnetBroadcastAddress(IPv4Address.of("10.10.10.254")));
    }

    @Test
    public void testMaskedMatchesCidr() {
        IPv4AddressWithMask slash28 = IPv4AddressWithMask.of("10.0.42.16/28");

        String[] notContained = {"0.0.0.0", "11.0.42.16", "10.0.41.1", "10.0.42.0", "10.0.42.15",
                                 "10.0.42.32", "255.255.255.255" };

        for(String n: notContained) {
            assertThat(String.format("slash 28 %s should not contain address %s",
                                     slash28, n),
                    slash28.matches(IPv4Address.of(n)), equalTo(false));
        }
        for(int i=16; i < 32; i++) {
            IPv4Address c = IPv4Address.of(String.format("10.0.42.%d", i));
            assertThat(String.format("slash 28 %s should contain address %s",
                                     slash28, c),
                       slash28.matches(c), equalTo(true));
        }
    }

    @Test
    public void testMaskedMatchesArbitrary() {
        // irregular octect on the 3rd bitmask requires '1'bit to be set
        // 4 bit unset, all others arbitrary
        IPv4AddressWithMask slash28 = IPv4AddressWithMask.of("1.2.1.4/255.255.5.255");

        String[] notContained = {"0.0.0.0", "1.2.3.5", "1.2.3.3",
                                 "1.2.0.4", "1.2.2.4", "1.2.4.4", "1.2.5.4", "1.2.6.4", "1.2.7.4",
                                 "1.2.8.4", "1.2.12.4", "1.2.13.4"
                                 };
        String[] contained = {"1.2.1.4", "1.2.3.4", "1.2.9.4", "1.2.11.4", "1.2.251.4",
                };

        for(String n: notContained) {
            assertThat(String.format("slash 28 %s should not contain address %s",
                                     slash28, n),
                    slash28.matches(IPv4Address.of(n)), equalTo(false));
        }
        for(String c: contained) {
            IPv4Address addr = IPv4Address.of(c);
            assertThat(String.format("slash 28 %s should contain address %s",
                                     slash28, addr),
                       slash28.matches(addr), equalTo(true));
        }

    }


    @Test
    public void testConstants() {
        byte[] zeros = { (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00 };
        byte[] ones =  { (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF };
        // Make sure class initializtation and static assignment don't get
        // messed up. Test everything twice for cached values
        assertTrue(IPv4Address.NONE.isCidrMask());
        assertEquals(0, IPv4Address.NONE.asCidrMaskLength());
        assertArrayEquals(zeros, IPv4Address.NONE.getBytes());
        assertTrue(IPv4Address.NONE.isCidrMask());
        assertEquals(0, IPv4Address.NONE.asCidrMaskLength());
        assertArrayEquals(zeros, IPv4Address.NONE.getBytes());

        assertTrue(IPv4Address.NO_MASK.isCidrMask());
        assertEquals(32, IPv4Address.NO_MASK.asCidrMaskLength());
        assertArrayEquals(ones, IPv4Address.NO_MASK.getBytes());
        assertTrue(IPv4Address.NO_MASK.isCidrMask());
        assertEquals(32, IPv4Address.NO_MASK.asCidrMaskLength());
        assertArrayEquals(ones, IPv4Address.NO_MASK.getBytes());

        assertTrue(IPv4Address.FULL_MASK.isCidrMask());
        assertEquals(0, IPv4Address.FULL_MASK.asCidrMaskLength());
        assertArrayEquals(zeros, IPv4Address.FULL_MASK.getBytes());
        assertTrue(IPv4Address.FULL_MASK.isCidrMask());
        assertEquals(0, IPv4Address.FULL_MASK.asCidrMaskLength());
        assertArrayEquals(zeros, IPv4Address.FULL_MASK.getBytes());
    }


    @Test
    public void testOfString() {
        for(int i=0; i < testAddresses.length; i++ ) {
            IPv4Address ip = IPv4Address.of(testStrings[i]);
            assertEquals(testInts[i], ip.getInt());
            assertArrayEquals(testAddresses[i], ip.getBytes());
            assertEquals(testStrings[i], ip.toString());
        }
    }

    @Test
    public void testOfByteArray() {
        for(int i=0; i < testAddresses.length; i++ ) {
            IPv4Address ip = IPv4Address.of(testAddresses[i]);
            assertEquals(testInts[i], ip.getInt());
            assertArrayEquals(testAddresses[i], ip.getBytes());
            assertEquals(testStrings[i], ip.toString());
        }
    }

    @Test
    public void testReadFrom() throws OFParseError {
        for(int i=0; i < testAddresses.length; i++ ) {
            IPv4Address ip = IPv4Address.read4Bytes(ChannelBuffers.copiedBuffer(testAddresses[i]));
            assertEquals(testInts[i], ip.getInt());
            assertArrayEquals(testAddresses[i], ip.getBytes());
            assertEquals(testStrings[i], ip.toString());
        }
    }


    @Test
    public void testInvalidIPs() throws OFParseError {
        for(String invalid : invalidIPs) {
            try {
                IPv4Address.of(invalid);
                fail("Invalid IP "+invalid+ " should have raised IllegalArgumentException");
            } catch(IllegalArgumentException e) {
                // ok
            }
        }
    }

    @Test
    public void testOfMasked() throws OFParseError {
        for (int i = 0; i < ipsWithMask.length; i++) {
            IPv4AddressWithMask value = IPv4AddressWithMask.of(ipsWithMask[i]);
            if (!hasMask[i]) {
                IPv4Address ip = value.getValue();
                assertArrayEquals(ipsWithMaskValues[i][0], ip.getBytes());
            }
            IPv4Address mask = value.getMask();
            if (ipsWithMaskLengths[i] == -1) {
                assertFalse(mask.isCidrMask());
                try {
                    mask.asCidrMaskLength();
                    fail("Expected IllegalStateException not thrown");
                } catch(IllegalStateException e) {
                    //expected
                }
            } else {
                assertTrue(mask.isCidrMask());
                assertEquals(ipsWithMaskLengths[i], mask.asCidrMaskLength());
            }
            assertArrayEquals(ipsWithMaskValues[i][1], mask.getBytes());
            byte[] ipBytes = new byte[4];
            System.arraycopy(ipsWithMaskValues[i][0], 0, ipBytes, 0, 4);
            assertEquals(ipBytes.length, value.getValue().getBytes().length);
            for (int j = 0; j < ipBytes.length; j++) {
                ipBytes[j] &= ipsWithMaskValues[i][1][j];
            }

            assertArrayEquals(ipBytes, value.getValue().getBytes());
            assertThat(String.format("Byte comparison for mask of %s (%s)", ipsWithMask[i], value),
                    value.getMask().getBytes(), CoreMatchers.equalTo(ipsWithMaskValues[i][1]));
        }
    }

    @Test
    public void testOfMaskedInvalid() throws Exception {
        for(String invalid : invalidIpsWithMask) {
            try {
                IPv4Address.of(invalid);
                fail("Invalid IP "+invalid+ " should have raised IllegalArgumentException");
            } catch(IllegalArgumentException e) {
                // ok
            }
        }
    }

    @Test
    public void testSuperclass() throws Exception {
        for(String ipString: testStrings) {
            IPAddress<?> superIp = IPAddress.of(ipString);
            assertEquals(IPVersion.IPv4, superIp.getIpVersion());
            assertEquals(IPv4Address.of(ipString), superIp);
        }

        for(String ipMaskedString: ipsWithMask) {
            IPAddressWithMask<?> superIp = IPAddressWithMask.of(ipMaskedString);
            assertEquals(IPVersion.IPv4, superIp.getIpVersion());
            assertEquals(IPv4AddressWithMask.of(ipMaskedString), superIp);
        }
    }

    @Test
    public void testOfExceptions() {
        // We check if the message of a caught NPE is set to a useful message
        // as a hacky way of verifying that we got an NPE thrown by use rather
        // than one the JVM created for a null access.
        try {
            String s = null;
            IPv4Address.of(s);
            fail("Should have thrown NullPointerException");
        } catch (NullPointerException e) {
            assertNotNull(e.getMessage());
        }
        try {
            byte[] b = null;
            IPv4Address.of(b);
            fail("Should have thrown NullPointerException");
        } catch (NullPointerException e) {
            assertNotNull(e.getMessage());
        }
        try {
            byte[] b = new byte[3];
            IPv4Address.of(b);
            fail("Should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected
        }
        try {
            byte[] b = new byte[5];
            IPv4Address.of(b);
            fail("Should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected
        }
        try {
            IPv4AddressWithMask.of(null);
            fail("Should have thrown NullPointerException");
        } catch (NullPointerException e) {
            assertNotNull(e.getMessage());
        }
        try {
            IPv4AddressWithMask.of(IPv4Address.of("1.2.3.4"), null);
            fail("Should have thrown NullPointerException");
        } catch (NullPointerException e) {
            assertNotNull(e.getMessage());
        }
        try {
            IPv4AddressWithMask.of(null, IPv4Address.of("255.0.0.0"));
            fail("Should have thrown NullPointerException");
        } catch (NullPointerException e) {
            assertNotNull(e.getMessage());
        }
        try {
            IPv4AddressWithMask.of(IPv4Address.of("10.10.10.0"),
                                   IPv4Address.of("255.0.255.0"))
                                   .getSubnetBroadcastAddress();
            fail("Should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertNotNull(e.getMessage());
        }
    }
}
