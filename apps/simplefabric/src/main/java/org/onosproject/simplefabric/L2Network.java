/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.simplefabric;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.onlab.packet.VlanId;
import org.onosproject.net.intf.Interface;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.EncapsulationType;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;


/**
 * Class stores a L2Network information.
 */
public final class L2Network {

    private String name;                  // also for network configuration
    private Set<String> interfaceNames;   // also for network configuration
    private EncapsulationType encapsulation;  // also for network configuration
    private boolean l2Forward;            // do l2Forward (default:true) or not
    private boolean l2Broadcast;          // do l2Broadcast (default:true) or not

    /* status variables */
    private Set<Interface> interfaces;    // available interfaces from interfaceNames
    private Set<HostId> hostIds;          // available hosts from interfaces
    private boolean dirty;

    /**
     * Constructs a L2Network data for Config value.
     *
     * @param name the given name
     * @param ifaceNames the interface names
     * @param encapsulation the encapsulation type
     * @param l2Forward flag for l2Forward intents to be installed or not
     * @param l2Broadcast flag for l2Broadcast intents to be installed or not
     */
    L2Network(String name, Collection<String> ifaceNames, EncapsulationType encapsulation,
              boolean l2Forward, boolean l2Broadcast) {
        this.name = name;
        this.interfaceNames = Sets.newHashSet();
        this.interfaceNames.addAll(ifaceNames);
        this.encapsulation = encapsulation;
        this.l2Forward = (SimpleFabricService.ALLOW_ETH_ADDRESS_SELECTOR) ? l2Forward : false;
        this.l2Broadcast = (SimpleFabricService.ALLOW_ETH_ADDRESS_SELECTOR) ? l2Broadcast : false;
        this.interfaces = Sets.newHashSet();
        this.hostIds = Sets.newHashSet();
        this.dirty = false;
    }

    /**
     * Constructs a L2Network data by given name and encapsulation type.
     *
     * @param name the given name
     * @param encapsulation the encapsulation type
     */
    private L2Network(String name, EncapsulationType encapsulation) {
        this.name = name;
        this.interfaceNames = Sets.newHashSet();
        this.encapsulation = encapsulation;
        this.l2Forward = (SimpleFabricService.ALLOW_ETH_ADDRESS_SELECTOR) ? true : false;
        this.l2Broadcast = (SimpleFabricService.ALLOW_ETH_ADDRESS_SELECTOR) ? true : false;
        this.interfaces = Sets.newHashSet();
        this.hostIds = Sets.newHashSet();
        this.dirty = false;
    }

    /**
     * Creates a L2Network data by given name.
     * The encapsulation type of the L2Network will be NONE.
     *
     * @param name the given name
     * @return the L2Network data
     */
    public static L2Network of(String name) {
        Objects.requireNonNull(name);
        return new L2Network(name, EncapsulationType.NONE);
    }

    /**
     * Creates a copy of L2Network data.
     *
     * @param l2Network the L2Network data
     * @return the copy of the L2Network data
     */
    public static L2Network of(L2Network l2Network) {
        Objects.requireNonNull(l2Network);
        L2Network l2NetworkCopy = new L2Network(l2Network.name(), l2Network.encapsulation());
        l2NetworkCopy.interfaceNames.addAll(l2Network.interfaceNames());
        l2NetworkCopy.l2Forward = (SimpleFabricService.ALLOW_ETH_ADDRESS_SELECTOR) ? l2Network.l2Forward() : false;
        l2NetworkCopy.l2Broadcast = (SimpleFabricService.ALLOW_ETH_ADDRESS_SELECTOR) ? l2Network.l2Broadcast() : false;
        l2NetworkCopy.interfaces.addAll(l2Network.interfaces());
        l2NetworkCopy.hostIds.addAll(l2Network.hostIds());
        l2NetworkCopy.setDirty(l2Network.dirty());
        return l2NetworkCopy;
    }

    // field queries

    /**
     * Gets L2Network name.
     *
     * @return the name of L2Network
     */
    public String name() {
        return name;
    }

