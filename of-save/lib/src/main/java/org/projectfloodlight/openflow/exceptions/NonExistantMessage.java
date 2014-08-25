package org.projectfloodlight.openflow.exceptions;

/**
 * Error: someone asked to create an OFMessage with wireformat type and version,
 * but that doesn't exist
 *
 * @author capveg
 */
public class NonExistantMessage extends Exception {

    private static final long serialVersionUID = 1L;
    byte type;
    byte version;

    /**
     * Error: someone asked to create an OFMessage with wireformat type and
     * version, but that doesn't exist
     *
     * @param type
     *            the wire format
     * @param version
     *            the OpenFlow wireformat version number, e.g. 1 == v1.1, 2 =
     *            v1.2, etc.
     */
    public NonExistantMessage(final byte type, final byte version) {
        this.type = type;
        this.version = version;
    }

}
