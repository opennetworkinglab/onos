/*
 *  Copyright 2016 Open Networking Laboratory
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

package org.onosproject.ui.impl.topo.model;

import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.NodeId;
import org.onosproject.cluster.RoleInfo;
import org.onosproject.event.EventDispatcher;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.Link;
import org.onosproject.net.region.Region;
import org.onosproject.ui.model.ServiceBundle;
import org.onosproject.ui.model.topo.UiClusterMember;
import org.onosproject.ui.model.topo.UiDevice;
import org.onosproject.ui.model.topo.UiTopology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.onosproject.ui.impl.topo.model.UiModelEvent.Type.CLUSTER_MEMBER_ADDED_OR_UPDATED;
import static org.onosproject.ui.impl.topo.model.UiModelEvent.Type.CLUSTER_MEMBER_REMOVED;
import static org.onosproject.ui.impl.topo.model.UiModelEvent.Type.DEVICE_ADDED_OR_UPDATED;
import static org.onosproject.ui.impl.topo.model.UiModelEvent.Type.DEVICE_REMOVED;

/**
 * UI Topology Model cache.
 */
class ModelCache {

    private static final Logger log = LoggerFactory.getLogger(ModelCache.class);

    private final ServiceBundle services;
    private final EventDispatcher dispatcher;
    private final UiTopology uiTopology = new UiTopology();

    ModelCache(ServiceBundle services, EventDispatcher eventDispatcher) {
        this.services = services;
        this.dispatcher = eventDispatcher;
    }

    @Override
    public String toString() {
        return "ModelCache{" + uiTopology + "}";
    }

    /**
     * Clear our model.
     */
    void clear() {
        uiTopology.clear();
    }

    /**
     * Create our internal model of the global topology.
     */
    void load() {
        // TODO - implement loading of initial state
//        loadClusterMembers();
//        loadRegions();
//        loadDevices();
//        loadHosts();
//        loadLinks();
    }


    /**
     * Updates the model (adds a new instance if necessary) with the given
     * controller node information.
     *
     * @param cnode controller node to be added/updated
     */
    void addOrUpdateClusterMember(ControllerNode cnode) {
        NodeId id = cnode.id();
        UiClusterMember member = uiTopology.findClusterMember(id);
        if (member == null) {
            member = new UiClusterMember(cnode);
            uiTopology.add(member);
        }

        // inject computed data about the cluster node, into the model object
        ControllerNode.State state = services.cluster().getState(id);
        member.setState(state);
        member.setDeviceCount(services.mastership().getDevicesOf(id).size());
        // NOTE: UI-attached is session-based data, not global

        dispatcher.post(new UiModelEvent(CLUSTER_MEMBER_ADDED_OR_UPDATED, member));
    }

    /**
     * Removes from the model the specified controller node.
     *
     * @param cnode controller node to be removed
     */
    void removeClusterMember(ControllerNode cnode) {
        NodeId id = cnode.id();
        UiClusterMember member = uiTopology.findClusterMember(id);
        if (member != null) {
            uiTopology.remove(member);
            dispatcher.post(new UiModelEvent(CLUSTER_MEMBER_REMOVED, member));
        } else {
            log.warn("Tried to remove non-member cluster node {}", id);
        }
    }

    void updateMasterships(DeviceId deviceId, RoleInfo roleInfo) {
        // TODO: store the updated mastership information
        // TODO: post event
    }

    void addOrUpdateRegion(Region region) {
        // TODO: find or create region assoc. with parameter
        // TODO: post event
    }

    void removeRegion(Region region) {
        // TODO: find region assoc. with parameter; remove from model
        // TODO: post event
    }

    void addOrUpdateDevice(Device device) {
        // TODO: find or create device assoc. with parameter
        // FIXME
        UiDevice uiDevice = new UiDevice();

        // TODO: post the (correct) event
        dispatcher.post(new UiModelEvent(DEVICE_ADDED_OR_UPDATED, uiDevice));
    }

    void removeDevice(Device device) {
        // TODO: get UiDevice associated with the given parameter; remove from model
        // FIXME
        UiDevice uiDevice = new UiDevice();

        // TODO: post the (correct) event
        dispatcher.post(new UiModelEvent(DEVICE_REMOVED, uiDevice));

    }

    void addOrUpdateLink(Link link) {
        // TODO: find ui-link assoc. with parameter; create or update.
        // TODO: post event
    }

    void removeLink(Link link) {
        // TODO: find ui-link assoc. with parameter; update or remove.
        // TODO: post event
    }

    void addOrUpdateHost(Host host) {
        // TODO: find or create host assoc. with parameter
        // TODO: post event
    }

    void moveHost(Host host, Host prevHost) {
        // TODO: process host-move
        // TODO: post event
    }

    void removeHost(Host host) {
        // TODO: find host assoc. with parameter; remove from model
    }

    /**
     * Returns the number of members in the cluster.
     *
     * @return number of cluster members
     */
    public int clusterMemberCount() {
        return uiTopology.clusterMemberCount();
    }

    /**
     * Returns the number of regions configured in the topology.
     *
     * @return number of regions
     */
    public int regionCount() {
        return uiTopology.regionCount();
    }
}
