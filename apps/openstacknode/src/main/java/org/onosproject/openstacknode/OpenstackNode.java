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
package org.onosproject.openstacknode;

import com.google.common.base.MoreObjects;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.TpPort;
import org.onosproject.net.DeviceId;

import java.util.Comparator;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Representation of a compute/gateway node for OpenstackSwitching/Routing service.
 */
public final class OpenstackNode {

    private final String hostName;
    private final IpAddress ovsdbIp;
    private final TpPort ovsdbPort;
    private final DeviceId bridgeId;
    private final OpenstackNodeService.OpenstackNodeType openstackNodeType;
    private final String gatewayExternalInterfaceName;
    private final MacAddress gatewayExternalInterfaceMac;
    private static final String OVSDB = "ovsdb:";


    public static final Comparator<OpenstackNode> OPENSTACK_NODE_COMPARATOR =
            (node1, node2) -> node1.hostName().compareTo(node2.hostName());

    /**
     * Creates a new node.
     *
     * @param hostName hostName
     * @param ovsdbIp OVSDB server IP address
     * @param ovsdbPort OVSDB server port number
     * @param bridgeId integration bridge identifier
     * @param openstackNodeType openstack node type
     * @param gatewayExternalInterfaceName gatewayExternalInterfaceName
     * @param gatewayExternalInterfaceMac gatewayExternalInterfaceMac
     */
    public OpenstackNode(String hostName, IpAddress ovsdbIp, TpPort ovsdbPort, DeviceId bridgeId,
                         OpenstackNodeService.OpenstackNodeType openstackNodeType,
                         String gatewayExternalInterfaceName,
                         MacAddress gatewayExternalInterfaceMac) {
        this.hostName = checkNotNull(hostName, "hostName cannot be null");
        this.ovsdbIp = checkNotNull(ovsdbIp, "ovsdbIp cannot be null");
        this.ovsdbPort = checkNotNull(ovsdbPort, "ovsdbPort cannot be null");
        this.bridgeId = checkNotNull(bridgeId, "bridgeId cannot be null");
        this.openstackNodeType = checkNotNull(openstackNodeType, "openstackNodeType cannot be null");
        this.gatewayExternalInterfaceName = gatewayExternalInterfaceName;
        this.gatewayExternalInterfaceMac = gatewayExternalInterfaceMac;

        if (openstackNodeType == OpenstackNodeService.OpenstackNodeType.GATEWAYNODE) {
            checkNotNull(gatewayExternalInterfaceName, "gatewayExternalInterfaceName cannot be null");
            checkNotNull(gatewayExternalInterfaceMac, "gatewayExternalInterfaceMac cannot be null");
        }
    }

    /**
     * Returns the OVSDB server IP address.
     *
     * @return ip address
     */
    public IpAddress ovsdbIp() {
        return this.ovsdbIp;
    }

    /**
     * Returns the OVSDB server port number.
     *
     * @return port number
     */
    public TpPort ovsdbPort() {
        return this.ovsdbPort;
    }

    /**
     * Returns the hostName.
     *
     * @return hostName
     */
    public String hostName() {
        return this.hostName;
    }

    /**
     * Returns the identifier of the integration bridge.
     *
     * @return device id
     */
    public DeviceId intBrId() {
        return this.bridgeId;
    }

    /**
     * Returns the identifier of the OVSDB device.
     *
     * @return device id
     */
    public DeviceId ovsdbId() {
        return DeviceId.deviceId(OVSDB.concat(this.ovsdbIp.toString()));
    }

    /**
     * Returns the openstack node type.
     *
     * @return openstack node type
     */
    public OpenstackNodeService.OpenstackNodeType openstackNodeType() {
        return this.openstackNodeType;
    }

    /**
     * Returns the gatewayExternalInterfaceName.
     *
     * @return gatewayExternalInterfaceName
     */
    public String gatewayExternalInterfaceName() {
        return this.gatewayExternalInterfaceName;
    }

    /**
     * Returns the gatewayExternalInterfaceMac.
     *
     * @return gatewayExternalInterfaceMac
     */
    public MacAddress gatewayExternalInterfaceMac() {
        return this.gatewayExternalInterfaceMac;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof OpenstackNode) {
            OpenstackNode that = (OpenstackNode) obj;

            if (Objects.equals(hostName, that.hostName) &&
                    Objects.equals(ovsdbIp, that.ovsdbIp) &&
                    Objects.equals(ovsdbPort, that.ovsdbPort) &&
                    Objects.equals(bridgeId, that.bridgeId) &&
                    Objects.equals(openstackNodeType, that.openstackNodeType)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(hostName, ovsdbIp, ovsdbPort, bridgeId, openstackNodeType);
    }

    @Override
    public String toString() {
        if (openstackNodeType == OpenstackNodeService.OpenstackNodeType.COMPUTENODE) {
            return MoreObjects.toStringHelper(getClass())
                    .add("host", hostName)
                    .add("ip", ovsdbIp)
                    .add("port", ovsdbPort)
                    .add("bridgeId", bridgeId)
                    .add("openstacknodetype", openstackNodeType)
                    .toString();
        } else {
            return MoreObjects.toStringHelper(getClass())
                    .add("host", hostName)
                    .add("ip", ovsdbIp)
                    .add("port", ovsdbPort)
                    .add("bridgeId", bridgeId)
                    .add("openstacknodetype", openstackNodeType)
                    .add("gatewayExternalInterfaceName", gatewayExternalInterfaceName)
                    .add("gatewayExternalInterfaceMac", gatewayExternalInterfaceMac)
                    .toString();
        }
    }
}

