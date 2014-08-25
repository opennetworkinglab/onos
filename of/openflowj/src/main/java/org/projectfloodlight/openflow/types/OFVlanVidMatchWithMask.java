package org.projectfloodlight.openflow.types;

public class OFVlanVidMatchWithMask extends Masked<OFVlanVidMatch> {
    private OFVlanVidMatchWithMask(OFVlanVidMatch value, OFVlanVidMatch mask) {
        super(value, mask);
    }

    /* a combination of Vlan Vid and mask that matches any tagged packet */
    public final static OFVlanVidMatchWithMask ANY_TAGGED = new OFVlanVidMatchWithMask(OFVlanVidMatch.PRESENT, OFVlanVidMatch.PRESENT);
}
