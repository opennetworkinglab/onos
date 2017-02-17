/*
 * Copyright 2017-present Open Networking Laboratory
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

import org.joda.time.LocalDateTime;
import org.onosproject.event.AbstractEvent;
import org.openstack4j.model.network.ExternalGateway;
import org.openstack4j.model.network.NetFloatingIP;
import org.openstack4j.model.network.Router;
import org.openstack4j.model.network.RouterInterface;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Describes OpenStack router service events.
 */
public class OpenstackRouterEvent extends AbstractEvent<OpenstackRouterEvent.Type, Router> {

    private final ExternalGateway exGateway;
    private final RouterInterface routerIface;
    private final NetFloatingIP floatingIP;
    private final String portId;

    public enum Type {

        /**
         * Signifies that a new OpenStack router is created.
         */
        OPENSTACK_ROUTER_CREATED,

        /**
         * Signifies that the OpenStack router is updated.
         */
        OPENSTACK_ROUTER_UPDATED,

        /**
         * Signifies that the OpenStack router is removed.
         */
        OPENSTACK_ROUTER_REMOVED,

        /**
         * Signifies that the external gateway is added to the router.
         */
        OPENSTACK_ROUTER_GATEWAY_ADDED,

        /**
         * Signifies that the external gateway is removed from the router.
         */
        OPENSTACK_ROUTER_GATEWAY_REMOVED,

        /**
         * Signifies that the OpenStack router interface is added.
         */
        OPENSTACK_ROUTER_INTERFACE_ADDED,

        /**
         * Signifies that the OpenStack router interface is updated.
         */
        OPENSTACK_ROUTER_INTERFACE_UPDATED,

        /**
         * Signifies that the OpenStack router interface is removed.
         */
        OPENSTACK_ROUTER_INTERFACE_REMOVED,

        /**
         * Signifies that a new floating IP is created.
         */
        OPENSTACK_FLOATING_IP_CREATED,

        /**
         * Signifies that the floating IP is updated.
         */
        OPENSTACK_FLOATING_IP_UPDATED,

        /**
         * Signifies that the floating IP is removed.
         */
        OPENSTACK_FLOATING_IP_REMOVED,

        /**
         * Signifies that the floating IP is associated to a fixed IP.
         */
        OPENSTACK_FLOATING_IP_ASSOCIATED,

        /**
         * Signifies that the floating IP disassociated from the fixed IP.
         */
        OPENSTACK_FLOATING_IP_DISASSOCIATED
    }

    /**
     * Creates an event of a given type for the specified router and the current time.
     *
     * @param type   openstack router event type
     * @param osRouter openstack router
     */
    public OpenstackRouterEvent(Type type, Router osRouter) {
        super(type, osRouter);
        this.exGateway = null;
        this.routerIface = null;
        this.floatingIP = null;
        this.portId = null;
    }

    /**
     * Creates an event of a given type for the specified router, external gateway and
     * the current time.
     *
     * @param type      openstack router event type
     * @param osRouter  openstack router
     * @param exGateway openstack router external gateway
     */
    public OpenstackRouterEvent(Type type, Router osRouter, ExternalGateway exGateway) {
        super(type, osRouter);
        this.exGateway = exGateway;
        this.routerIface = null;
        this.floatingIP = null;
        this.portId = null;
    }

    /**
     * Creates an event of a given type for the specified router, floating IP and
     * the current time.
     *
     * @param type          openstack router event type
     * @param osRouter      openstack router
     * @param osRouterIface openstack router interface
     */
    public OpenstackRouterEvent(Type type, Router osRouter, RouterInterface osRouterIface) {
        super(type, osRouter);
        this.exGateway = null;
        this.routerIface = osRouterIface;
        this.floatingIP = null;
        this.portId = null;
    }

    /**
     * Creates an event of a given type for the specified router, floating IP and
     * the current time.
     *
     * @param type       openstack router event type
     * @param router     openstack router
     * @param floatingIP openstack floating ip
     */
    public OpenstackRouterEvent(Type type, Router router, NetFloatingIP floatingIP) {
        super(type, router);
        this.exGateway = null;
        this.routerIface = null;
        this.floatingIP = floatingIP;
        this.portId = null;
    }

    /**
     * Creates an event of a given type for the specified router, floating IP,
     * associated OpenStack port ID and the current time.
     *
     * @param type       openstack router event type
     * @param router     openstack router
     * @param floatingIP openstack floating ip
     * @param portId     associated openstack port id
     */
    public OpenstackRouterEvent(Type type, Router router, NetFloatingIP floatingIP,
                                String portId) {
        super(type, router);
        this.exGateway = null;
        this.routerIface = null;
        this.floatingIP = floatingIP;
        this.portId = portId;
    }

    /**
     * Returns the router external gateway object of the router event.
     *
     * @return openstack router external gateway; null if the event is not
     * relevant to the router external gateway
     */
    public ExternalGateway externalGateway() {
        return exGateway;
    }

    /**
     * Returns the router interface object of the router event.
     *
     * @return openstack router interface; null if the event is not relevant to
     * the router interface
     */
    public RouterInterface routerIface() {
        return routerIface;
    }

    /**
     * Returns the floating IP of the router event.
     *
     * @return openstack floating ip; null if the event is not relevant to
     * the floating ip
     */
    public NetFloatingIP floatingIp() {
        return floatingIP;
    }

    /**
     * Returns the associated port ID of the floating IP.
     *
     * @return openstack port id; null if the event is not relevant to the
     * floating ip
     */
    public String portId() {
        return portId;
    }

    @Override
    public String toString() {
        if (floatingIP == null) {
            return super.toString();
        }
        return toStringHelper(this)
                .add("time", new LocalDateTime(time()))
                .add("type", type())
                .add("router", subject())
                .add("externalGateway", exGateway)
                .add("routerIface", routerIface)
                .add("floatingIp", floatingIP)
                .add("portId", portId)
                .toString();
    }
}
