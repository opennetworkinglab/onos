package org.onlab.onos.net;

import java.net.URI;

/**
 * Immutable representation of a device identity.
 */
public class DeviceId extends ElementId {

    /**
     * Creates a device id using the supplied URI.
     *
     * @param uri backing device URI
     */
    public DeviceId(URI uri) {
        super(uri);
    }

}
