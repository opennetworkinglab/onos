package org.projectfloodlight.openflow.protocol;

public enum OFVersion {
    OF_10(1), OF_11(2), OF_12(3), OF_13(4);

    public final int wireVersion;

    OFVersion(final int wireVersion) {
        this.wireVersion = wireVersion;
    }

    public int getWireVersion() {
        return wireVersion;
    }

}
