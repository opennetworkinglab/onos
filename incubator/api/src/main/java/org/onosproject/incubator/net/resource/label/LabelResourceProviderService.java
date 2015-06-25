package org.onosproject.incubator.net.resource.label;

import com.google.common.annotations.Beta;
import org.onosproject.net.DeviceId;
import org.onosproject.net.provider.ProviderService;

/**
 * Means for injecting label information into the core.
 */
@Beta
public interface LabelResourceProviderService extends ProviderService<LabelResourceProvider> {

    /**
     * Signals that a device label resource pool has been detected.
     * @param deviceId device identifier
     * @param beginLabel the begin label number of resource
     * @param endLabel the end label number of resource
     */
    void deviceLabelResourcePoolDetected(DeviceId deviceId,
                                         LabelResourceId beginLabel,
                                         LabelResourceId endLabel);

    /**
     * Signals that an label resource pool has been destroyed.
     * @param deviceId device identifier
     */
    void deviceLabelResourcePoolDestroyed(DeviceId deviceId);
}
