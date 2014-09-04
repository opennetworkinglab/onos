package org.onlab.onos.net;

import java.net.URI;

/**
 * Immutable representation of a device identity.
 */
public final class DeviceId extends ElementId {

    // Public construction is prohibited
    private DeviceId(URI uri) {
        super(uri);
    }

    /**
     * Creates a device id using the supplied URI.
     *
     * @param uri device URI
     */
    public static DeviceId deviceId(URI uri) {
        return new DeviceId(uri);
    }

    /**
     * Creates a device id using the supplied URI string.
     *
     * @param string device URI string
     */
    public static DeviceId deviceId(String string) {
        return new DeviceId(URI.create(string));
    }

}
