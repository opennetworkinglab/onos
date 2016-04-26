/*
 * Copyright 2016-present Open Networking Laboratory
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

package org.onosproject.scalablegateway;

import org.onosproject.core.GroupId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;

import java.util.List;

/**
 * Manages gateway node for gateway scalability.
 */
public class ScalableGatewayManager implements ScalableGatewayService {

    @Override
    public GatewayNode getGatewayNode(DeviceId deviceId) {
        return null;
    }

    @Override
    public List<PortNumber> getGatewayExternalPorts(DeviceId deviceId) {
        return null;
    }

    @Override
    public GroupId getGroupIdForGatewayLoadBalance(DeviceId srcDeviceId) {
        return null;
    }

    @Override
    public List<GatewayNode> getGatewayNodes() {
        return null;
    }

    @Override
    public List<DeviceId> getGatewayDeviceIds() {
        return null;
    }

    @Override
    public boolean addGatewayNode(GatewayNode gatewayNode) {
        return false;
    }

    @Override
    public boolean deleteGatewayNode(GatewayNode gatewayNode) {
        return false;
    }
}
