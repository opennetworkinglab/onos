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
package org.onosproject.openstackinterface;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;

import java.util.Collection;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * It represents the Openstack Port information.
 */
public final class OpenstackPort {

    public enum PortStatus {
        UP,
        DOWN,
        ACTIVE,
        NA,
    }

    private PortStatus status;
    private String name;
    private ImmutableMap<IpAddress, MacAddress> allowedAddressPairs;
    private boolean adminStateUp;
    private String networkId;
    private String tenantId;
    private String deviceOwner;
    private MacAddress macAddress;
    // <subnet id, ip address>
    private ImmutableMap<String, Ip4Address> fixedIps;
    private String id;
    private Collection<String> securityGroups;
    private String deviceId;

    private OpenstackPort(PortStatus status, String name, Map<IpAddress, MacAddress> allowedAddressPairs,
                          boolean adminStateUp, String networkId, String tenantId,
                          String deviceOwner, MacAddress macAddress, Map<String, Ip4Address> fixedIps,
                          String id, Collection<String> securityGroups, String deviceId) {
        this.status = status;
        this.name = name;
        this.allowedAddressPairs = checkNotNull(ImmutableMap.copyOf(allowedAddressPairs));
        this.adminStateUp = adminStateUp;
        this.networkId = checkNotNull(networkId);
        this.tenantId = checkNotNull(tenantId);
        this.deviceOwner = deviceOwner;
        this.macAddress = checkNotNull(macAddress);
        this.fixedIps = checkNotNull(ImmutableMap.copyOf(fixedIps));
        this.id = checkNotNull(id);
        this.securityGroups = securityGroups;
        this.deviceId = deviceId;
    }



    /**
     * Returns OpenstackPort builder object.
     *
     * @return OpenstackPort builder
     */
    public static OpenstackPort.Builder builder() {
        return new Builder();
    }

    /**
     * Returns port status.
     *
     * @return port status
     */
    public PortStatus status() {
        return status;
    }

    /**
     * Returns port name.
     *
     * @return port name
     */
    public String name() {
        return name;
    }

    /**
     * Returns allowed address pairs.
     *
     * @return map of ip address and mac address, or empty map
     */
    public Map<IpAddress, MacAddress> allowedAddressPairs() {
        return allowedAddressPairs;
    }

    /**
     * Returns whether admin state up or not.
     *
     * @return true if admin state up, false otherwise
     */
    public boolean isAdminStateUp() {
        return adminStateUp;
    }

    /**
     * Returns network ID.
     *
     * @return network ID
     */
    public String networkId() {
        return networkId;
    }

    /**
     * Returns device owner.
     *
     * @return device owner
     */
    public String deviceOwner() {
        return deviceOwner;
    }

    /**
     * Returns mac address.
     *
     * @return mac address
     */
    public MacAddress macAddress() {
        return macAddress;
    }

    /**
     * Returns the fixed IP information.
     *
     * @return fixed IP info
     */
    public Map<String, Ip4Address> fixedIps() {
        return fixedIps;
    }

    /**
     * Returns port ID.
     *
     * @return port ID
     */
    public String id() {
        return id;
    }

    /**
     * Returns security group information.
     *
     * @return security group info
     */
    public Collection<String> securityGroups() {
        return securityGroups;
    }

    /**
     * Returns device ID.
     *
     * @return device ID
     */
    public String deviceId() {
        return deviceId;
    }

    /**
     * OpenstackPort Builder class.
     */
    public static final class Builder {

        private PortStatus status;
        private String name;
        private Map<IpAddress, MacAddress> allowedAddressPairs;
        private boolean adminStateUp;
        private String networkId;
        private String tenantId;
        private String deviceOwner;
        private MacAddress macAddress;
        // list  of hash map <subnet id, ip address>
        private Map<String, Ip4Address> fixedIps;
        private String id;
        private Collection<String> securityGroups;
        private String deviceId;

        Builder() {
            fixedIps = Maps.newHashMap();
            allowedAddressPairs = Maps.newHashMap();
        }

        /**
         * Sets port status.
         *
         * @param status port status
         * @return Builder object
         */
        public Builder portStatus(PortStatus status) {
            this.status = status;

            return this;
        }

        /**
         * Sets port name.
         *
         * @param name port name
         * @return Builder object
         */
        public Builder name(String name) {
            this.name = name;

            return this;
        }

        /**
         * Sets allowed address pairs.
         *
         * @param addrPairs map of ip address and mac address
         * @return Builder object
         */
        public Builder allowedAddressPairs(Map<IpAddress, MacAddress> addrPairs) {
            this.allowedAddressPairs.putAll(addrPairs);
            return this;
        }

        /**
         * Sets whether admin state up or not.
         *
         * @param isAdminStateUp true if admin state is up, false otherwise
         * @return Builder object
         */
        public Builder adminState(boolean isAdminStateUp) {
            this.adminStateUp = isAdminStateUp;

            return this;
        }

        /**
         * Sets network ID.
         *
         * @param networkId network ID
         * @return Builder object
         */
        public Builder netwrokId(String networkId) {
            this.networkId = networkId;

            return this;
        }

        /**
         * Sets tenant ID.
         *
         * @param tenantId tenant ID
         * @return Builder object
         */
        public Builder tenantId(String tenantId) {
            this.tenantId = tenantId;

            return this;
        }

        /**
         * Sets device owner.
         *
         * @param owner device owner
         * @return Builder object
         */
        public Builder deviceOwner(String owner) {
            this.deviceOwner = owner;

            return this;
        }

        /**
         * Sets MAC address of the port.
         *
         * @param mac MAC address
         * @return Builder object
         */
        public Builder macAddress(MacAddress mac) {
            this.macAddress = mac;

            return this;
        }

        /**
         * Sets Fixed IP address information.
         *
         * @param fixedIpList Fixed IP info
         * @return Builder object
         */
        public Builder fixedIps(Map<String, Ip4Address> fixedIpList) {
            fixedIps.putAll(fixedIpList);

            return this;
        }

        /**
         * Sets ID of the port.
         *
         * @param id ID of the port
         * @return Builder object
         */
        public Builder id(String id) {
            this.id = id;

            return this;
        }

        /**
         * Sets security group of the port.
         *
         * @param securityGroupList security group list of the port
         * @return Builder object
         */
        public Builder securityGroup(Collection<String> securityGroupList) {
            this.securityGroups = securityGroupList;
            return this;
        }

        /**
         * Sets device ID of the port.
         *
         * @param deviceId device ID
         * @return Builder object
         */
        public Builder deviceId(String deviceId) {
            this.deviceId = deviceId;

            return this;
        }

        /**
         * Builds an OpenstackPort object.
         *
         * @return OpenstackPort objecet
         */
        public OpenstackPort build() {
            return new OpenstackPort(status, name, allowedAddressPairs, adminStateUp,
                                     networkId, networkId, deviceOwner, macAddress, fixedIps,
                                     id, securityGroups, deviceId);
        }
    }
}

