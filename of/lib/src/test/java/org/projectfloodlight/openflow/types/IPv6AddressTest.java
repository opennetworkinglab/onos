package org.projectfloodlight.openflow.types;

import static org.junit.Assert.*;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.hamcrest.CoreMatchers;
import org.jboss.netty.buffer.ChannelBuffers;
import org.junit.Test;
import org.projectfloodlight.openflow.exceptions.OFParseError;

import com.google.common.io.BaseEncoding;

public class IPv6AddressTest {

    String[] testStrings = {
            "::",
            "::1",
            "ffe0::",
            "1:2:3:4:5:6:7:8"
    };


    private final BaseEncoding hex = BaseEncoding.base16().omitPadding().lowerCase();

    private class WithMaskTaskCase {
        final String input;
        boolean hasMask;
        int expectedMaskLength = 128;
        byte[] expectedMask = hex.decode("ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff".replaceAll(" ", ""));

        public WithMaskTaskCase(String input) {
            super();
            this.input = input;
        }

        public WithMaskTaskCase maskHex(String string) {
            string = string.replaceAll(" ", "");
            this.hasMask = true;
            expectedMask = hex.decode(string);
            return this;
        }

        public WithMaskTaskCase expectedMaskLength(int expectedLength) {
            this.expectedMaskLength = expectedLength;
            return this;
        }

    }

    WithMaskTaskCase[] withMasks = new WithMaskTaskCase[] {
            new WithMaskTaskCase("1::1/80")
                .maskHex("ff ff ff ff ff ff ff ff ff ff 00 00 00 00 00 00")
                .expectedMaskLength(80),

            new WithMaskTaskCase("ffff:ffee:1::/ff00:ff00:ff00:ff00::")
                .maskHex("ff 00 ff 00 ff 00 ff 00 00 00 00 00 00 00 00 00")
                .expectedMaskLength(-1),
            new WithMaskTaskCase("1:2:3:4:5:6:7:8/1::ff00:ff00")
                .maskHex("00 01 00 00 00 00 00 00 00 00 00 00 ff 00 ff 00")
                .expectedMaskLength(-1),
            new WithMaskTaskCase("1:2:3:4:5:6:7:8/::ff00:ff00")
                .maskHex("00 00 00 00 00 00 00 00 00 00 00 00 ff 00 ff 00")
                .expectedMaskLength(-1),
            new WithMaskTaskCase("1:2:3:4:5:6:7:8/ffff:ffff:ffff:ffff:ffff::ff00:ff00")
                .maskHex("ff ff ff ff ff ff ff ff ff ff 00 00 ff 00 ff 00")
                .expectedMaskLength(-1),
            new WithMaskTaskCase("8:8:8:8:8:8:8:8"),
            new WithMaskTaskCase("8:8:8:8:8:8:8:8"),
            new WithMaskTaskCase("1:2:3:4:5:6:7:8/128"),
            new WithMaskTaskCase("::/0")
                .maskHex("00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00")
                .expectedMaskLength(0),
    };

    @Test
    public void testLogicalOperatorsBroadcast() {
        assertTrue(IPv6Address.NO_MASK.not().equals(IPv6Address.FULL_MASK));
        assertTrue(IPv6Address.NO_MASK.or(IPv6Address.FULL_MASK).
                   equals(IPv6Address.NO_MASK));
        assertTrue(IPv6Address.NO_MASK.and(IPv6Address.FULL_MASK).
                   equals(IPv6Address.FULL_MASK));

        assertTrue(IPv6Address.NO_MASK.isBroadcast());
        assertTrue(!IPv6Address.FULL_MASK.isBroadcast());
    }

    @Test
    public void testMaskedSubnetBroadcast() {
        assertTrue(IPv6AddressWithMask.of("10:10::1/112")
                   .getSubnetBroadcastAddress()
                   .equals(IPv6Address.of("10:10::ffff")));
        assertTrue(IPv6AddressWithMask.of("10:10::1/112")
                   .isSubnetBroadcastAddress(IPv6Address.of("10:10::ffff")));
        assertTrue(!IPv6AddressWithMask.of("10:10::1/112")
                   .isSubnetBroadcastAddress(IPv6Address.of("10:10::fffd")));
    }

