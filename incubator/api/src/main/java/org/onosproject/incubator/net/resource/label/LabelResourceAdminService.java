package org.onosproject.incubator.net.resource.label;

import com.google.common.annotations.Beta;
import org.onosproject.net.DeviceId;

/**
 * Service for managing label resource.
 */
@Beta
public interface LabelResourceAdminService {
    /**
     * Creates the only label resource of some device id from begin label to end
     * label.
     *
     * @param deviceId device identifier
     * @param beginLabel represents for the first label id in the range of label
     *            pool
     * @param endLabel represents for the last label id in the range of label
     *            pool
     * @return success or fail
     */
    boolean createDevicePool(DeviceId deviceId, LabelResourceId beginLabel,
                             LabelResourceId endLabel);

    /**
     * Creates the only global label resource pool.
     *
     * @param beginLabel represents for the first label id in the range of label
     *            pool
     * @param endLabel represents for the last label id in the range of label
     *            pool
     * @return success or fail
     */
    boolean createGlobalPool(LabelResourceId beginLabel,
                             LabelResourceId endLabel);

    /**
     * Destroys a label resource pool of a specific device id.
     *
     * @param deviceId device identifier
     * @return success or fail
     */
    boolean destroyDevicePool(DeviceId deviceId);

    /**
     * Destroys the global label resource pool.
     *
     * @return success or fail
     */
    boolean destroyGlobalPool();
}
