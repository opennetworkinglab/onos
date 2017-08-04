/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.scalablegateway.api;

import org.onosproject.core.GroupId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;

import java.util.List;

/**
 * ScalableGateWay Service Interface.
 */
public interface ScalableGatewayService {

    /**
     * Returns gateway node with the given device identifier.
     *
     * @param deviceId The gateway node deviceId
     * @return The gateway node information
     */
    GatewayNode getGatewayNode(DeviceId deviceId);

    /**
     * Returns the uplink port number of the gateway with the supplied device ID.
     *
     * @param deviceId the gateway node device id
     * @return the external interface port number
     */
    PortNumber getUplinkPort(DeviceId deviceId);

    /**
     * Returns group id for gateway load balance.
     * If the group does not exist in the supplied source device, creates one.
     *
     * @param srcDeviceId source device id
     * @return The group id
     */
    GroupId getGatewayGroupId(DeviceId srcDeviceId);

    /**
     * Returns the list of gateway node information with the given device identifier.
     *
     * @return The list of gateway node information
     */
    List<GatewayNode> getGatewayNodes();

    /**
     * Returns the list of gateway`s device identifiers.
     *
     * @return The list of device identifier]
     */
    List<DeviceId> getGatewayDeviceIds();

    /**
     * Adds gateway node in scalableGW application.
     *
     * @param gatewayNode Target gateway node
     * @return Result of method call
     */
    boolean addGatewayNode(GatewayNode gatewayNode);

    /**
     * Removes gateway node in scalableGW application.
     *
     * @param gatewayNode Target gateway node
     * @return Result of method call
     */
    boolean deleteGatewayNode(GatewayNode gatewayNode);
}