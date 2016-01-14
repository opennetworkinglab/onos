/*
 * Copyright 2015-2016 Open Networking Laboratory
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
package org.onosproject.cluster.impl;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collection;
import java.util.Enumeration;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.IpAddress;
import org.onosproject.cluster.ClusterMetadata;
import org.onosproject.cluster.ClusterMetadataAdminService;
import org.onosproject.cluster.ClusterMetadataEvent;
import org.onosproject.cluster.ClusterMetadataEventListener;
import org.onosproject.cluster.ClusterMetadataService;
import org.onosproject.cluster.ClusterMetadataStore;
import org.onosproject.cluster.ClusterMetadataStoreDelegate;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.event.AbstractListenerManager;
import org.onosproject.store.service.Versioned;
import org.slf4j.Logger;

/**
 * Implementation of ClusterMetadataService.
 */
@Component(immediate = true)
@Service
public class ClusterMetadataManager
    extends AbstractListenerManager<ClusterMetadataEvent, ClusterMetadataEventListener>
    implements ClusterMetadataService, ClusterMetadataAdminService {

    private final Logger log = getLogger(getClass());
    private ControllerNode localNode;

    private ClusterMetadataStoreDelegate delegate = new InternalStoreDelegate();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterMetadataStore store;

    @Activate
    public void activate() {
        store.setDelegate(delegate);
        eventDispatcher.addSink(ClusterMetadataEvent.class, listenerRegistry);
        establishSelfIdentity();
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        store.unsetDelegate(delegate);
        eventDispatcher.removeSink(ClusterMetadataEvent.class);
        log.info("Stopped");
    }

    @Override
    public ClusterMetadata getClusterMetadata() {
        return Versioned.valueOrElse(store.getClusterMetadata(), null);
    }

    @Override
    public ControllerNode getLocalNode() {
        return localNode;
    }

    @Override
    public void setClusterMetadata(ClusterMetadata metadata) {
        checkNotNull(metadata, "Cluster metadata cannot be null");
        store.setClusterMetadata(metadata);
    }

    // Store delegate to re-post events emitted from the store.
    private class InternalStoreDelegate implements ClusterMetadataStoreDelegate {
        @Override
        public void notify(ClusterMetadataEvent event) {
            post(event);
        }
    }

    private IpAddress findLocalIp(Collection<ControllerNode> controllerNodes) throws SocketException {
        Enumeration<NetworkInterface> interfaces =
                NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface iface = interfaces.nextElement();
            Enumeration<InetAddress> inetAddresses = iface.getInetAddresses();
            while (inetAddresses.hasMoreElements()) {
                IpAddress ip = IpAddress.valueOf(inetAddresses.nextElement());
                if (controllerNodes.stream()
                        .map(ControllerNode::ip)
                        .anyMatch(nodeIp -> ip.equals(nodeIp))) {
                    return ip;
                }
            }
        }
        throw new IllegalStateException("Unable to determine local ip");
    }

    private void establishSelfIdentity() {
        try {
            IpAddress ip = findLocalIp(getClusterMetadata().getNodes());
            localNode = getClusterMetadata().getNodes()
                                            .stream()
                                            .filter(node -> node.ip().equals(ip))
                                            .findFirst()
                                            .get();
        } catch (SocketException e) {
            throw new IllegalStateException("Cannot determine local IP", e);
        }
    }
}