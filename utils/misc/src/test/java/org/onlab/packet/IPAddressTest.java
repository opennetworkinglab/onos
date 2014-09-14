package org.onlab.packet;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;
import org.onlab.packet.IPAddress.Version;

import com.google.common.testing.EqualsTester;

public class IPAddressTest {

    private static final byte [] BYTES1 = new byte [] {0x0, 0x0, 0x0, 0xa};
    private static final byte [] BYTES2 = new byte [] {0x0, 0x0, 0x0, 0xb};
    private static final int INTVAL1 = 10;
    private static final int INTVAL2 = 12;
    private static final String STRVAL = "0.0.0.11";

    @Test
    public void testEquality() {
        IPAddress ip1 = IPAddress.valueOf(BYTES1);
        IPAddress ip2 = IPAddress.valueOf(BYTES2);
        IPAddress ip3 = IPAddress.valueOf(INTVAL1);
        IPAddress ip4 = IPAddress.valueOf(INTVAL2);
        IPAddress ip5 = IPAddress.valueOf(STRVAL);

        new EqualsTester().addEqualityGroup(ip1, ip3)
        .addEqualityGroup(ip2, ip5)
        .addEqualityGroup(ip4)
        .testEquals();
    }

    @Test
    public void basics() {
        IPAddress ip4 = IPAddress.valueOf(BYTES1);
        assertEquals("incorrect IP Version", Version.INET, ip4.version());
        assertEquals("faulty toOctets()", Arrays.equals(
                new byte [] {0x0, 0x0, 0x0, 0xa}, ip4.toOctets()), true);
        assertEquals("faulty toInt()", INTVAL1, ip4.toInt());
        assertEquals("faulty toString()", "0.0.0.10", ip4.toString());
    }
}
