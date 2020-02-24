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
package org.onosproject.openstacknetworking.api;

import org.onlab.util.Tools;
import org.onosproject.event.AbstractEvent;
import org.openstack4j.model.network.Network;
import org.openstack4j.model.network.Port;
import org.openstack4j.model.network.Subnet;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Describes OpenStack network service event.
 */
public class OpenstackNetworkEvent
        extends AbstractEvent<OpenstackNetworkEvent.Type, Network> {

    private final Port port;
    private final Subnet subnet;
    private final String securityGroupId;
    private final ExternalPeerRouter peerRouter;

    public enum Type {
        /**
         * Signifies that a new OpenStack network is created.
         */
        OPENSTACK_NETWORK_CREATED,

        /**
         * Signifies that the OpenStack network is updated.
         */
        OPENSTACK_NETWORK_UPDATED,

        /**
         * Signifies that the OpenStack network is pre-removed.
         */
        OPENSTACK_NETWORK_PRE_REMOVED,

        /**
         * Signifies that the OpenStack network is removed.
         */
        OPENSTACK_NETWORK_REMOVED,

        /**
         * Signifies that a new OpenStack subnet is created.
         */
        OPENSTACK_SUBNET_CREATED,

        /**
         * Signifies that the OpenStack subnet is updated.
         */
        OPENSTACK_SUBNET_UPDATED,

        /**
         * Signifies that the OpenStack subnet is removed.
         */
        OPENSTACK_SUBNET_REMOVED,

        /**
         * Signifies that a new OpenStack port is created.
         */
        OPENSTACK_PORT_CREATED,

        /**
         * Signifies that the OpenStack port will be updated.
         */
        OPENSTACK_PORT_PRE_UPDATE,

        /**
         * Signifies that the OpenStack port is updated.
         */
        OPENSTACK_PORT_UPDATED,

        /**
         * Signifies that the OpenStack port will be removed.
         */
        OPENSTACK_PORT_PRE_REMOVE,

        /**
         * Signifies that the OpenStack port is removed.
         */
        OPENSTACK_PORT_REMOVED,

        /**
         * Signifies that the OpenStack security group rule is added to a specific port.
         */
        OPENSTACK_PORT_SECURITY_GROUP_ADDED,

        /**
         * Signifies that the OpenStack security group rule is removed from a specific port.
         */
        OPENSTACK_PORT_SECURITY_GROUP_REMOVED,

        /**
         * Signifies that the external peer router is created.
         */
        EXTERNAL_PEER_ROUTER_CREATED,

        /**
         * Signifies that the external peer router is updated.
         */
        EXTERNAL_PEER_ROUTER_UPDATED,

        /**
         * Signifies that the external peer router MAC is updated.
         */
        EXTERNAL_PEER_ROUTER_MAC_UPDATED,

        /**
         * Signifies that the external peer router is removed.
         */
        EXTERNAL_PEER_ROUTER_REMOVED,
    }

    /**
     * Creates an event of a given type for the specified network and the current time.
     *
     * @param type    openstack network event type
     * @param network openstack network
     */
    public OpenstackNetworkEvent(Type type, Network network) {
        super(type, network);
        this.port = null;
        this.subnet = null;
        this.securityGroupId = null;
        this.peerRouter = null;
    }

    /**
     * Creates an event of a given type for the specified network, port and the
     * current time.
     *
     * @param type    openstack network event type
     * @param network openstack network
     * @param port    openstack port
     */
    public OpenstackNetworkEvent(Type type, Network network, Port port) {
        super(type, network);
        this.port = port;
        this.subnet = null;
        this.securityGroupId = null;
        this.peerRouter = null;
    }

    /**
     * Creates an event of a given type for the specified network, subnet and the
     * current time.
     *
     * @param type    openstack network event type
     * @param network openstack network
     * @param subnet  openstack subnet
     */
    public OpenstackNetworkEvent(Type type, Network network, Subnet subnet) {
        super(type, network);
        this.port = null;
        this.subnet = subnet;
        this.securityGroupId = null;
        this.peerRouter = null;
    }

    /**
     * Creates an event of a given type for the specified port and security groups.
     *
     * @param type openstack network event type
     * @param port openstack port
     * @param securityGroupId openstack security group
     */
    public OpenstackNetworkEvent(Type type, Port port, String securityGroupId) {
        super(type, null);
        this.port = port;
        this.subnet = null;
        this.securityGroupId = securityGroupId;
        this.peerRouter = null;
    }

    /**
     * Creates an event of a given type for the specified external peer router.
     *
     * @param type openstack network event type
     * @param peerRouter external peer router
     */
    public OpenstackNetworkEvent(Type type, ExternalPeerRouter peerRouter) {
        super(type, null);
        this.port = null;
        this.subnet = null;
        this.securityGroupId = null;
        this.peerRouter = peerRouter;
    }

    /**
     * Returns the port of the network event.
     *
     * @return openstack port; null if the event is not port specific
     */
    public Port port() {
        return port;
    }

    /**
     * Returns the subnet of the network event.
     *
     * @return openstack subnet; null if the event is not subnet specific
     */
    public Subnet subnet() {
        return subnet;
    }

    /**
     * Returns the external peer router.
     *
     * @return external peer router; null if the event is not peer router specific
     */
    public ExternalPeerRouter peerRouter() {
        return peerRouter;
    }

    /**
     * Returns the security group rule IDs updated.
     *
     * @return openstack security group
     */
    public String securityGroupId() {
        return securityGroupId;
    }

    @Override
    public String toString() {
        if (port == null && subnet == null) {
            return super.toString();
        }
        return toStringHelper(this)
                .add("time", Tools.defaultOffsetDataTime(time()))
                .add("type", type())
                .add("network", subject())
                .add("port", port)
                .add("subnet", subnet)
                .add("security group", securityGroupId())
                .add("external peer router", peerRouter)
                .toString();
    }
}
