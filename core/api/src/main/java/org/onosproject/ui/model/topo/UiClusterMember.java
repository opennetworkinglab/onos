/*
 *  Copyright 2016-present Open Networking Laboratory
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.onosproject.ui.model.topo;

import org.onlab.packet.IpAddress;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.NodeId;
import org.onosproject.net.DeviceId;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.onosproject.cluster.ControllerNode.State.INACTIVE;

/**
 * Represents an individual member of the cluster (ONOS instance).
 */
public class UiClusterMember extends UiElement {

    private final UiTopology topology;
    private final ControllerNode cnode;

    private ControllerNode.State state = INACTIVE;
    private final Set<DeviceId> mastership = new HashSet<>();

    /**
     * Constructs a UI cluster member, with a reference to the parent
     * topology instance and the specified controller node instance.
     *
     * @param topology parent topology containing this cluster member
     * @param cnode    underlying controller node
     */
    public UiClusterMember(UiTopology topology, ControllerNode cnode) {
        this.topology = topology;
        this.cnode = cnode;
    }

    @Override
    public String toString() {
        return "UiClusterMember{" + cnode +
                ", online=" + isOnline() +
                ", ready=" + isReady() +
                ", #devices=" + deviceCount() +
                "}";
    }

    @Override
    public String idAsString() {
        return id().toString();
    }

    /**
     * Returns the controller node instance backing this UI cluster member.
     *
     * @return the backing controller node instance
     */
    public ControllerNode backingNode() {
        return cnode;
    }

    /**
     * Sets the state of this cluster member.
     *
     * @param state the state
     */
    public void setState(ControllerNode.State state) {
        this.state = state;
    }

    /**
     * Sets the collection of identities of devices for which this
     * controller node is master.
     *
     * @param mastership device IDs
     */
    public void setMastership(Set<DeviceId> mastership) {
        this.mastership.clear();
        this.mastership.addAll(mastership);
    }

    /**
     * Returns the identity of the cluster member.
     *
     * @return member identifier
     */
    public NodeId id() {
        return cnode.id();
    }

    /**
     * Returns the IP address of the cluster member.
     *
     * @return the IP address
     */
    public IpAddress ip() {
        return cnode.ip();
    }

    /**
     * Returns true if this cluster member is online (active).
     *
     * @return true if online, false otherwise
     */
    public boolean isOnline() {
        return state.isActive();
    }

    /**
     * Returns true if this cluster member is considered ready.
     *
     * @return true if ready, false otherwise
     */
    public boolean isReady() {
        return state.isReady();
    }

    /**
     * Returns the number of devices for which this cluster member is master.
     *
     * @return number of devices for which this member is master
     */
    public int deviceCount() {
        return mastership.size();
    }

    /**
     * Returns the list of devices for which this cluster member is master.
     *
     * @return list of devices for which this member is master
     */
    public Set<DeviceId> masterOf() {
        return Collections.unmodifiableSet(mastership);
    }

    /**
     * Returns true if the specified device is one for which this cluster
     * member is master.
     *
     * @param deviceId device ID
     * @return true if this cluster member is master for the given device
     */
    public boolean masterOf(DeviceId deviceId) {
        return mastership.contains(deviceId);
    }
}
