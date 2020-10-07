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
package org.onosproject.openstacknode.api;

import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.ControllerInfo;
import org.onosproject.openstacknode.api.DpdkConfig.DatapathType;

import java.util.Collection;

public class OpenstackNodeAdapter implements OpenstackNode {
    public OpenstackNodeAdapter() {

    }
    @Override
    public String hostname() {
        return null;
    }

    @Override
    public OpenstackNode.NodeType type() {
        return null;
    }

    @Override
    public DeviceId ovsdb() {
        return null;
    }

    @Override
    public DeviceId intgBridge() {
        return null;
    }

    @Override
    public IpAddress managementIp() {
        return null;
    }

    @Override
    public IpAddress dataIp() {
        return null;
    }

    @Override
    public String vlanIntf() {
        return null;
    }

    @Override
    public NodeState state() {
        return null;
    }

    @Override
    public PortNumber greTunnelPortNum() {
        return null;
    }

    @Override
    public PortNumber vxlanTunnelPortNum() {
        return null;
    }

    @Override
    public PortNumber geneveTunnelPortNum() {
        return null;
    }

    @Override
    public PortNumber vlanPortNum() {
        return null;
    }

    @Override
    public PortNumber patchPortNum() {
        return null;
    }

    @Override
    public MacAddress portMacByName(String portName) {
        return null;
    }

    @Override
    public PortNumber portNumByName(String portName) {
        return null;
    }

    @Override
    public MacAddress vlanPortMac() {
        return null;
    }

    @Override
    public String uplinkPort() {
        return null;
    }

    @Override
    public DatapathType datapathType() {
        return DatapathType.NORMAL;
    }

    @Override
    public String socketDir() {
        return null;
    }

    @Override
    public PortNumber uplinkPortNum() {
        return null;
    }

    @Override
    public OpenstackNode updateState(NodeState newState) {
        return null;
    }

    @Override
    public OpenstackNode updateIntbridge(DeviceId newIntgBridge) {
        return null;
    }

    @Override
    public Collection<OpenstackPhyInterface> phyIntfs() {
        return null;
    }

    @Override
    public Collection<ControllerInfo> controllers() {
        return null;
    }

    @Override
    public OpenstackSshAuth sshAuthInfo() {
        return null;
    }

    @Override
    public DpdkConfig dpdkConfig() {
        return null;
    }

    @Override
    public KeystoneConfig keystoneConfig() {
        return null;
    }

    @Override
    public NeutronConfig neutronConfig() {
        return null;
    }
}
