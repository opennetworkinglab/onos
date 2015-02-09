package org.onosproject.net.resource;

import java.util.Collection;

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
    void create(DeviceId deviceId, LabelResourceId beginLabel,
                LabelResourceId endLabel);

    /**
     * create the global label resource pool.
     * when the only global label resource pool is created by using the command
     * "create-label-resource-pool", all label resource pool of network devices
     * is disable. the device id of global label resource pool is different from
     * the device id of network. It's special,e.g global-resource-pool-device-id
     *
     * @param deviceId
     * @param beginLabel
     * @param endLabel
     */
    void createGlobal(LabelResourceId beginLabel,
                                    LabelResourceId endLabel);

    /**
     * destroy a label resource pool of a specific device id.
     * @param deviceId
     */
    void destroy(DeviceId deviceId);

    /**
     * destroy a the global label resource pool.
     * when the only global label resource pool is destroyed by using the command
     * "destroy-label-resource-pool", all label resource pool of network devices
     * is enabled.
     *
     * @param deviceId
     */
    void destroyGlobal();

    /**
     * get labels from resource pool by a specific device id.
     * if the global resource pool is available, apply labels from it, and it doesn't matter what is the device id.
     * but if it is unavailable,apply labels from a pool of a specific device id
     *
     * @param deviceId
     * @param applyNum
     * @return collection of labels
     */
    Collection<DefaultLabelResource> apply(DeviceId deviceId, ApplyLabelNumber applyNum);

    /**
     * release unused labels to pool.
     * if the global resource pool is available, release labels to it, and it doesn't matter what is the device id.
     * but if it is unavailable, release labels to a pool of a specific device id
     *
     * @param release
     * @return success or fail
     */
    boolean release(Multimap<DeviceId, DefaultLabelResource> release);

    /**
     * judge if the pool of a specific device id is full.
     * @param deviceId
     * @return yes or no
     */
    boolean isFull(DeviceId deviceId);

    /**
     * get the unused number of a label resource by a specific device id.
     * @param deviceId
     * @return
     */
    long getFreeNum(DeviceId deviceId);

    /**
     * get the lable resource pool of a label resource by a specific device id.
     * @param deviceId
     * @return
     */
    LabelResourcePool getLabelResourcePool(DeviceId deviceId);

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
