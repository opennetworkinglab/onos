package org.onlab.packet;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.google.common.testing.EqualsTester;

public class VlanIdTest {

    @Test
    public void testEquality() {

        VlanId vlan1 = VlanId.vlanId((short) -1);
        VlanId vlan2 = VlanId.vlanId((short) 100);
        VlanId vlan3 = VlanId.vlanId((short) 100);

        new EqualsTester().addEqualityGroup(VlanId.vlanId(), vlan1)
        .addEqualityGroup(vlan2, vlan3)
        .addEqualityGroup(VlanId.vlanId((short) 10));

    }

    @Test
    public void basics() {
        // purposefully create UNTAGGED VLAN
        VlanId vlan1 = VlanId.vlanId((short) 10);
        VlanId vlan2 = VlanId.vlanId((short) -1);

        assertEquals("incorrect VLAN value", 10, vlan1.toShort());
        assertEquals("invalid untagged value", VlanId.UNTAGGED, vlan2.toShort());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllicitVLAN() {
        VlanId.vlanId((short) 5000);
    }
}
