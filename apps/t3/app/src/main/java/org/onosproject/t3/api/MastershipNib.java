/*
 * Copyright 2020-present Open Networking Foundation
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

package org.onosproject.t3.api;

import com.google.common.collect.ImmutableMap;
import org.onosproject.cluster.NodeId;
import org.onosproject.net.DeviceId;

import java.util.Map;

/**
 * Represents Network Information Base (NIB) for mastership
 * and supports alternative functions to
 * {@link org.onosproject.mastership.MastershipService} for offline data.
 */
public class MastershipNib extends AbstractNib {

    private Map<DeviceId, NodeId> deviceMasterMap;

    // use the singleton helper to create the instance
    protected MastershipNib() {
    }

    /**
     * Sets a map of device id : master node id.
     *
     * @param deviceMasterMap device-master map
     */
    public void setDeviceMasterMap(Map<DeviceId, NodeId> deviceMasterMap) {
        this.deviceMasterMap = deviceMasterMap;
    }

    /**
     * Returns the device-master map.
     *
     * @return device-master map
     */
    public Map<DeviceId, NodeId> getDeviceMasterMap() {
        return ImmutableMap.copyOf(deviceMasterMap);
    }

    /**
     * Returns the current master for a given device.
     *
     * @param deviceId the identifier of the device
     * @return the ID of the master controller for the device
     */
    public NodeId getMasterFor(DeviceId deviceId) {
        return deviceMasterMap.get(deviceId);
    }

    /**
     * Returns the singleton instance of mastership NIB.
     *
     * @return instance of mastership NIB
     */
    public static MastershipNib getInstance() {
        return MastershipNib.SingletonHelper.INSTANCE;
    }

    private static class SingletonHelper {
        private static final MastershipNib INSTANCE = new MastershipNib();
    }

}
