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
    }

    /**
     * Creates an event of a given type for the specified kubevirt router.
     *
     * @param type          kubevirt router event type
     * @param subject       kubevirt router
     * @param externalIp    virtual router's IP address included in external network
     * @param externalNet   external network name
     * @param peerRouterIp  external peer router IP address
     */
    public KubevirtRouterEvent(Type type, KubevirtRouter subject,
                               String externalIp, String externalNet,
                               String peerRouterIp) {
        super(type, subject);
        this.internal = null;
        this.podName = null;
        this.floatingIp = null;
        this.externalIp = externalIp;
        this.externalNet = externalNet;
        this.peerRouterIp = peerRouterIp;
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
        KUBEVIRT_FLOATING_IP_DISASSOCIATED
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
                .toString();
    }
}
