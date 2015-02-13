package org.onosproject.net.flowext;

import java.nio.ByteBuffer;


/**
 * Represents a generic abstraction of the service data. User app can customize whatever it needs to install on devices.
 */
public interface FlowEntryExtension {
    // We can add something here later, like length, type, etc

    /**
     * Get the payload of flowExtension.
     *
     * @return  the byte steam value of payload.
     */
    ByteBuffer getPayload();
}
