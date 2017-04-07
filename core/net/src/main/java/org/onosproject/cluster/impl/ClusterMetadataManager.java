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
package org.onosproject.cluster.impl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.IpAddress;
import org.onosproject.cluster.ClusterMetadata;
import org.onosproject.cluster.ClusterMetadataAdminService;
import org.onosproject.cluster.ClusterMetadataEvent;
import org.onosproject.cluster.ClusterMetadataEventListener;
import org.onosproject.cluster.ClusterMetadataProvider;
import org.onosproject.cluster.ClusterMetadataProviderRegistry;
import org.onosproject.cluster.ClusterMetadataProviderService;
import org.onosproject.cluster.ClusterMetadataService;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.NodeId;
import org.onosproject.cluster.PartitionId;
import org.onosproject.net.provider.AbstractListenerProviderRegistry;
import org.onosproject.net.provider.AbstractProviderService;
import org.onosproject.store.service.Versioned;
import org.slf4j.Logger;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.util.Collection;
import java.util.Enumeration;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.security.AppGuard.checkPermission;
import static org.onosproject.security.AppPermission.Type.CLUSTER_READ;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of ClusterMetadataService.
 */
@Component(immediate = true)
@Service
public class ClusterMetadataManager
    extends AbstractListenerProviderRegistry<ClusterMetadataEvent,
                                             ClusterMetadataEventListener,
                                             ClusterMetadataProvider,
                                             ClusterMetadataProviderService>
    implements ClusterMetadataService, ClusterMetadataAdminService, ClusterMetadataProviderRegistry {

    private final Logger log = getLogger(getClass());
    private ControllerNode localNode;

    @Activate
    public void activate() {
        // FIXME: Need to ensure all cluster metadata providers are registered before we activate
        eventDispatcher.addSink(ClusterMetadataEvent.class, listenerRegistry);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        eventDispatcher.removeSink(ClusterMetadataEvent.class);
        log.info("Stopped");
    }

    @Override
    public ClusterMetadata getClusterMetadata() {
        checkPermission(CLUSTER_READ);
        Versioned<ClusterMetadata> metadata = getProvider().getClusterMetadata();
        return metadata.value();
    }


    @Override
    protected ClusterMetadataProviderService createProviderService(
            ClusterMetadataProvider provider) {
        checkPermission(CLUSTER_READ);
        return new InternalClusterMetadataProviderService(provider);
    }

    @Override
    public ControllerNode getLocalNode() {
        checkPermission(CLUSTER_READ);
        if (localNode == null) {
            establishSelfIdentity();
        }
        return localNode;
    }

    @Override
    public void setClusterMetadata(ClusterMetadata metadata) {
        checkNotNull(metadata, "Cluster metadata cannot be null");
        ClusterMetadataProvider primaryProvider = getPrimaryProvider();
        if (primaryProvider == null) {
            throw new IllegalStateException("Missing primary provider. Cannot update cluster metadata");
        }
        primaryProvider.setClusterMetadata(metadata);
    }

    /**
     * Returns the provider to use for fetching cluster metadata.
     * @return cluster metadata provider
     */
    private ClusterMetadataProvider getProvider() {
        ClusterMetadataProvider primaryProvider = getPrimaryProvider();
        if (primaryProvider != null && primaryProvider.isAvailable()) {
            return primaryProvider;
        }
        return getProvider("default");
    }

    /**
     * Returns the primary provider for cluster metadata.
     * @return primary cluster metadata provider
     */
    private ClusterMetadataProvider getPrimaryProvider() {
        String metadataUri = System.getProperty("onos.cluster.metadata.uri");
        try {
            String protocol = metadataUri == null ? null : new URL(metadataUri).getProtocol();
            if (protocol != null && (!"file".equals(protocol) && !"http".equals(protocol))) {
                return getProvider(protocol);
            }
            // file provider supports both "file" and "http" uris
            return getProvider("file");
        } catch (MalformedURLException e) {
            return null;
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

    private class InternalClusterMetadataProviderService
            extends AbstractProviderService<ClusterMetadataProvider>
            implements ClusterMetadataProviderService {

        InternalClusterMetadataProviderService(ClusterMetadataProvider provider) {
            super(provider);
        }

        @Override
        public void clusterMetadataChanged(Versioned<ClusterMetadata> newMetadata) {
            log.info("Cluster metadata changed. New metadata: {}", newMetadata);
            post(new ClusterMetadataEvent(ClusterMetadataEvent.Type.METADATA_CHANGED, newMetadata.value()));
        }

        @Override
        public void newActiveMemberForPartition(PartitionId partitionId, NodeId nodeId) {
            log.info("Node {} is active member for partition {}", nodeId, partitionId);
            // TODO: notify listeners
        }
    }
}
