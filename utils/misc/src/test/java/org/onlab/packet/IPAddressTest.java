package org.onlab.packet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;
import org.onlab.packet.IpAddress.Version;

import com.google.common.testing.EqualsTester;

public class IPAddressTest {

    private static final byte [] BYTES1 = new byte [] {0xa, 0x0, 0x0, 0xa};
    private static final byte [] BYTES2 = new byte [] {0xa, 0x0, 0x0, 0xb};
    private static final int INTVAL1 = 167772170;
    private static final int INTVAL2 = 167772171;
    private static final String STRVAL = "10.0.0.12";
    private static final int MASK = 16;

    @Test
    public void testEquality() {
        IpAddress ip1 = IpAddress.valueOf(BYTES1);
        IpAddress ip2 = IpAddress.valueOf(INTVAL1);
        IpAddress ip3 = IpAddress.valueOf(BYTES2);
        IpAddress ip4 = IpAddress.valueOf(INTVAL2);
        IpAddress ip5 = IpAddress.valueOf(STRVAL);

        new EqualsTester().addEqualityGroup(ip1, ip2)
        .addEqualityGroup(ip3, ip4)
        .addEqualityGroup(ip5)
        .testEquals();

        // string conversions
        IpAddress ip6 = IpAddress.valueOf(BYTES1, MASK);
        IpAddress ip7 = IpAddress.valueOf("10.0.0.10/16");
        IpAddress ip8 = IpAddress.valueOf(new byte [] {0xa, 0x0, 0x0, 0xc});
        assertEquals("incorrect address conversion", ip6, ip7);
        assertEquals("incorrect address conversion", ip5, ip8);
    }

    @Test
    public void basics() {
        IpAddress ip1 = IpAddress.valueOf(BYTES1, MASK);
        final byte [] bytes = new byte [] {0xa, 0x0, 0x0, 0xa};

        //check fields
        assertEquals("incorrect IP Version", Version.INET, ip1.version());
        assertEquals("incorrect netmask", 16, ip1.netmask);
        assertTrue("faulty toOctets()", Arrays.equals(bytes, ip1.toOctets()));
        assertEquals("faulty toInt()", INTVAL1, ip1.toInt());
        assertEquals("faulty toString()", "10.0.0.10/16", ip1.toString());
    }

    @Test
    public void netmasks() {
        // masked
        IpAddress ip1 = IpAddress.valueOf(BYTES1, MASK);

        IpAddress host = IpAddress.valueOf("0.0.0.10/16");
        IpAddress network = IpAddress.valueOf("10.0.0.0/16");
        assertEquals("incorrect host address", host, ip1.host());
        assertEquals("incorrect network address", network, ip1.network());
        assertEquals("incorrect netmask", "255.255.0.0", ip1.netmask().toString());

        //unmasked
        IpAddress ip2 = IpAddress.valueOf(BYTES1);
        IpAddress umhost = IpAddress.valueOf("10.0.0.10/0");
        IpAddress umnet = IpAddress.valueOf("0.0.0.0/0");
        assertEquals("incorrect host address", umhost, ip2.host());
        assertEquals("incorrect host address", umnet, ip2.network());
        assertTrue("incorrect netmask",
                Arrays.equals(IpAddress.ANY, ip2.netmask().toOctets()));
    }

    @Test
    public void testContains() {
        IpAddress slash31 = IpAddress.valueOf(BYTES1, 31);
        IpAddress slash32 = IpAddress.valueOf(BYTES1, 32);
        IpAddress differentSlash32 = IpAddress.valueOf(BYTES2, 32);

        assertTrue(slash31.contains(differentSlash32));
        assertFalse(differentSlash32.contains(slash31));

        assertTrue(slash31.contains(slash32));
        assertFalse(slash32.contains(differentSlash32));
        assertFalse(differentSlash32.contains(slash32));

        IpAddress zero = IpAddress.valueOf("0.0.0.0/0");
        assertTrue(zero.contains(differentSlash32));
        assertFalse(differentSlash32.contains(zero));

        IpAddress slash8 = IpAddress.valueOf("10.0.0.0/8");
        assertTrue(slash8.contains(slash31));
        assertFalse(slash31.contains(slash8));
    }
}
