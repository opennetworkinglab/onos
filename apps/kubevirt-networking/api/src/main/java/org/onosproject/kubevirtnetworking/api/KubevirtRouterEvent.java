/*
 * Copyright 2021-present Open Networking Foundation
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
package org.onosproject.kubevirtnetworking.api;

import org.onlab.packet.MacAddress;
import org.onosproject.event.AbstractEvent;

import java.util.Set;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Kubevirt router event class.
 */
public class KubevirtRouterEvent extends AbstractEvent<KubevirtRouterEvent.Type, KubevirtRouter> {

    private final KubevirtFloatingIp floatingIp;
    private final String podName;
    private final Set<String> internal;
    private final String externalIp;
    private final String externalNet;
    private final String peerRouterIp;
    private final String gateway;
    private final MacAddress peerRouterMac;

    /**
     * Creates an event of a given type for the specified kubevirt router.
     *
     * @param type      kubevirt router event type
     * @param subject   kubevirt router
     */
    public KubevirtRouterEvent(Type type, KubevirtRouter subject) {
        super(type, subject);
        this.floatingIp = null;
        this.podName = null;
        this.internal = null;
        this.externalIp = null;
        this.externalNet = null;
        this.peerRouterIp = null;
        this.gateway = null;
        this.peerRouterMac = null;
    }

    /**
     * Creates an event of a given type for the specified kubevirt router.
     *
     * @param type       kubevirt router event type
     * @param subject    kubevirt router
     * @param floatingIp kubevirt floating IP
     */
    public KubevirtRouterEvent(Type type, KubevirtRouter subject, KubevirtFloatingIp floatingIp) {
        super(type, subject);
        this.floatingIp = floatingIp;
        this.podName = null;
        this.internal = null;
        this.externalIp = null;
        this.externalNet = null;
        this.peerRouterIp = null;
        this.gateway = null;
        this.peerRouterMac = null;
    }

    /**
     * Creates an event of a given type for the specified kubevirt router.
     *
     * @param type       kubevirt router event type
     * @param subject    kubevirt router
     * @param floatingIp kubevirt floating IP
     * @param podName    kubevirt POD name
     */
    public KubevirtRouterEvent(Type type, KubevirtRouter subject, KubevirtFloatingIp floatingIp, String podName) {
        super(type, subject);
        this.floatingIp = floatingIp;
        this.podName = podName;
        this.internal = null;
        this.externalIp = null;
        this.externalNet = null;
        this.peerRouterIp = null;
        this.gateway = null;
        this.peerRouterMac = null;
    }

    /**
     * Creates an event of a given type for the specified kubevirt router.
     *
     * @param type        kubevirt router event type
     * @param subject     kubevirt router
     * @param internal    internal networks attached to the router
     */
    public KubevirtRouterEvent(Type type, KubevirtRouter subject, Set<String> internal) {
        super(type, subject);
        this.internal = internal;
        this.podName = null;
        this.floatingIp = null;
        this.externalIp = null;
        this.externalNet = null;
        this.peerRouterIp = null;
        this.gateway = null;
        this.peerRouterMac = null;
    }

    /**
     * Creates an event of a given type for the specified kubevirt router.
     *
     * @param type          kubevirt router event type
     * @param subject       kubevirt router
     * @param externalIp    virtual router's IP address included in external network
     * @param externalNet   external network name
     * @param peerRouterIp  external peer router IP address
     * @param peerRouterMac external peer router MAC address
     */
    public KubevirtRouterEvent(Type type, KubevirtRouter subject,
                               String externalIp, String externalNet,
                               String peerRouterIp, MacAddress peerRouterMac) {
        super(type, subject);
        this.internal = null;
        this.podName = null;
        this.floatingIp = null;
        this.externalIp = externalIp;
        this.externalNet = externalNet;
        this.peerRouterIp = peerRouterIp;
        this.gateway = null;
        this.peerRouterMac = peerRouterMac;
    }

    public KubevirtRouterEvent(Type type, KubevirtRouter subject,
                               String gateway) {
        super(type, subject);
        this.gateway = gateway;
        this.floatingIp = null;
        this.podName = null;
        this.internal = null;
        this.externalIp = null;
        this.externalNet = null;
        this.peerRouterIp = null;
        this.peerRouterMac = null;
    }

    public enum Type {
        /**
         * Signifies that a new kubevirt router is created.
         */
        KUBEVIRT_ROUTER_CREATED,

        /**
         * Signifies that the kubevirt router is updated.
         */
        KUBEVIRT_ROUTER_UPDATED,