    /**
     * Gets L2Network interfaceNames.
     *
     * @return the interfaceNames of L2Network
     */
    public Set<String> interfaceNames() {
        return ImmutableSet.copyOf(interfaceNames);
    }

    /**
     * Gets L2Network encapsulation type.
     *
     * @return the encapsulation type of L2Network
     */
    public EncapsulationType encapsulation() {
        return encapsulation;
    }

    /**
     * Gets L2Network l2Forward flag.
     *
     * @return the l2Forward flag of L2Network
     */
    public boolean l2Forward() {
        return l2Forward;
    }

    /**
     * Gets L2Network l2Broadcast flag.
     *
     * @return the l2Broadcast flag of L2Network
     */
    public boolean l2Broadcast() {
        return l2Broadcast;
    }

    /**
     * Gets L2Network interfaces.
     *
     * @return the interfaces of L2Network
     */
    public Set<Interface> interfaces() {
        return ImmutableSet.copyOf(interfaces);
    }

    /**
     * Gets L2Network hosts.
     *
     * @return the hosts of L2Network
     */
    public Set<HostId> hostIds() {
        return ImmutableSet.copyOf(hostIds);
    }

    /**
     * Gets L2Network dirty flag.
     *
     * @return the dirty flag of L2Network
     */
    public boolean dirty() {
        return dirty;
    }

    /**
     * Checks if the interface is of L2Network.
     *
     * @param iface the interface to be checked
     * @return true if L2Network contains the interface
     */
    public boolean contains(Interface iface) {
        return interfaces.contains(iface);
    }

    /**
     * Checks if the ConnectPoint and Vlan is of L2Network.
     *
     * @param port the ConnectPoint to be checked
     * @param vlanId the VlanId of the ConnectPoint to be checked
     * @return true if L2Network contains the interface of the ConnnectPoint and VlanId
     */
    public boolean contains(ConnectPoint port, VlanId vlanId) {
        for (Interface iface : interfaces) {
            if (iface.connectPoint().equals(port) && iface.vlan().equals(vlanId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the DeviceId is of L2Network.
     *
     * @param deviceId the DeviceId to be checked
     * @return true if L2Network contains any interface of the DeviceId
     */
    public boolean contains(DeviceId deviceId) {
        for (Interface iface : interfaces) {
            if (iface.connectPoint().deviceId().equals(deviceId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Adds interface to L2Network.
     *
     * @param iface the Interface to be added
     */
    public void addInterface(Interface iface) {
        Objects.requireNonNull(iface);
        if (interfaces.add(iface)) {
            setDirty(true);
        }
    }

    /**
     * Adds host to L2Network.
     *
     * @param host the Host to be added
     */
    public void addHost(Host host) {
        Objects.requireNonNull(host);
        if (hostIds.add(host.id())) {
            setDirty(true);
        }
    }

    /**
     * Sets L2Network dirty flag.
     *
     * @param newDirty the dirty flag to be set
     */
    public void setDirty(boolean newDirty) {
        dirty = newDirty;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("name", name)
                .add("interfaceNames", interfaceNames)
                .add("encapsulation", encapsulation)
                .add("l2Forward", l2Forward)
                .add("l2Broadcast", l2Broadcast)
                .add("interfaces", interfaces)
                .add("hostIds", hostIds)
                .add("dirty", dirty)
                .toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof L2Network)) {
            return false;
        }
        L2Network other = (L2Network) obj;
        return Objects.equals(other.name, this.name)
               && Objects.equals(other.interfaceNames, this.interfaceNames)
               && Objects.equals(other.encapsulation, this.encapsulation)
               && Objects.equals(other.l2Forward, this.l2Forward)
               && Objects.equals(other.l2Broadcast, this.l2Broadcast)
               && Objects.equals(other.interfaces, this.interfaces)
               && Objects.equals(other.hostIds, this.hostIds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, interfaces, encapsulation, l2Forward, l2Broadcast);
    }
}
