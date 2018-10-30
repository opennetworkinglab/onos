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

package org.onosproject.simplefabric.impl;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.onlab.packet.VlanId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.EncapsulationType;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.intf.Interface;
import org.onosproject.simplefabric.api.FabricNetwork;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static org.onosproject.simplefabric.api.Constants.ALLOW_ETH_ADDRESS_SELECTOR;


/**
 * Class stores a DefaultFabricNetwork information.
 */
public final class DefaultFabricNetwork implements FabricNetwork {

    private static final String NOT_NULL_MSG = "FabricNetwork % cannot be null";

    private final String name;
    private final Set<String> interfaceNames;
    private final EncapsulationType encapsulation;
    private boolean forward;
    private boolean broadcast;

    /* status variables */
    private final Set<Interface> interfaces;
    private final Set<HostId> hostIds;
    private boolean dirty;

    /**
     * Constructs a DefaultFabricNetwork instance.
     *
     * @param name              fabric name name
     * @param interfaceNames    a collection of  interface names
     * @param encapsulation     encapsulation type
     * @param forward           flag for forward intents to be installed or not
     * @param broadcast         flag for broadcast intents to be installed or not
     */
    private DefaultFabricNetwork(String name, Collection<String> interfaceNames,
                                 EncapsulationType encapsulation,
                                 boolean forward, boolean broadcast) {
        this.name = name;
        this.interfaceNames = Sets.newHashSet();

        if (interfaceNames != null) {
            this.interfaceNames.addAll(interfaceNames);
        }

        this.encapsulation = encapsulation;
        this.forward = (ALLOW_ETH_ADDRESS_SELECTOR) && forward;
        this.broadcast = (ALLOW_ETH_ADDRESS_SELECTOR) && broadcast;
        this.interfaces = Sets.newHashSet();
        this.hostIds = Sets.newHashSet();
        this.dirty = false;
    }

    /**
     * Constructs a DefaultFabricNetwork instance.
     *
     * @param name              fabric network name
     * @param encapsulation     encapsulation type
     */
    private DefaultFabricNetwork(String name, EncapsulationType encapsulation) {
        this.name = name;
        this.interfaceNames = Sets.newHashSet();
        this.encapsulation = encapsulation;
        this.forward = ALLOW_ETH_ADDRESS_SELECTOR;
        this.broadcast = ALLOW_ETH_ADDRESS_SELECTOR;
        this.interfaces = Sets.newHashSet();
        this.hostIds = Sets.newHashSet();
        this.dirty = false;
    }

    /**
     * Creates a DefaultFabricNetwork data by given name.
     * The encapsulation type of the DefaultFabricNetwork will be NONE.
     *
     * @param name              fabric network name
     * @return DefaultFabricNetwork instance
     */
    public static FabricNetwork of(String name) {
        Objects.requireNonNull(name);
        return new DefaultFabricNetwork(name, EncapsulationType.NONE);
    }

    /**
     * Creates a copy of DefaultFabricNetwork instance.
     *
     * @param fabricNetwork DefaultFabricNetwork instance
     * @return the copy of the DefaultFabricNetwork instance
     */
    public static FabricNetwork of(FabricNetwork fabricNetwork) {
        Objects.requireNonNull(fabricNetwork);
        DefaultFabricNetwork fabricNetworkCopy =
                new DefaultFabricNetwork(fabricNetwork.name(), fabricNetwork.encapsulation());
        fabricNetworkCopy.interfaceNames.addAll(fabricNetwork.interfaceNames());
        fabricNetworkCopy.forward = (ALLOW_ETH_ADDRESS_SELECTOR) && fabricNetwork.isForward();
        fabricNetworkCopy.broadcast = (ALLOW_ETH_ADDRESS_SELECTOR) && fabricNetwork.isBroadcast();
        fabricNetworkCopy.interfaces.addAll(fabricNetwork.interfaces());
        fabricNetworkCopy.hostIds.addAll(fabricNetwork.hostIds());
        fabricNetworkCopy.setDirty(fabricNetwork.isDirty());
        return fabricNetworkCopy;
    }

