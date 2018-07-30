/*
 * Copyright 2016-present Open Networking Foundation
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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.packet.IpAddress;
import org.onosproject.cluster.ClusterMetadata;
import org.onosproject.cluster.ClusterMetadataProvider;
import org.onosproject.cluster.ClusterMetadataProviderRegistry;
import org.onosproject.cluster.ClusterMetadataProviderService;
import org.onosproject.cluster.DefaultControllerNode;
import org.onosproject.cluster.Node;
import org.onosproject.cluster.NodeId;
import org.onosproject.cluster.PartitionId;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.store.service.Versioned;
import org.slf4j.Logger;

import static com.google.common.base.Preconditions.checkState;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provider of {@link ClusterMetadata cluster metadata} sourced from a local config file.
 */
@Component(immediate = true)
public class ConfigFileBasedClusterMetadataProvider implements ClusterMetadataProvider {

    private final Logger log = getLogger(getClass());

    private static final String CONFIG_DIR = "../config";
    private static final String CONFIG_FILE_NAME = "cluster.json";
    private static final File CONFIG_FILE = new File(CONFIG_DIR, CONFIG_FILE_NAME);

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterMetadataProviderRegistry providerRegistry;

    private static final ProviderId PROVIDER_ID = new ProviderId("file", "none");
    private final AtomicReference<Versioned<ClusterMetadata>> cachedMetadata = new AtomicReference<>();
    private final ScheduledExecutorService configFileChangeDetector =
            newSingleThreadScheduledExecutor(groupedThreads("onos/cluster/metadata/config-watcher", "", log));

    private String metadataUrl;
    private ObjectMapper mapper;
    private ClusterMetadataProviderService providerService;

    @Activate
    public void activate() {
        mapper = new ObjectMapper();
        providerService = providerRegistry.register(this);
        metadataUrl = System.getProperty("onos.cluster.metadata.uri", "file://" + CONFIG_DIR + "/" + CONFIG_FILE);
        configFileChangeDetector.scheduleWithFixedDelay(() -> watchUrl(metadataUrl), 100, 500, TimeUnit.MILLISECONDS);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        configFileChangeDetector.shutdown();
        providerRegistry.unregister(this);
        log.info("Stopped");
    }

    @Override
    public ProviderId id() {
        return PROVIDER_ID;
    }

    @Override
    public Versioned<ClusterMetadata> getClusterMetadata() {
        checkState(isAvailable());
        synchronized (this) {
            if (cachedMetadata.get() == null) {
                cachedMetadata.set(blockForMetadata(metadataUrl));
            }
            return cachedMetadata.get();
        }
    }

    private ClusterMetadataPrototype toPrototype(ClusterMetadata metadata) {
        ClusterMetadataPrototype prototype = new ClusterMetadataPrototype();
        prototype.setName(metadata.getName());
        prototype.setController(metadata.getNodes()
                .stream()
                .map(this::toPrototype)
                .collect(Collectors.toSet()));
        prototype.setStorage(metadata.getStorageNodes()
                .stream()
                .map(this::toPrototype)
                .collect(Collectors.toSet()));
        return prototype;
    }

    private NodePrototype toPrototype(Node node) {
        NodePrototype prototype = new NodePrototype();
        prototype.setId(node.id().id());
        prototype.setIp(node.ip().toString());
        prototype.setPort(node.tcpPort());
        return prototype;
    }

