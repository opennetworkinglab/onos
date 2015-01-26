package org.onosproject.net.resource;

import java.util.Collection;

import org.onosproject.net.DeviceId;

import com.google.common.collect.Multimap;

/**
 *Service for providing label resource allocation
 *
 */
public interface LabelResourceService {


        /**
         * create a label resource of some device id from begin label to end label.
         * @param deviceId
         * @param beginLabel
         * @param endLabel
         * @return LabelResourceEvent
         */
        void create(DeviceId deviceId, long beginLabel, long endLabel);

        /**
         * create a label resource of some device id by exsiting pool.
         * @param labelResourcePool
         * @return LabelResourceEvent
         */
        void create(LabelResourcePool labelResourcePool);

        /**
         * destroy a label resource pool of a specific device id.
         * @param deviceId
         * @return LabelResourceEvent
         */
        void destroy(DeviceId deviceId);

        /**
         * get labels from resource pool by a specific device id.
         * @param deviceId
         * @param applyNum
         * @return collection of labels
         */
        Collection<DefaultLabelResource> apply(DeviceId deviceId, long applyNum);

        /**
         * release unused labels to pool
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
         * Adds the specified label resource listener.
         *
         * @param listener flow rule listener
         */
        void addListener(LabelResourceListener listener);

        /**
         * Removes the specified label resource listener.
         *
         * @param listener flow rule listener
         */
        void removeListener(LabelResourceListener listener);
}
