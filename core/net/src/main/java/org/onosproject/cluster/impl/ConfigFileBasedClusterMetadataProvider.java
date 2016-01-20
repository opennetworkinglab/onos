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

import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.IOException;
import java.util.Set;
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

    private static final File CONFIG_FILE = new File("../config/cluster.json");

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterMetadataProviderRegistry providerRegistry;

    private static final ProviderId PROVIDER_ID = new ProviderId("config", "none");
    private AtomicReference<Versioned<ClusterMetadata>> cachedMetadata = new AtomicReference<>();

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
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
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
                loadMetadata();
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
        return CONFIG_FILE.exists();
    }

    private void loadMetadata() {
        ClusterMetadata metadata = null;
        long version = 0;
        try {
            metadata = mapper.readValue(CONFIG_FILE, ClusterMetadata.class);
            version = metadata.hashCode();
        } catch (IOException e) {
            Throwables.propagate(e);
        }
        cachedMetadata.set(new Versioned<>(new ClusterMetadata(PROVIDER_ID,
                                                               metadata.getName(),
                                                               Sets.newHashSet(metadata.getNodes()),
                                                               Sets.newHashSet(metadata.getPartitions())),
                                           version));
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
}