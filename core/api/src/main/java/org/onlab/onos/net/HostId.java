package org.onlab.onos.net;

import org.onlab.packet.MACAddress;
import org.onlab.packet.VLANID;

import java.net.URI;

/**
 * Immutable representation of a host identity.
 */
public final class HostId extends ElementId {

    // Public construction is prohibited
    private HostId(URI uri) {
        super(uri);
    }

    /**
     * Creates a device id using the supplied URI.
     *
     * @param uri device URI
     * @return host identifier
     */
    public static HostId hostId(URI uri) {
        return new HostId(uri);
    }

    /**
     * Creates a device id using the supplied URI string.
     *
     * @param string device URI string
     * @return host identifier
     */
    public static HostId hostId(String string) {
        return hostId(URI.create(string));
    }

    /**
     * Creates a device id using the supplied MAC &amp; VLAN ID.
     *
     * @param mac    mac address
     * @param vlanId vlan identifier
     * @return host identifier
     */
    // FIXME: replace vlanId long with a rich data-type, e.g. VLanId or something like that
    public static HostId hostId(MACAddress mac, VLANID vlanId) {
        // FIXME: use more efficient means of encoding
        return hostId("nic" + ":" + mac + "/" + vlanId);
    }

}
