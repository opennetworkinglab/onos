/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.vtnrsc.event;

import java.util.Objects;

import org.onosproject.vtnrsc.FloatingIp;
import org.onosproject.vtnrsc.Router;
import org.onosproject.vtnrsc.RouterInterface;
import org.onosproject.vtnrsc.PortPair;
import org.onosproject.vtnrsc.PortPairGroup;
import org.onosproject.vtnrsc.FlowClassifier;
import org.onosproject.vtnrsc.PortChain;
import org.onosproject.vtnrsc.VirtualPort;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Representation of a VtnRsc event feedback.
 */
public class VtnRscEventFeedback {
    private final FloatingIp floaingtIp;
    private final Router router;
    private final RouterInterface routerInterface;
    private final PortPair portPair;
    private final PortPairGroup portPairGroup;
    private final FlowClassifier flowClassifier;
    private final PortChain portChain;
    private final VirtualPort virtualPort;

    /**
     * Creates VtnRscEventFeedback object.
     *
     * @param floatingIp the floating Ip
     */
    public VtnRscEventFeedback(FloatingIp floatingIp) {
        this.floaingtIp = checkNotNull(floatingIp, "floaintIp cannot be null");
        this.router = null;
        this.routerInterface = null;
        this.portPair = null;
        this.portPairGroup = null;
        this.flowClassifier = null;
        this.portChain = null;
        this.virtualPort = null;
    }

    /**
     * Creates VtnRscEventFeedback object.
     *
     * @param router the router
     */
    public VtnRscEventFeedback(Router router) {
        this.floaingtIp = null;
        this.router = checkNotNull(router, "router cannot be null");
        this.routerInterface = null;
        this.portPair = null;
        this.portPairGroup = null;
        this.flowClassifier = null;
        this.portChain = null;
        this.virtualPort = null;
    }

    /**
     * Creates VtnRscEventFeedback object.
     *
     * @param routerInterface the router interface
     */
    public VtnRscEventFeedback(RouterInterface routerInterface) {
        this.floaingtIp = null;
        this.router = null;
        this.routerInterface = checkNotNull(routerInterface,
                                            "routerInterface cannot be null");
        this.portPair = null;
        this.portPairGroup = null;
        this.flowClassifier = null;
        this.portChain = null;
        this.virtualPort = null;
    }

    /**
     * Creates VtnRscEventFeedback object.
     *
     * @param portPair the Port-Pair
     */
    public VtnRscEventFeedback(PortPair portPair) {
        this.floaingtIp = null;
        this.router = null;
        this.routerInterface = null;
        this.portPair = checkNotNull(portPair,
                                     "Port-Pair cannot be null");
        this.portPairGroup = null;
        this.flowClassifier = null;
        this.portChain = null;
        this.virtualPort = null;
    }

    /**
     * Creates VtnRscEventFeedback object.
     *
     * @param portPairGroup the Port-Pair-Group
     */
    public VtnRscEventFeedback(PortPairGroup portPairGroup) {
        this.floaingtIp = null;
        this.router = null;
        this.routerInterface = null;
        this.portPair = null;
        this.portPairGroup = checkNotNull(portPairGroup,
                "Port-Pair-Group cannot be null");
        this.flowClassifier = null;
        this.portChain = null;
        this.virtualPort = null;
    }

    /**
     * Creates VtnRscEventFeedback object.
     *
     * @param flowClassifier the Flow-Classifier
     */
    public VtnRscEventFeedback(FlowClassifier flowClassifier) {
        this.floaingtIp = null;
        this.router = null;
        this.routerInterface = null;
        this.portPair = null;
        this.portPairGroup = null;
        this.flowClassifier = checkNotNull(flowClassifier,
                "Flow-Classifier cannot be null");
        this.portChain = null;
        this.virtualPort = null;
    }

    /**
     * Creates VtnRscEventFeedback object.
     *
     * @param portChain the Port-Chain
     */
    public VtnRscEventFeedback(PortChain portChain) {
        this.floaingtIp = null;
        this.router = null;
        this.routerInterface = null;
        this.portPair = null;
        this.portPairGroup = null;
        this.flowClassifier = null;
        this.portChain = checkNotNull(portChain,
                "Port-Chain cannot be null");
        this.virtualPort = null;
    }

    /**
     * Creates VtnRscEventFeedback object.
     *
     * @param virtualPort the Virtual-Port
     */
    public VtnRscEventFeedback(VirtualPort virtualPort) {
        this.floaingtIp = null;
        this.router = null;
        this.routerInterface = null;
        this.portPair = null;
        this.portPairGroup = null;
        this.flowClassifier = null;
        this.portChain = null;
        this.virtualPort = checkNotNull(virtualPort,
                "Virtual-port cannot be null");
    }

    /**
     * Returns floating IP.
     *
     * @return floaingtIp the floating IP
     */
    public FloatingIp floatingIp() {
        return floaingtIp;
    }

    /**
     * Returns router.
     *
     * @return router the router
     */
    public Router router() {
        return router;
    }

    /**
     * Returns router interface.
     *
     * @return routerInterface the router interface
     */
    public RouterInterface routerInterface() {
        return routerInterface;
    }

    /**
     * Returns Port-Pair.
     *
     * @return portPair the Port-Pair
     */
    public PortPair portPair() {
        return portPair;
    }

    /**
     * Returns Port-Pair-Group.
     *
     * @return portPairGroup the Port-Pair-Group
     */
    public PortPairGroup portPairGroup() {
        return portPairGroup;
    }

    /**
     * Returns Flow-Classifier.
     *
     * @return flowClassifier the Flow-Classifier
     */
    public FlowClassifier flowClassifier() {
        return flowClassifier;
    }

    /**
     * Returns Port-Chain.
     *
     * @return portChain the Port-Chain
     */
    public PortChain portChain() {
        return portChain;
    }

    /**
     * Returns Virtual-Port.
     *
     * @return virtualPort the Virtual-Port
     */
    public VirtualPort virtualPort() {
        return virtualPort;
    }

    @Override
    public int hashCode() {
        return Objects.hash(floaingtIp, router, routerInterface, portPair,
                            portPairGroup, flowClassifier, portChain, virtualPort);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof VtnRscEventFeedback) {
            final VtnRscEventFeedback that = (VtnRscEventFeedback) obj;
            return Objects.equals(this.floaingtIp, that.floaingtIp)
                    && Objects.equals(this.router, that.router)
                    && Objects.equals(this.routerInterface, that.routerInterface)
                    && Objects.equals(this.portPair, that.portPair)
                    && Objects.equals(this.portPairGroup, that.portPairGroup)
                    && Objects.equals(this.flowClassifier, that.flowClassifier)
                    && Objects.equals(this.portChain, that.portChain)
                    && Objects.equals(this.virtualPort, that.virtualPort);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("router", router)
                .add("floaingtIp", floaingtIp)
                .add("routerInterface", routerInterface)
                .add("portPair", portPair)
                .add("portPairGroup", portPairGroup)
                .add("flowClassifier", flowClassifier)
                .add("portChain", portChain)
                .add("virtualPort", virtualPort)
                .toString();
    }
}
