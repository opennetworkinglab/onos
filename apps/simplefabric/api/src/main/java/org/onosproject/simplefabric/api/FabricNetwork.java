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

package org.onosproject.simplefabric.api;

import org.onlab.packet.VlanId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.EncapsulationType;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.intf.Interface;

import java.util.Set;

/**
 * Interface of fabric network.
 */
public interface FabricNetwork {

    /**
     * Gets DefaultFabricNetwork name.
     *
     * @return the name of DefaultFabricNetwork
     */
    String name();

    /**
     * Gets DefaultFabricNetwork interfaceNames.
     *
     * @return the interfaceNames of DefaultFabricNetwork
     */
    Set<String> interfaceNames();

    /**
     * Gets DefaultFabricNetwork encapsulation type.
     *
     * @return the encapsulation type of DefaultFabricNetwork
     */
    EncapsulationType encapsulation();

    /**
     * Gets DefaultFabricNetwork forward flag.
     *
     * @return the forward flag of DefaultFabricNetwork
     */
    boolean isForward();

    /**
     * Gets DefaultFabricNetwork broadcast flag.
     *
     * @return the broadcast flag of DefaultFabricNetwork
     */
    boolean isBroadcast();

    /**
     * Gets DefaultFabricNetwork interfaces.
     *
     * @return the interfaces of DefaultFabricNetwork
     */
    Set<Interface> interfaces();

    /**
     * Gets DefaultFabricNetwork hosts.
     *
     * @return the hosts of DefaultFabricNetwork
     */
    Set<HostId> hostIds();

    /**
     * Gets DefaultFabricNetwork isDirty flag.
     *
     * @return the isDirty flag of DefaultFabricNetwork
     */
    boolean isDirty();

    /**
     * Checks if the interface is of DefaultFabricNetwork.
     *
     * @param iface the interface to be checked
     * @return true if DefaultFabricNetwork contains the interface
     */
    boolean contains(Interface iface);

    /**
     * Checks if the ConnectPoint and Vlan is of DefaultFabricNetwork.
     *
     * @param port the ConnectPoint to be checked
     * @param vlanId the VlanId of the ConnectPoint to be checked
     * @return true if DefaultFabricNetwork contains the interface of the ConnnectPoint and VlanId
     */
    boolean contains(ConnectPoint port, VlanId vlanId);

    /**
     * Checks if the DeviceId is of DefaultFabricNetwork.
     *
     * @param deviceId the DeviceId to be checked
     * @return true if DefaultFabricNetwork contains any interface of the DeviceId
     */
    boolean contains(DeviceId deviceId);

    /**
     * Adds interface to DefaultFabricNetwork.
     *
     * @param iface the Interface to be added
     */
    void addInterface(Interface iface);

    /**
     * Adds host to DefaultFabricNetwork.
     *
     * @param host the Host to be added
     */
    void addHost(Host host);

    /**
     * Sets DefaultFabricNetwork isDirty flag.
     *
     * @param newDirty the isDirty flag to be set
     */
    void setDirty(boolean newDirty);

    /**
     * Builder of FabricNetwork.
     */
    interface Builder {

        /**
         * Returns FabricNetwork builder with supplied network name.
         *
         * @param name network name
         * @return FabricNetwork instance builder
         */
        Builder name(String name);

        /**
         * Returns FabricNetwork builder with supplied interface names.
         *
         * @param interfaceNames interface names
         * @return FabricNetwork instance builder
         */
        Builder interfaceNames(Set<String> interfaceNames);

        /**
         * Returns FabricNetwork builder with supplied encapsulation type.
         *
         * @param encapsulation encapsulation type
         * @return FabricNetwork instance builder
         */
        Builder encapsulation(EncapsulationType encapsulation);

        /**
         * Returns FabricNetwork builder with supplied forward flag.
         *
         * @param forward forward flag
         * @return FabricNetwork instance builder
         */
        Builder forward(boolean forward);

        /**
         * Returns FabricNetwork builder with supplied broadcast flag.
         *
         * @param broadcast broadcast flag
         * @return FabricNetwork instance builder
         */
        Builder broadcast(boolean broadcast);

        /**
         * Builds an immutable FabricNetwork instance.
         *
         * @return FabricNetwork instance
         */
        FabricNetwork build();
    }
}
