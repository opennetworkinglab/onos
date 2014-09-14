package org.onlab.packet;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.google.common.testing.EqualsTester;

public class VLANIDTest {

    @Test
    public void testEquality() {

        VLANID vlan1 = VLANID.vlanId((short) -1);
        VLANID vlan2 = VLANID.vlanId((short) 100);
        VLANID vlan3 = VLANID.vlanId((short) 100);

        new EqualsTester().addEqualityGroup(VLANID.vlanId(), vlan1)
        .addEqualityGroup(vlan2, vlan3)
        .addEqualityGroup(VLANID.vlanId((short) 10));

    }

    @Test
    public void basics() {
        // purposefully create UNTAGGED VLAN
        VLANID vlan1 = VLANID.vlanId((short) 10);
        VLANID vlan2 = VLANID.vlanId((short) -1);

        assertEquals("incorrect VLAN value", 10, vlan1.toShort());
        assertEquals("invalid untagged value", VLANID.UNTAGGED, vlan2.toShort());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllicitVLAN() {
        VLANID.vlanId((short) 5000);
    }
}