        /**
         * Signifies that the kubevirt router is removed.
         */
        KUBEVIRT_ROUTER_REMOVED,

        /**
         * Signifies that a new external network is added to the router.
         */
        KUBEVIRT_ROUTER_EXTERNAL_NETWORK_ATTACHED,

        /**
         * Signifies that the existing external network is removed from the router.
         */
        KUBEVIRT_ROUTER_EXTERNAL_NETWORK_DETACHED,

        /**
         * Signifies that a new internal network is added to the router.
         */
        KUBEVIRT_ROUTER_INTERNAL_NETWORKS_ATTACHED,

        /**
         * Signifies that the existing internal network is removed from the router.
         */
        KUBEVIRT_ROUTER_INTERNAL_NETWORKS_DETACHED,

        /**
         * Signifies that a new kubevirt floating IP is created.
         */
        KUBEVIRT_FLOATING_IP_CREATED,

        /**
         * Signifies that the kubevirt floating IP is updated.
         */
        KUBEVIRT_FLOATING_IP_UPDATED,

        /**
         * Signifies that the kubevirt floating IP is removed.
         */
        KUBEVIRT_FLOATING_IP_REMOVED,

        /**
         * Signifies that the floating IP is associated to a fixed IP.
         */
        KUBEVIRT_FLOATING_IP_ASSOCIATED,

        /**
         * Signifies that the floating IP disassociated from the fixed IP.
         */
        KUBEVIRT_FLOATING_IP_DISASSOCIATED,

        /**
         * Signified that the gateway node associated for this router.
         */
        KUBEVIRT_GATEWAY_NODE_ATTACHED,
        /**
         * Signified that the gateway node disassociated for this router.
         */
        KUBEVIRT_GATEWAY_NODE_DETACHED,
        /**
         * Signified that the gateway node changed for this router.
         */
        KUBEVIRT_GATEWAY_NODE_CHANGED,
        /**
         * Signified that the snat status disabled for this router.
         */
        KUBEVIRT_SNAT_STATUS_DISABLED,
        /**
         * Signifies that the floating IP is associated to a lb VIP.
         */
        KUBEVIRT_FLOATING_IP_LB_ASSOCIATED,
        /**
         * Signifies that the floating IP is disassociated to a lb VIP.
         */
        KUBEVIRT_FLOATING_IP_LB_DISASSOCIATED,
        /**
         * Signifies the that peer router mac address is retrieved for this router.
         */
        KUBEVIRT_PEER_ROUTER_MAC_RETRIEVED,
    }

    /**
     * Returns the floating IP of the router event.
     *
     * @return kubevirt floating IP; null if the event is not relevant to the floating IP
     */
    public KubevirtFloatingIp floatingIp() {
        return floatingIp;
    }

    /**
     * Returns the pod name of the router event.
     *
     * @return kubevirt pod name; null if the event is not relevant to the pod name
     */
    public String podName() {
        return podName;
    }

    /**
     * Returns the internal of the router event.
     *
     * @return kubevirt internal network set, null if the event is not relevant to the internal
     */
    public Set<String> internal() {
        return internal;
    }

    /**
     * Returns the external IP address of the router event.
     *
     * @return external IP address, null if the event is not relevant to the external
     */
    public String externalIp() {
        return externalIp;
    }

    /**
     * Returns the external network of the router event.
     *
     * @return external network, null if the event is not relevant ot the external
     */
    public String externalNet() {
        return externalNet;
    }

    /**
     * Returns the gateway of the router event.
     *
     * @return gateway if exists, null otherwise
     */
    public String gateway() {
        return gateway;
    }

    /**
     * Returns the external peer router IP address.
     *
     * @return external peer router IP if exists, null otherwise
     */
    public String externalPeerRouterIp() {
        return peerRouterIp;
    }
    /**
     * Returns the external peer router MAC address.
     *
     * @return external peer router MAC if exists, null otherwise
     */
    public MacAddress peerRouterMac() {
        return peerRouterMac;
    }

    @Override
    public String toString() {
        if (floatingIp == null) {
            return super.toString();
        }
        return toStringHelper(this)
                .add("type", type())
                .add("router", subject())
                .add("floatingIp", floatingIp)
                .add("podName", podName)
                .add("internal", internal)
                .add("externalIp", externalIp)
                .add("externalNet", externalNet)
                .add("peerRouterIp", peerRouterIp)
                .add("gatewayNodeHostName", gateway)
                .toString();
    }
}
