/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.openstackswitching;

import com.google.common.collect.Lists;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.MacAddress;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

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
    // FIX_ME
    private String allowedAddressPairs;
    private boolean adminStateUp;
    private String networkId;
    private String tenantId;
    private String deviceOwner;
    private MacAddress macAddress;
    // <subnet id, ip address>
    private HashMap<String, Ip4Address> fixedIps;
    private String id;
    private List<String> securityGroups;
    private String deviceId;

    private OpenstackPort(PortStatus status, String name, boolean adminStateUp,
                          String networkId, String tenantId, String deviceOwner,
                          MacAddress macAddress, HashMap fixedIps, String id,
                          List<String> securityGroups, String deviceId) {

        this.status = status;
        this.name = name;
        this.adminStateUp = adminStateUp;
        this.networkId = checkNotNull(networkId);
        this.tenantId = checkNotNull(tenantId);
        this.deviceOwner = deviceOwner;
        this.macAddress = checkNotNull(macAddress);
        this.fixedIps = checkNotNull(fixedIps);
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
    public HashMap fixedIps() {
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
    public List<String> securityGroups() {
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

    // TODO : Implement the following functions when necessary
    //@Override
    //public void equals(Object that) {
    //
    //}
    //
    //@Override
    //public int hashCode() {
    //
    //}

    @Override
    public Object clone() {
        OpenstackPort op = new OpenstackPort(this.status, this.name, this.adminStateUp,
                this.networkId, this.tenantId, this.deviceOwner, this.macAddress,
                (HashMap) this.fixedIps.clone(), this.id,
                Collections.unmodifiableList(this.securityGroups), this.deviceId);

        return op;
    }

    /**
     * OpenstackPort Builder class.
     */
    public static final class Builder {

        private PortStatus status;
        private String name;
        // FIX_ME
        private String allowedAddressPairs;
        private boolean adminStateUp;
        private String networkId;
        private String tenantId;
        private String deviceOwner;
        private MacAddress macAddress;
        // list  of hash map <subnet id, ip address>
        private HashMap<String, Ip4Address> fixedIps;
        private String id;
        private List<String> securityGroups;
        private String deviceId;

        Builder() {
            fixedIps = new HashMap<>();
            securityGroups = Lists.newArrayList();
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
        public Builder fixedIps(HashMap<String, Ip4Address> fixedIpList) {
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
         * @param securityGroup security group of the port
         * @return Builder object
         */
        public Builder securityGroup(String securityGroup) {
            securityGroups.add(securityGroup);

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
            return new OpenstackPort(status, name, adminStateUp, networkId, networkId,
                    deviceOwner, macAddress, fixedIps, id, securityGroups, deviceId);
        }
    }
}

