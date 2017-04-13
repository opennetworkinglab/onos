/*
 * Copyright 2016-present Open Networking Laboratory
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

import static org.onlab.util.Tools.groupedThreads;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

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
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.DefaultControllerNode;
import org.onosproject.cluster.DefaultPartition;
import org.onosproject.cluster.NodeId;
import org.onosproject.cluster.Partition;
import org.onosproject.cluster.PartitionId;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.store.service.Versioned;
import org.slf4j.Logger;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.base.Throwables;
import com.google.common.collect.Sets;
import com.google.common.io.Files;

import static com.google.common.base.Preconditions.checkState;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;

/**
 * Provider of {@link ClusterMetadata cluster metadata} sourced from a local config file.
 */
@Component(immediate = true)
public class ConfigFileBasedClusterMetadataProvider implements ClusterMetadataProvider {

    private final Logger log = getLogger(getClass());

    // constants for filed names (used in serialization)
    private static final String ID = "id";
    private static final String PORT = "port";
    private static final String IP = "ip";

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
        SimpleModule module = new SimpleModule();
        module.addSerializer(NodeId.class, new NodeIdSerializer());
        module.addDeserializer(NodeId.class, new NodeIdDeserializer());
        module.addSerializer(ControllerNode.class, new ControllerNodeSerializer());
        module.addDeserializer(ControllerNode.class, new ControllerNodeDeserializer());
        module.addDeserializer(Partition.class, new PartitionDeserializer());
        module.addSerializer(PartitionId.class, new PartitionIdSerializer());
        module.addDeserializer(PartitionId.class, new PartitionIdDeserializer());
        mapper.registerModule(module);
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
                cachedMetadata.set(fetchMetadata(metadataUrl));
            }
            return cachedMetadata.get();
        }
    }

    @Override
    public void setClusterMetadata(ClusterMetadata metadata) {
        try {
            Files.createParentDirs(CONFIG_FILE);
            mapper.writeValue(CONFIG_FILE, metadata);
            providerService.clusterMetadataChanged(new Versioned<>(metadata, CONFIG_FILE.lastModified()));
        } catch (IOException e) {
            Throwables.propagate(e);
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
            } else if ("http".equals(url.getProtocol())) {
                try (InputStream file = url.openStream()) {
                    return true;
                }
            } else {
                // Unsupported protocol
                return false;
            }
        } catch (Exception e) {
            log.warn("Exception accessing metadata file at {}:", metadataUrl, e);
            return false;
        }
    }

    private Versioned<ClusterMetadata> fetchMetadata(String metadataUrl) {
        try {
            URL url = new URL(metadataUrl);
            ClusterMetadata metadata = null;
            long version = 0;
            if ("file".equals(url.getProtocol())) {
                File file = new File(metadataUrl.replaceFirst("file://", ""));
                version = file.lastModified();
                metadata = mapper.readValue(new FileInputStream(file), ClusterMetadata.class);
            } else if ("http".equals(url.getProtocol())) {
                URLConnection conn = url.openConnection();
                version = conn.getLastModified();
                metadata = mapper.readValue(conn.getInputStream(), ClusterMetadata.class);
            }
            if (null == metadata) {
                log.warn("Metadata is null in the function fetchMetadata");
                throw new NullPointerException();
            }
            return new Versioned<>(new ClusterMetadata(PROVIDER_ID,
                                                       metadata.getName(),
                                                       Sets.newHashSet(metadata.getNodes()),
                                                       Sets.newHashSet(metadata.getPartitions())),
                                   version);
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    private static class PartitionDeserializer extends JsonDeserializer<Partition> {
        @Override
        public Partition deserialize(JsonParser jp, DeserializationContext ctxt)
                throws IOException, JsonProcessingException {
            return jp.readValueAs(DefaultPartition.class);
        }
    }

    private static class PartitionIdSerializer extends JsonSerializer<PartitionId> {
        @Override
        public void serialize(PartitionId partitionId, JsonGenerator jgen, SerializerProvider provider)
          throws IOException, JsonProcessingException {
            jgen.writeNumber(partitionId.asInt());
        }
    }

    private class PartitionIdDeserializer extends JsonDeserializer<PartitionId> {
        @Override
        public PartitionId deserialize(JsonParser jp, DeserializationContext ctxt)
          throws IOException, JsonProcessingException {
            JsonNode node = jp.getCodec().readTree(jp);
            return new PartitionId(node.asInt());
        }
    }

    private static class ControllerNodeSerializer extends JsonSerializer<ControllerNode> {
        @Override
        public void serialize(ControllerNode node, JsonGenerator jgen, SerializerProvider provider)
          throws IOException, JsonProcessingException {
            jgen.writeStartObject();
            jgen.writeStringField(ID, node.id().toString());
            jgen.writeStringField(IP, node.ip().toString());
            jgen.writeNumberField(PORT, node.tcpPort());
            jgen.writeEndObject();
        }
    }

    private static class ControllerNodeDeserializer extends JsonDeserializer<ControllerNode> {
        @Override
        public ControllerNode deserialize(JsonParser jp, DeserializationContext ctxt)
                throws IOException, JsonProcessingException {
            JsonNode node = jp.getCodec().readTree(jp);
            NodeId nodeId = new NodeId(node.get(ID).textValue());
            IpAddress ip = IpAddress.valueOf(node.get(IP).textValue());
            int port = node.get(PORT).asInt();
            return new DefaultControllerNode(nodeId, ip, port);
        }
    }

    private static class NodeIdSerializer extends JsonSerializer<NodeId> {
        @Override
        public void serialize(NodeId nodeId, JsonGenerator jgen, SerializerProvider provider)
          throws IOException, JsonProcessingException {
            jgen.writeString(nodeId.toString());
        }
    }

    private class NodeIdDeserializer extends JsonDeserializer<NodeId> {
        @Override
        public NodeId deserialize(JsonParser jp, DeserializationContext ctxt)
          throws IOException, JsonProcessingException {
            JsonNode node = jp.getCodec().readTree(jp);
            return new NodeId(node.asText());
        }
    }

    /**
     * Monitors the metadata url for any updates and notifies providerService accordingly.
     */
    private void watchUrl(String metadataUrl) {
        // TODO: We are merely polling the url.
        // This can be easily addressed for files. For http urls we need to move to a push style protocol.
        Versioned<ClusterMetadata> latestMetadata = fetchMetadata(metadataUrl);
        if (cachedMetadata.get() != null && cachedMetadata.get().version() < latestMetadata.version()) {
            cachedMetadata.set(latestMetadata);
            providerService.clusterMetadataChanged(latestMetadata);
        }
    }
}