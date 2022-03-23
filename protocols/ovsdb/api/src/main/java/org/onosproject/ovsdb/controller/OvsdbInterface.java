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
package org.onosproject.ovsdb.controller;

import com.google.common.collect.Maps;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.behaviour.PatchDescription;
import org.onosproject.net.behaviour.TunnelDescription;
import org.onosproject.ovsdb.rfc.table.Interface.InterfaceColumn;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.google.common.base.MoreObjects.toStringHelper;
import static org.onosproject.ovsdb.controller.OvsdbConstant.PATCH_PEER;
import static org.onosproject.ovsdb.controller.OvsdbConstant.TUNNEL_KEY;
import static org.onosproject.ovsdb.controller.OvsdbConstant.TUNNEL_LOCAL_IP;
import static org.onosproject.ovsdb.controller.OvsdbConstant.TUNNEL_REMOTE_IP;

/**
 * The class representing an OVSDB interface.
 * This class is immutable.
 */
public final class OvsdbInterface {

    public enum Type {
        /**
         * An ordinary network device, e.g. eth0 on Linux.
         */
        SYSTEM,
        /**
         * A simulated network device that sends and receives traffic.
         */
        INTERNAL,
        /**
         * A TUN/TAP device managed by Open vSwitch.
         */
        TAP,
        /**
         * An Ethernet over RFC 2890 Generic Routing Encapsulation over IPv4 IPsec tunnel.
         */
        GRE,
        /**
         * An Ethernet over draft-ietf-nvo3-geneve-08 Generic Network Virtualization Encapsulation tunnel.
         */
        GENEVE,
        /**
         * An Ethernet tunnel over the experimental, UDP-based VXLAN protocol.
         */
        VXLAN,
        /**
         * An Ethernet over draft-davie-stt-06 Stateless Transport Tunneling Protocol for Network Virtualization.
         */
        STT,
        /**
         * A pair of virtual devices that act as a patch cable.
         */
        PATCH,
        /**
         * A DPDK net device.
         */
        DPDK
    }

    private final String name;
    private final Type type;
    private final Optional<Long> mtu;
    private final Map<InterfaceColumn, Map<String, String>> data;

    /* Adds more configs */

    /* Fields start with "options:" prefix defined in the OVSDB */
    private final Map<String, String> options;

    private OvsdbInterface(String name, Type type, Optional<Long> mtu,
                           Map<String, String> options, Map<InterfaceColumn,
                            Map<String, String>> data) {
        this.name = name;
        this.type = type;
        this.mtu = mtu;
        this.options = Maps.newHashMap(options);
        this.data = data;
    }

    /**
     * Returns name of the interface.
     *
     * @return interface name
     */
    public String name() {
        return name;
    }

    /**
     * Returns type of the interface.
     *
     * @return interface type
     */
    public Type type() {
        return type;
    }

    /**
     * Returns type of the interface with lowercase string.
     *
     * @return interface type string
     */
    public String typeToString() {
        return type.name().toLowerCase();
    }

    /**
     * Returns mtu of the interface.
     *
     * @return interface mtu
     */
    public Optional<Long> mtu() {
        return mtu;
    }

    /**
     * Returns optional configs of the interface.
     *
     * @return interface options
     */
    public Map<String, String> options() {
        return options;
    }

    /**
     * Returns the data of the interface.
     *
     * @return interface data
     */
    public Map<InterfaceColumn, Map<String, String>> data() {
        return data;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof OvsdbInterface) {
            final OvsdbInterface otherOvsdbInterface = (OvsdbInterface) obj;
            return Objects.equals(this.name, otherOvsdbInterface.name);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("name", name)
                .add("type", type)
                .add("mtu", mtu)
                .add("options", options)
                .add("data", data)
                .toString();
    }

    /**
     * Returns new OVSDB interface builder.
     *
     * @return ovsdb interface builder
     */
    public static OvsdbInterface.Builder builder() {
        return new Builder();
    }

    /**
     * Returns new OVSDB interface builder with tunnel interface description.
     *
     * @param tunnelDesc tunnel interface description
     * @return ovsdb interface builder
     */
    public static OvsdbInterface.Builder builder(TunnelDescription tunnelDesc) {
        return new Builder(tunnelDesc);
    }

    /**
     * Returns new OVSDB interface builder with patch interface description.
     *
     * @param patchDesc patch interface description
     * @return ovsdb interface builder
     */
    public static OvsdbInterface.Builder builder(PatchDescription patchDesc) {
        return new Builder(patchDesc);
    }

    /**
     * Builder of OVSDB interface entities.
     */
    public static final class Builder {
        private String name;
        private Type type;
        private Optional<Long> mtu = Optional.empty();
        private Map<String, String> options = Maps.newHashMap();
        private Map<InterfaceColumn, Map<String, String>> data = Maps.newHashMap();

        private Builder() {
        }

        /**
         * Constructs a builder with a given tunnel interface description.
         *
         * @param tunnelDesc tunnel interface description
         */
        private Builder(TunnelDescription tunnelDesc) {
            this.name = tunnelDesc.ifaceName();
            this.type = Type.valueOf(tunnelDesc.type().name());

            Map<String, String> tunOptions = Maps.newHashMap();
            if (tunnelDesc.local().isPresent()) {
                tunOptions.put(TUNNEL_LOCAL_IP, tunnelDesc.local().get().strValue());
            }
            if (tunnelDesc.remote().isPresent()) {
                tunOptions.put(TUNNEL_REMOTE_IP, tunnelDesc.remote().get().strValue());
            }
            if (tunnelDesc.key().isPresent()) {
                tunOptions.put(TUNNEL_KEY, tunnelDesc.key().get().strValue());
            }

            // set other configurations if there are any
            tunOptions.putAll(((DefaultAnnotations) tunnelDesc.annotations()).asMap());
            options = tunOptions;
        }

        /**
         * Constructs a builder with a given patch interface description.
         *
         * @param patchDesc patch interface description
         */
        private Builder(PatchDescription patchDesc) {
            this.name = patchDesc.ifaceName();
            this.type = Type.PATCH;

            Map<String, String> patchOptions = Maps.newHashMap();
            patchOptions.put(PATCH_PEER, patchDesc.peer());
            options = patchOptions;
        }

        /**
         * Returns new OVSDB interface.
         *
         * @return ovsdb interface
         */
        public OvsdbInterface build() {
            return new OvsdbInterface(name, type, mtu, options, data);
        }

        /**
         * Returns OVSDB interface builder with a given name.
         *
         * @param name name of the interface
         * @return ovsdb interface builder
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Returns OVSDB interface builder with a given interface type.
         *
         * @param type type of the interface
         * @return ovsdb interface builder
         */
        public Builder type(Type type) {
            this.type = type;
            return this;
        }

        /**
         * Returns OVSDB interface builder with a given interface mtu.
         *
         * @param mtu mtu of the interface
         * @return ovsdb interface builder
         */
        public Builder mtu(Long mtu) {
            this.mtu = Optional.ofNullable(mtu);
            return this;
        }

        /**
         * Returns OVSDB interface builder with given options.
         *
         * @param options map of options
         * @return ovsdb interface builder
         */
        public Builder options(Map<String, String> options) {
            this.options = Maps.newHashMap(options);
            return this;
        }

        /**
         * Returns OVSDB interface builder with given data.
         *
         * @param data map of data
         * @return ovsdb interface builder
         */
        public Builder data(Map<InterfaceColumn, Map<String, String>> data) {
            this.data = data;
            return this;
        }
    }
}
