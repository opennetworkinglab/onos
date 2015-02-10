package org.onosproject.net.resource;

import java.util.Collection;
import java.util.Set;

import org.onosproject.net.DeviceId;
import org.onosproject.store.Store;

import com.google.common.collect.Multimap;

/**
 * manages inventory of label; not intended for direct use.
 *
 */
public interface LabelResourceStore
        extends Store<LabelResourceEvent, LabelResourceDelegate> {

    /**
     * create a label resource of some device id from begin label to end label.
     *
     * @param deviceId device identifier
     * @param beginLabel the begin label number of resource
     * @param endLabel the end label number of resource
     * @return LabelResourceEvent
     */
    LabelResourceEvent createDevicePool(DeviceId deviceId, LabelResourceId beginLabel,
                              LabelResourceId endLabel);

    /**
     * create the global label resource pool.
     *
     * @param deviceId device identifier
     * @param beginLabel the begin label number of resource
     * @param endLabel the end label number of resource
     * @return LabelResourceEvent
     */
    LabelResourceEvent createGlobalPool(LabelResourceId beginLabel,
                                    LabelResourceId endLabel);

    /**
     * destroy a label resource pool of a specific device id.
     *
     * @param deviceId device identifier
     * @return LabelResourceEvent
     */
    LabelResourceEvent destroyDevicePool(DeviceId deviceId);

    /**
     * destroy a the global label resource pool.
     *
     * @param deviceId device identifier
     * @param applyNum the applying number
     * @return LabelResourceEvent
     */
    LabelResourceEvent destroyGlobalPool();

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
     * @return collection of labels
     */
    Collection<DefaultLabelResource> applyFromGlobalPool(ApplyLabelNumber applyNum);

    /**
     * release unused labels to device pools  .
     *
     * @param release the collection of releasing labels
     * @return success or fail
     */
    boolean releaseToDevicePool(Multimap<DeviceId, DefaultLabelResource> release);

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
     * @param deviceId
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
     * @param deviceId
     * @return the device label resource pool
     */
    LabelResourcePool getDeviceLabelResourcePool(DeviceId deviceId);

    /**
     * get the global label resource pool.
     * @param deviceId
     * @return the global label resource pool
     */
    LabelResourcePool getGlobalLabelResourcePool();
}
