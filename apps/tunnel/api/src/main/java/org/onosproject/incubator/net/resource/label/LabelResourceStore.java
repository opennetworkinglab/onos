/*
 * Copyright 2018-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.incubator.net.resource.label;

import java.util.Collection;
import java.util.Set;

import com.google.common.annotations.Beta;
import org.onosproject.net.DeviceId;
import org.onosproject.store.Store;

import com.google.common.collect.Multimap;

/**
 * Manages inventory of label; not intended for direct use.
 *
 */
@Beta
public interface LabelResourceStore
        extends Store<LabelResourceEvent, LabelResourceDelegate> {

    /**
     * Creates a label resource of some device id from begin label to end label.
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
     * Creates the global label resource pool.
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
     * Destroys a the global label resource pool.
     *
     * @return success or fail
     */
    boolean destroyGlobalPool();

    /**
     * Returns labels from resource pool by a specific device id.
     *
     * @param deviceId device identifier
     * @param applyNum the applying number
     * @return collection of applying labels
     */
    Collection<LabelResource> applyFromDevicePool(DeviceId deviceId,
                                                  long applyNum);

    /**
     * Returns labels from the global label resource pool.
     *
     * @param applyNum apply the number of labels
     * @return collection of labels
     */
    Collection<LabelResource> applyFromGlobalPool(long applyNum);

    /**
     * Releases unused labels to device pools .
     *
     * @param release the collection of releasing labels
     * @return success or fail
     */
    boolean releaseToDevicePool(Multimap<DeviceId, LabelResource> release);

    /**
     * Releases unused labels to the global resource pool.
     *
     * @param release release the collection of releasing labels
     * @return success or fail
     */
    boolean releaseToGlobalPool(Set<LabelResourceId> release);

    /**
     * Judges if the pool of a specific device id is full.
     *
     * @param deviceId device identifier
     * @return yes or no
     */
    boolean isDevicePoolFull(DeviceId deviceId);

    /**
     * Judges if the global resource pool is full.
     *
     * @return yes or no
     */
    boolean isGlobalPoolFull();

    /**
     * Returns the unused label number of a label resource pool by a specific device
     * id.
     *
     * @param deviceId device identifier
     * @return number of unused labels
     */
    long getFreeNumOfDevicePool(DeviceId deviceId);

    /**
     * Returns the unused number of a global label resource pool.
     *
     * @return number of unused labels
     */
    long getFreeNumOfGlobalPool();

    /**
     * Returns the label resource pool by a specific device id.
     *
     * @param deviceId device identifier
     * @return the device label resource pool
     */
    LabelResourcePool getDeviceLabelResourcePool(DeviceId deviceId);

    /**
     * Returns the global label resource pool.
     *
     * @return the global label resource pool
     */
    LabelResourcePool getGlobalLabelResourcePool();
}
