package org.onosproject.net.resource;

import java.util.Collection;
import java.util.Set;

import org.onosproject.net.DeviceId;

import com.google.common.collect.Multimap;

/**
 * Service for providing label resource allocation.
 *
 */
public interface LabelResourceService {

    /**
     * create a label resource of some device id from begin label to end label.
     * @param deviceId
     * @param beginLabel
     * @param endLabel
     */
    void createDevicePool(DeviceId deviceId, LabelResourceId beginLabel,
                LabelResourceId endLabel);

    /**
     * create the global label resource pool.
     *
     * @param deviceId
     * @param beginLabel
     * @param endLabel
     */
    void createGlobalPool(LabelResourceId beginLabel,
                                    LabelResourceId endLabel);

    /**
     * destroy a label resource pool of a specific device id.
     * @param deviceId
     */
    void destroyDevicePool(DeviceId deviceId);

    /**
     * destroy the global label resource pool.
     *
     * @param deviceId
     */
    void destroyGlobalPool();

    /**
     * get labels from resource pool by a specific device id.
     *
     * @param deviceId
     * @param applyNum
     * @return collection of labels
     */
    Collection<DefaultLabelResource> applyFromDevicePool(DeviceId deviceId, ApplyLabelNumber applyNum);

    /**
     * get labels from the global label resource pool.
     *
     * @param deviceId
     * @param applyNum
     * @return collection of labels
     */
    Collection<DefaultLabelResource> applyFromGlobalPool(ApplyLabelNumber applyNum);

    /**
     * release unused labels to device pools  .
     *
     * @param release
     * @return success or fail
     */
    boolean releaseToDevicePool(Multimap<DeviceId, DefaultLabelResource> release);

    /**
     * release unused labels to the global resource pool.
     *
     * @param release
     * @return success or fail
     */
    boolean releaseToGlobalPool(Set<DefaultLabelResource> release);

    /**
     * judge if the pool of a specific device id is full.
     * @param deviceId
     * @return yes or no
     */
    boolean isDevicePoolFull(DeviceId deviceId);

    /**
     * judge if the global resource pool is full.
     * @param deviceId
     * @return yes or no
     */
    boolean isGlobalPoolFull();

    /**
     * get the unused number of a label resource pool by a specific device id.
     * @param deviceId
     * @return
     */
    long getFreeNumOfDevicePool(DeviceId deviceId);

    /**
     * get the unused number of a global label resource pool.
     * @param deviceId
     * @return
     */
    long getFreeNumOfGlobalPool();

    /**
     * get the label resource pool of a label resource by a specific device id.
     * @param deviceId
     * @return
     */
    LabelResourcePool getDeviceLabelResourcePool(DeviceId deviceId);

    /**
     * get the global label resource pool.
     * @param deviceId
     * @return
     */
    LabelResourcePool getGlobalLabelResourcePool();

    /**
     * Adds the specified label resource listener.
     * @param listener flow rule listener
     */
    void addListener(LabelResourceListener listener);

    /**
     * Removes the specified label resource listener.
     * @param listener flow rule listener
     */
    void removeListener(LabelResourceListener listener);
}
