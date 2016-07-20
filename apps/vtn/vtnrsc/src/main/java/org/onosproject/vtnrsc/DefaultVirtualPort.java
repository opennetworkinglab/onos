/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.vtnrsc;

import static com.google.common.base.MoreObjects.toStringHelper;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.onlab.packet.MacAddress;
import org.onosproject.net.DeviceId;

/**
 * Default implementation of VirtualPort interface .
 */
public final class DefaultVirtualPort implements VirtualPort {
    private final VirtualPortId id;
    private final TenantNetworkId networkId;
    private final Boolean adminStateUp;
    private final String name;
    private final State state;
    private final MacAddress macAddress;
    private final TenantId tenantId;
    private final String deviceOwner;
    private final DeviceId deviceId;
    private final Set<FixedIp> fixedIps;
    private final BindingHostId bindingHostId;
    private final String bindingVnicType;
    private final String bindingVifType;
    private final String bindingVifDetails;
    private final Set<AllowedAddressPair> allowedAddressPairs;
    private final Set<SecurityGroup> securityGroups;

    /**
     * Creates a VirtualPort object.
     *
     * @param id the virtual port identifier
     * @param networkId the network identifier
     * @param adminStateUp adminStateup true or false
     * @param strMap the map of properties of virtual port
     * @param state virtual port state
     * @param macAddress the MAC address
     * @param tenantId the tenant identifier
     * @param deviceId the device identifier
     * @param fixedIps set of fixed IP
     * @param bindingHostId the binding host identifier
     * @param allowedAddressPairs the collection of allowdeAddressPairs
     * @param securityGroups the collection of securityGroups
     */
    public DefaultVirtualPort(VirtualPortId id,
                              TenantNetworkId networkId,
                              Boolean adminStateUp,
                              Map<String, String> strMap,
                              State state,
                              MacAddress macAddress,
                              TenantId tenantId,
                              DeviceId deviceId,
                              Set<FixedIp> fixedIps,
                              BindingHostId bindingHostId,
                              Set<AllowedAddressPair> allowedAddressPairs,
                              Set<SecurityGroup> securityGroups) {
        this.id = id;
        this.networkId = networkId;
        this.adminStateUp = adminStateUp;
        this.name = strMap.get("name");
        this.state = state;
        this.macAddress = macAddress;
        this.tenantId = tenantId;
        this.deviceOwner = strMap.get("deviceOwner");
        this.deviceId = deviceId;
        this.fixedIps = fixedIps;
        this.bindingHostId = bindingHostId;
        this.bindingVnicType = strMap.get("bindingVnicType");
        this.bindingVifType = strMap.get("bindingVifType");
        this.bindingVifDetails = strMap.get("bindingVifDetails");
        this.allowedAddressPairs = allowedAddressPairs;
        this.securityGroups = securityGroups;
    }

    @Override
    public VirtualPortId portId() {
        return id;
    }

    @Override
    public TenantNetworkId networkId() {
        return networkId;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public boolean adminStateUp() {
        return adminStateUp;
    }

    @Override
    public State state() {
        return state;
    }

    @Override
    public MacAddress macAddress() {
        return macAddress;
    }

    @Override
    public TenantId tenantId() {
        return tenantId;
    }

    @Override
    public DeviceId deviceId() {
        return deviceId;
    }

    @Override
    public String deviceOwner() {
        return deviceOwner;
    }

    @Override
    public Collection<AllowedAddressPair> allowedAddressPairs() {
        return allowedAddressPairs;
    }

    @Override
    public Set<FixedIp> fixedIps() {
        return fixedIps;
    }

    @Override
    public BindingHostId bindingHostId() {
        return bindingHostId;
    }

    @Override
    public String bindingVnicType() {
        return bindingVifType;
    }

    @Override
    public String bindingVifType() {
        return bindingVifType;
    }

    @Override
    public String bindingVifDetails() {
        return bindingVifDetails;
    }

    @Override
    public Collection<SecurityGroup> securityGroups() {
        return securityGroups;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, networkId, adminStateUp, name, state,
                            macAddress, tenantId, deviceId, deviceOwner,
                            allowedAddressPairs, fixedIps, bindingHostId,
                            bindingVnicType, bindingVifType, bindingVifDetails,
                            securityGroups);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DefaultVirtualPort) {
            final DefaultVirtualPort that = (DefaultVirtualPort) obj;
            return Objects.equals(this.id, that.id)
                    && Objects.equals(this.networkId, that.networkId)
                    && Objects.equals(this.adminStateUp, that.adminStateUp)
                    && Objects.equals(this.state, that.state)
                    && Objects.equals(this.name, that.name)
                    && Objects.equals(this.tenantId, that.tenantId)
                    && Objects.equals(this.macAddress, that.macAddress)
                    && Objects.equals(this.deviceId, that.deviceId)
                    && Objects.equals(this.deviceOwner, that.deviceOwner)
                    && Objects.equals(this.allowedAddressPairs,
                                      that.allowedAddressPairs)
                    && Objects.equals(this.fixedIps, that.fixedIps)
                    && Objects.equals(this.bindingHostId, that.bindingHostId)
                    && Objects.equals(this.bindingVifDetails,
                                      that.bindingVifDetails)
                    && Objects.equals(this.bindingVifType, that.bindingVifType)
                    && Objects.equals(this.bindingVnicType,
                                      that.bindingVnicType)
                    && Objects.equals(this.securityGroups, that.securityGroups);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("id", id).add("network_id", networkId)
                .add("adminStateUp", adminStateUp).add("state", state)
                .add("name", name).add("state", state)
                .add("macAddress", macAddress).add("tenantId", tenantId)
                .add("deviced", deviceId).add("deviceOwner", deviceOwner)
                .add("allowedAddressPairs", allowedAddressPairs)
                .add("fixedIp", fixedIps).add("bindingHostId", bindingHostId)
                .add("bindingVnicType", bindingVnicType)
                .add("bindingVifDetails", bindingVifDetails)
                .add("bindingVifType", bindingVifType)
                .add("securityGroups", securityGroups).toString();
    }

}
