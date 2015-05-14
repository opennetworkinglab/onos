package org.onosproject.openflow.controller;

/**
 * Port description property types (OFPPDPT enums) in OF 1.3 &lt;.
 */
public enum PortDescPropertyType {
    ETHERNET(0),            /* Ethernet port */
    OPTICAL(1),             /* Optical port */
    OPTICAL_TRANSPORT(2),   /* OF1.3 Optical transport extension */
    PIPELINE_INPUT(2),      /* Ingress pipeline */
    PIPELINE_OUTPUT(3),     /* Egress pipeline */
    RECIRCULATE(4),         /* Recirculation */
    EXPERIMENTER(0xffff);   /* Experimenter-implemented */

    private final int value;

    PortDescPropertyType(int v) {
        value = v;
    }

    public int valueOf() {
        return value;
    }
}
