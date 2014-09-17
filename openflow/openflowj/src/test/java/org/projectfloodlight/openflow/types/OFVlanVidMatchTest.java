package org.projectfloodlight.openflow.types;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.hamcrest.CoreMatchers;
import org.junit.Test;

public class OFVlanVidMatchTest {
    @Test
    public void testofVlanVid() {
        assertThat(
                (int) OFVlanVidMatch.ofVlanVid(VlanVid.ofVlan(1)).getRawVid(),
                equalTo(0x1001));
        assertThat(
                (int) OFVlanVidMatch.ofVlanVid(VlanVid.ofVlan(0xFFF)).getRawVid(),
                equalTo(0x1FFF));
        assertThat(OFVlanVidMatch.ofVlanVid(null), equalTo(OFVlanVidMatch.UNTAGGED));
        assertThat(OFVlanVidMatch.ofVlanVid(VlanVid.NO_MASK),
                equalTo(OFVlanVidMatch.NO_MASK));
        // a fully masked VlanVid means "PRESENT" in OFVlanVid
        // (because a VlanVid always specifies a Vlan)
        assertThat(OFVlanVidMatch.ofVlanVid(VlanVid.FULL_MASK),
                equalTo(OFVlanVidMatch.PRESENT));
    }
    @Test
    public void testtoVlanVid() {
        assertThat(
                OFVlanVidMatch.ofRawVid((short)0x1001).getVlanVid(),
                                        equalTo(VlanVid.ofVlan(1)));
        assertThat(
                OFVlanVidMatch.ofRawVid((short)0x1FFF).getVlanVid(),
                                        equalTo(VlanVid.ofVlan(0xFFF)));
        assertThat(OFVlanVidMatch.UNTAGGED.getVlanVid(), CoreMatchers.nullValue());
        assertThat(
                OFVlanVidMatch.NO_MASK.getVlanVid(),
                                        equalTo(VlanVid.NO_MASK));
        assertThat(
                OFVlanVidMatch.PRESENT.getVlanVid(),
                                        equalTo(VlanVid.FULL_MASK));
    }


}
