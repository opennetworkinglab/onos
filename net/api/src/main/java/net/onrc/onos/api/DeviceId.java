package net.onrc.onos.api;

import java.net.URI;

/**
 * Immutable representaion of a device identity.
 */
public class DeviceId {

    private final URI uri;

    public DeviceId(URI uri) {
        this.uri = uri;
    }

    /**
     * Returns the backing URI.
     *
     * @return backing device URI
     */
    public URI uri() {
        return uri;
    }

}