    // field queries

    @Override
    public String name() {
        return name;
    }

    @Override
    public Set<String> interfaceNames() {
        return ImmutableSet.copyOf(interfaceNames);
    }

    @Override
    public EncapsulationType encapsulation() {
        return encapsulation;
    }

    @Override
    public boolean isForward() {
        return forward;
    }

    @Override
    public boolean isBroadcast() {
        return broadcast;
    }

    @Override
    public Set<Interface> interfaces() {
        return ImmutableSet.copyOf(interfaces);
    }

    @Override
    public Set<HostId> hostIds() {
        return ImmutableSet.copyOf(hostIds);
    }

    @Override
    public boolean isDirty() {
        return dirty;
    }

    @Override
    public boolean contains(Interface iface) {
        return interfaces.contains(iface);
    }

    @Override
    public boolean contains(ConnectPoint port, VlanId vlanId) {
        for (Interface iface : interfaces) {
            if (iface.connectPoint().equals(port) && iface.vlan().equals(vlanId)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean contains(DeviceId deviceId) {
        for (Interface iface : interfaces) {
            if (iface.connectPoint().deviceId().equals(deviceId)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void addInterface(Interface iface) {
        Objects.requireNonNull(iface);
        if (interfaces.add(iface)) {
            setDirty(true);
        }
    }

    @Override
    public void addHost(Host host) {
        Objects.requireNonNull(host);
        if (hostIds.add(host.id())) {
            setDirty(true);
        }
    }

    @Override
    public void setDirty(boolean newDirty) {
        dirty = newDirty;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("name", name)
                .add("interfaceNames", interfaceNames)
                .add("encapsulation", encapsulation)
                .add("forward", forward)
                .add("broadcast", broadcast)
                .add("interfaces", interfaces)
                .add("hostIds", hostIds)
                .add("isDirty", dirty)
                .toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof DefaultFabricNetwork)) {
            return false;
        }
        DefaultFabricNetwork other = (DefaultFabricNetwork) obj;
        return Objects.equals(other.name, this.name)
                && Objects.equals(other.interfaceNames, this.interfaceNames)
                && Objects.equals(other.encapsulation, this.encapsulation)
                && Objects.equals(other.forward, this.forward)
                && Objects.equals(other.broadcast, this.broadcast)
                && Objects.equals(other.interfaces, this.interfaces)
                && Objects.equals(other.hostIds, this.hostIds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, interfaces, encapsulation, forward, broadcast);
    }

    /**
     * Returns new builder instance.
     *
     * @return fabric network builder
     */
    public static DefaultFabricNetworkBuilder builder() {
        return new DefaultFabricNetworkBuilder();
    }

    /**
     * A builder class for fabric network.
     */
    public static final class DefaultFabricNetworkBuilder implements Builder {
        private String name;
        private Set<String> interfaceNames;
        private EncapsulationType encapsulation;
        private boolean forward;
        private boolean broadcast;

        private DefaultFabricNetworkBuilder() {
        }

        @Override
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        @Override
        public Builder interfaceNames(Set<String> interfaceNames) {
            this.interfaceNames = interfaceNames;
            return this;
        }

        @Override
        public Builder encapsulation(EncapsulationType encapsulation) {
            this.encapsulation = encapsulation;
            return this;
        }

        @Override
        public Builder forward(boolean forward) {
            this.forward = forward;
            return this;
        }

        @Override
        public Builder broadcast(boolean broadcast) {
            this.broadcast = broadcast;
            return this;
        }

        @Override
        public FabricNetwork build() {
            checkArgument(name != null, NOT_NULL_MSG, "name");
            return new DefaultFabricNetwork(name, interfaceNames,
                    encapsulation, forward, broadcast);
        }
    }
}