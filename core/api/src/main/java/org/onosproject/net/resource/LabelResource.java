package org.onosproject.net.resource;

import org.onosproject.net.Annotated;
import org.onosproject.net.DeviceId;
import org.onosproject.net.NetworkResource;
import org.onosproject.net.Provided;

/**
 * Representation of label resource.
 */
public interface LabelResource extends Annotated, Provided, NetworkResource {
    /**
     * Returns device id.
     * @return DeviceId
     */
    public DeviceId deviceId();

    /**
     * Returns labelResource Id.
     * @return LabelResourceId
     */
    public LabelResourceId labelResourceId();
}