    @Override
    public void setClusterMetadata(ClusterMetadata metadata) {
        try {
            File configFile = new File(metadataUrl.replaceFirst("file://", ""));
            Files.createParentDirs(configFile);
            ClusterMetadataPrototype metadataPrototype = toPrototype(metadata);
            mapper.writeValue(configFile, metadataPrototype);
            cachedMetadata.set(fetchMetadata(metadataUrl));
            providerService.clusterMetadataChanged(new Versioned<>(metadata, configFile.lastModified()));
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public void addActivePartitionMember(PartitionId partitionId, NodeId nodeId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeActivePartitionMember(PartitionId partitionId, NodeId nodeId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<NodeId> getActivePartitionMembers(PartitionId partitionId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isAvailable() {
        try {
            URL url = new URL(metadataUrl);
            if ("file".equals(url.getProtocol())) {
                File file = new File(metadataUrl.replaceFirst("file://", ""));
                return file.exists();
            } else {
                // Return true for HTTP URLs since we allow blocking until HTTP servers come up
                return "http".equals(url.getProtocol());
            }
        } catch (Exception e) {
            log.warn("Exception accessing metadata file at {}:", metadataUrl, e);
            return false;
        }
    }

    private Versioned<ClusterMetadata> blockForMetadata(String metadataUrl) {
        long iterations = 0;
        for (;;) {
            try {
                Versioned<ClusterMetadata> metadata = fetchMetadata(metadataUrl);
                if (metadata != null) {
                    return metadata;
                }
            } catch (Exception e) {
                log.warn("Exception attempting to access metadata file at {}: {}", metadataUrl, e);
            }

            try {
                Thread.sleep((int) Math.pow(2, iterations < 7 ? ++iterations : iterations) * 10L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
        }
    }

    private Versioned<ClusterMetadata> fetchMetadata(String metadataUrl) {
        try {
            URL url = new URL(metadataUrl);
            ClusterMetadataPrototype metadata = null;
            long version = 0;
            if ("file".equals(url.getProtocol())) {
                File file = new File(metadataUrl.replaceFirst("file://", ""));
                version = file.lastModified();
                metadata = mapper.readValue(new FileInputStream(file), ClusterMetadataPrototype.class);
            } else if ("http".equals(url.getProtocol())) {
                try {
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    if (conn.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND) {
                        log.warn("Could not reach metadata URL {}. Retrying...", url);
                        return null;
                    }
                    if (conn.getResponseCode() == HttpURLConnection.HTTP_NO_CONTENT) {
                        return null;
                    }
                    version = conn.getLastModified();
                    metadata = mapper.readValue(conn.getInputStream(), ClusterMetadataPrototype.class);
                } catch (IOException e) {
                    log.warn("Could not reach metadata URL {}. Retrying...", url);
                    return null;
                }
            }

            if (null == metadata) {
                log.warn("Metadata is null in the function fetchMetadata");
                throw new NullPointerException();
            }

            return new Versioned<>(new ClusterMetadata(
                PROVIDER_ID,
                metadata.getName(),
                metadata.getNode() != null ?
                    new DefaultControllerNode(
                        metadata.getNode().getId() != null
                            ? NodeId.nodeId(metadata.getNode().getId())
                            : metadata.getNode().getIp() != null
                            ? NodeId.nodeId(IpAddress.valueOf(metadata.getNode().getIp()).toString())
                            : NodeId.nodeId(UUID.randomUUID().toString()),
                        metadata.getNode().getIp() != null
                            ? IpAddress.valueOf(metadata.getNode().getIp())
                            : null,
                        metadata.getNode().getPort() != null
                            ? metadata.getNode().getPort()
                            : DefaultControllerNode.DEFAULT_PORT) : null,
                    metadata.getController()
                        .stream()
                        .map(node -> new DefaultControllerNode(
                            NodeId.nodeId(node.getId()),
                            IpAddress.valueOf(node.getIp()),
                            node.getPort() != null ? node.getPort() : 5679))
                        .collect(Collectors.toSet()),
                    metadata.getStorage()
                        .stream()
                        .map(node -> new DefaultControllerNode(
                            NodeId.nodeId(node.getId()),
                            IpAddress.valueOf(node.getIp()),
                            node.getPort() != null ? node.getPort() : 5679))
                        .collect(Collectors.toSet())),
                version);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Monitors the metadata url for any updates and notifies providerService accordingly.
     */
    private void watchUrl(String metadataUrl) {
        if (!isAvailable()) {
            return;
        }
        // TODO: We are merely polling the url.
        // This can be easily addressed for files. For http urls we need to move to a push style protocol.
        try {
            Versioned<ClusterMetadata> latestMetadata = fetchMetadata(metadataUrl);
            if (cachedMetadata.get() != null && latestMetadata != null
                    && cachedMetadata.get().version() < latestMetadata.version()) {
                cachedMetadata.set(latestMetadata);
                providerService.clusterMetadataChanged(latestMetadata);
            }
        } catch (Exception e) {
            log.error("Unable to parse metadata : ", e);
        }
    }

    private static class ClusterMetadataPrototype {
        private String name;
        private NodePrototype node;
        private Set<NodePrototype> controller = Sets.newHashSet();
        private Set<NodePrototype> storage = Sets.newHashSet();

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public NodePrototype getNode() {
            return node;
        }

        public void setNode(NodePrototype node) {
            this.node = node;
        }

        public Set<NodePrototype> getController() {
            return controller;
        }

        public void setController(Set<NodePrototype> controller) {
            this.controller = controller;
        }

        public Set<NodePrototype> getStorage() {
            return storage;
        }

        public void setStorage(Set<NodePrototype> storage) {
            this.storage = storage;
        }
    }

    private static class NodePrototype {
        private String id;
        private String ip;
        private Integer port;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getIp() {
            return ip;
        }

        public void setIp(String ip) {
            this.ip = ip;
        }

        public Integer getPort() {
            return port;
        }

        public void setPort(Integer port) {
            this.port = port;
        }
    }
}