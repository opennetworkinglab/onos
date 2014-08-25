package org.projectfloodlight.protocol;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.projectfloodlight.openflow.protocol.OFFactories;
import org.projectfloodlight.openflow.protocol.OFOxmList;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.protocol.oxm.OFOxmIpv6DstMasked;
import org.projectfloodlight.openflow.protocol.oxm.OFOxmIpv6SrcMasked;
import org.projectfloodlight.openflow.protocol.oxm.OFOxms;
import org.projectfloodlight.openflow.types.IPv6AddressWithMask;

public class OFOxmListTest {
    private OFOxms oxms;

    @Before
    public void setup() {
        oxms = OFFactories.getFactory(OFVersion.OF_13).oxms();
    }

    @Test
    public void testCanonicalize() {
        OFOxmList.Builder builder = new OFOxmList.Builder();
        IPv6AddressWithMask fullMasked = IPv6AddressWithMask.of("::/0");
        OFOxmIpv6DstMasked  fullMaskedOxm = oxms.ipv6DstMasked(fullMasked.getValue(), fullMasked.getMask());
        builder.set(fullMaskedOxm);

        IPv6AddressWithMask address= IPv6AddressWithMask.of("1:2:3:4:5:6::8");
        OFOxmIpv6SrcMasked  addressSrcOxm = oxms.ipv6SrcMasked(address.getValue(), address.getMask());
        builder.set(addressSrcOxm);

        OFOxmList list = builder.build();
        assertThat(list.get(MatchField.IPV6_DST), CoreMatchers.nullValue());
        assertFalse(list.get(MatchField.IPV6_SRC).isMasked());
    }
}
