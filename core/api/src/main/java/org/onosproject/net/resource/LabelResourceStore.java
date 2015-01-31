package org.onosproject.net.resource;

import java.util.Collection;

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
     * @param deviceId
     * @param beginLabel
     * @param endLabel
     * @return LabelResourceEvent
     */
    LabelResourceEvent create(DeviceId deviceId, long beginLabel, long endLabel);

    /**
     * create a label resource of some device id by exsiting pool.
     * @param labelResourcePool
     * @return LabelResourceEvent
     */
    LabelResourceEvent create(LabelResourcePool labelResourcePool);

    /**
     * destroy a label resource pool of a specific device id.
     * @param deviceId
     * @return LabelResourceEvent
     */
    LabelResourceEvent destroy(DeviceId deviceId);

    /**
     * get labels from resource pool by a specific device id.
     * @param deviceId
     * @param applyNum
     * @return collection of labels
     */
    Collection<DefaultLabelResource> apply(DeviceId deviceId, long applyNum);

    /**
     * release unused labels to pool.
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
}
