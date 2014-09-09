package org.onlab.onos.net;

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
     */
    public static HostId hostId(URI uri) {
        return new HostId(uri);
    }

    /**
     * Creates a device id using the supplied URI string.
     *
     * @param string device URI string
     */
    public static HostId hostId(String string) {
        return hostId(URI.create(string));
    }

}