    @Test
    public void testConstants() {
        byte[] zeros = { (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                         (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                         (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                         (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00 };
        byte[] ones = { (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
                        (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
                        (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
                        (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF };
        // Make sure class initializtation and static assignment don't get
        // messed up. Test everything twice for cached values
        assertTrue(IPv6Address.NONE.isCidrMask());
        assertEquals(0, IPv6Address.NONE.asCidrMaskLength());
        assertArrayEquals(zeros, IPv6Address.NONE.getBytes());
        assertTrue(IPv6Address.NONE.isCidrMask());
        assertEquals(0, IPv6Address.NONE.asCidrMaskLength());
        assertArrayEquals(zeros, IPv6Address.NONE.getBytes());

        assertTrue(IPv6Address.NO_MASK.isCidrMask());
        assertEquals(128, IPv6Address.NO_MASK.asCidrMaskLength());
        assertArrayEquals(ones, IPv6Address.NO_MASK.getBytes());
        assertTrue(IPv6Address.NO_MASK.isCidrMask());
        assertEquals(128, IPv6Address.NO_MASK.asCidrMaskLength());
        assertArrayEquals(ones, IPv6Address.NO_MASK.getBytes());

        assertTrue(IPv6Address.FULL_MASK.isCidrMask());
        assertEquals(0, IPv6Address.FULL_MASK.asCidrMaskLength());
        assertArrayEquals(zeros, IPv6Address.FULL_MASK.getBytes());
        assertTrue(IPv6Address.FULL_MASK.isCidrMask());
        assertEquals(0, IPv6Address.FULL_MASK.asCidrMaskLength());
        assertArrayEquals(zeros, IPv6Address.FULL_MASK.getBytes());
    }

    @Test
    public void testMasked() throws UnknownHostException {
        for(WithMaskTaskCase w: withMasks) {
            IPv6AddressWithMask value = IPv6AddressWithMask.of(w.input);
            if (!w.hasMask) {
                IPv6Address ip = value.getValue();
                InetAddress inetAddress = InetAddress.getByName(w.input.split("/")[0]);

                assertArrayEquals(ip.getBytes(), inetAddress.getAddress());
                assertEquals(w.input.split("/")[0], ip.toString());
            }
            InetAddress inetAddress = InetAddress.getByName(w.input.split("/")[0]);

            if (w.expectedMaskLength == -1) {
                assertFalse(value.getMask().isCidrMask());
                try {
                    value.getMask().asCidrMaskLength();
                    fail("Expected IllegalStateException not thrown");
                } catch(IllegalStateException e) {
                    //expected
                }
            } else {
                assertTrue(value.getMask().isCidrMask());
                assertEquals("Input " + w.input, w.expectedMaskLength,
                             value.getMask().asCidrMaskLength());
            }

            byte[] address = inetAddress.getAddress();
            assertEquals(address.length, value.getValue().getBytes().length);

            for (int j = 0; j < address.length; j++) {
                address[j] &= w.expectedMask[j];
            }

            assertThat("Address bytes for input " + w.input + ", value=" + value, value.getValue().getBytes(), CoreMatchers.equalTo(address));
            assertThat("mask check for input " + w.input + ", value=" + value, value.getMask().getBytes(), CoreMatchers.equalTo(w.expectedMask));
        }
        for (int i = 0; i <= 128; i++) {
            String ipString = String.format("8001:2::1/%d", i);
            IPv6AddressWithMask value = IPv6AddressWithMask.of(ipString);
            assertEquals("Input " + ipString, i, value.getMask().asCidrMaskLength());
        }
    }


    @Test
    public void testOfString() throws UnknownHostException {
        for(int i=0; i < testStrings.length; i++ ) {
            IPv6Address ip = IPv6Address.of(testStrings[i]);
            InetAddress inetAddress = InetAddress.getByName(testStrings[i]);

            assertArrayEquals(ip.getBytes(), inetAddress.getAddress());
            assertEquals(testStrings[i], ip.toString());
        }
    }

    @Test
    public void testOfByteArray() throws UnknownHostException {
        for(int i=0; i < testStrings.length; i++ ) {
            byte[] bytes = Inet6Address.getByName(testStrings[i]).getAddress();
            IPv6Address ip = IPv6Address.of(bytes);
            assertEquals(testStrings[i], ip.toString());
            assertArrayEquals(bytes, ip.getBytes());
        }
    }

    @Test
    public void testReadFrom() throws OFParseError, UnknownHostException {
        for(int i=0; i < testStrings.length; i++ ) {
            byte[] bytes = Inet6Address.getByName(testStrings[i]).getAddress();
            IPv6Address ip = IPv6Address.read16Bytes(ChannelBuffers.copiedBuffer(bytes));
            assertEquals(testStrings[i], ip.toString());
            assertArrayEquals(bytes, ip.getBytes());
        }
    }

    String[] invalidIPs = {
            "",
            ":",
            "1:2:3:4:5:6:7:8:9",
            "1:2:3:4:5:6:7:8:",
            "1:2:3:4:5:6:7:8g",
            "1:2:3:",
            "12345::",
            "1::3::8",
            "::3::"
    };

    @Test
    public void testInvalidIPs() throws OFParseError {
        for(String invalid : invalidIPs) {
            try {
                IPv6Address.of(invalid);
                fail("Invalid IP "+invalid+ " should have raised IllegalArgumentException");
            } catch(IllegalArgumentException e) {
                // ok
            }
        }
    }

    @Test
    public void testZeroCompression() throws OFParseError {
        assertEquals("::", IPv6Address.of("::").toString(true, false));
        assertEquals("0:0:0:0:0:0:0:0", IPv6Address.of("::").toString(false, false));
        assertEquals("0000:0000:0000:0000:0000:0000:0000:0000", IPv6Address.of("::").toString(false, true));
        assertEquals("1::4:5:6:0:8", IPv6Address.of("1:0:0:4:5:6:0:8").toString(true, false));
        assertEquals("1:0:0:4::8", IPv6Address.of("1:0:0:4:0:0:0:8").toString(true, false));
    }

    @Test
    public void testSuperclass() throws Exception {
        for(String ipString: testStrings) {
            IPAddress<?> superIp = IPAddress.of(ipString);
            assertEquals(IPVersion.IPv6, superIp.getIpVersion());
            assertEquals(IPv6Address.of(ipString), superIp);
        }

        for(WithMaskTaskCase w: withMasks) {
            String ipMaskedString = w.input;
            IPAddressWithMask<?> superIp = IPAddressWithMask.of(ipMaskedString);
            assertEquals(IPVersion.IPv6, superIp.getIpVersion());
            assertEquals(IPv6AddressWithMask.of(ipMaskedString), superIp);
        }
    }

    @Test
    public void testOfExceptions() throws Exception {
        try {
            IPv6AddressWithMask.of(null);
            fail("Should have thrown NullPointerException");
        } catch (NullPointerException e) {
            assertNotNull(e.getMessage());
        }
        try {
            String s = null;
            IPv6Address.of(s);
            fail("Should have thrown NullPointerException");
        } catch (NullPointerException e) {
            assertNotNull(e.getMessage());
        }
        try {
            byte[] b = null;
            IPv6Address.of(b);
            fail("Should have thrown NullPointerException");
        } catch (NullPointerException e) {
            assertNotNull(e.getMessage());
        }
        try {
            byte[] b = new byte[7];
            IPv6Address.of(b);
            fail("Should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected
        }
        try {
            byte[] b = new byte[9];
            IPv6Address.of(b);
            fail("Should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected
        }
        try {
            IPv6AddressWithMask.of(IPv6Address.of("1::"), null);
            fail("Should have thrown NullPointerException");
        } catch (NullPointerException e) {
            assertNotNull(e.getMessage());
        }
        try {
            IPv6AddressWithMask.of(null, IPv6Address.of("255::"));
            fail("Should have thrown NullPointerException");
        } catch (NullPointerException e) {
            assertNotNull(e.getMessage());
        }
        try {
            IPv6AddressWithMask.of(IPv6Address.of("10:10::0"),
                                   IPv6Address.of("ffff:0:ffff::"))
                                   .getSubnetBroadcastAddress();
            fail("Should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertNotNull(e.getMessage());
        }
    }
}
