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
     * @param deviceId device identifier
     * @param beginLabel the begin label number of resource
     * @param endLabel the end label number of resource
     */
    void createDevicePool(DeviceId deviceId, LabelResourceId beginLabel,
                          LabelResourceId endLabel);

    /**
     * create the global label resource pool.
     *
     * @param beginLabel the begin label number of resource
     * @param endLabel the end label number of resource
     */
    void createGlobalPool(LabelResourceId beginLabel, LabelResourceId endLabel);

    /**
     * destroy a label resource pool of a specific device id.
     * @param deviceId device identifier
     */
    void destroyDevicePool(DeviceId deviceId);

    /**
     * destroy the global label resource pool.
     */
    void destroyGlobalPool();

    /**
     * get labels from resource pool by a specific device id.
     *
     * @param deviceId device identifier
     * @param applyNum the applying number
     * @return collection of applying labels
     */
    Collection<DefaultLabelResource> applyFromDevicePool(DeviceId deviceId,
                                                         ApplyLabelNumber applyNum);

    /**
     * get labels from the global label resource pool.
     *
     * @param applyNum the applying number
     * @return collection of applying labels
     */
    Collection<DefaultLabelResource> applyFromGlobalPool(
            ApplyLabelNumber applyNum);

    /**
     * release unused labels to device pools  .
     *
     * @param release the collection of releasing labels
     * @return success or fail
     */
    boolean releaseToDevicePool(
            Multimap<DeviceId, DefaultLabelResource> release);

    /**
     * release unused labels to the global resource pool.
     *
     * @param release release the collection of releasing labels
     * @return success or fail
     */
    boolean releaseToGlobalPool(Set<DefaultLabelResource> release);

    /**
     * judge if the pool of a specific device id is full.
     * @param deviceId device identifier
     * @return yes or no
     */
    boolean isDevicePoolFull(DeviceId deviceId);

    /**
     * judge if the global resource pool is full.
     * @return yes or no
     */
    boolean isGlobalPoolFull();

    /**
     * get the unused number of a label resource pool by a specific device id.
     * @param deviceId device identifier
     * @return the free number
     */
    long getFreeNumOfDevicePool(DeviceId deviceId);

    /**
     * get the unused number of a global label resource pool.
     * @return the free number of global resource pool
     */
    long getFreeNumOfGlobalPool();

    /**
     * get the label resource pool of a label resource by a specific device id.
     * @param deviceId device identifier
     * @return the device label resource pool
     */
    LabelResourcePool getDeviceLabelResourcePool(DeviceId deviceId);

    /**
     * get the global label resource pool.
     * @return the global label resource pool
     */
    LabelResourcePool getGlobalLabelResourcePool();

    /**
     * Adds the specified label resource listener.
     * @param listener label resource listener
     */
    void addListener(LabelResourceListener listener);

    /**
     * Removes the specified label resource listener.
     * @param listener label resource listener
     */
    void removeListener(LabelResourceListener listener);
}
