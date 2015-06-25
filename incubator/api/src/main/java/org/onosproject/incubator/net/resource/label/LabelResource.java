package org.onosproject.incubator.net.resource.label;

import com.google.common.annotations.Beta;
import org.onosproject.net.Annotated;
import org.onosproject.net.DeviceId;
import org.onosproject.net.NetworkResource;
import org.onosproject.net.Provided;

/**
 * Representation of label resource.
 */
@Beta
public interface LabelResource extends Annotated, Provided, NetworkResource {
    /**
     * Returns device id.
     * @return DeviceId
     */
    DeviceId deviceId();

    /**
     * Returns label resource identifier.
     *
     * @return resource id
     */
    LabelResourceId labelResourceId();
}
