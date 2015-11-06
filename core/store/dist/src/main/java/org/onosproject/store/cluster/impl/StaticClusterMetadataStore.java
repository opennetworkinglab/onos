package org.onosproject.store.cluster.impl;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.net.NetworkInterface.getNetworkInterfaces;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Inet4Address;
import java.net.NetworkInterface;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.IpAddress;
import org.onosproject.cluster.ClusterMetadata;
import org.onosproject.cluster.ClusterMetadataEvent;
import org.onosproject.cluster.ClusterMetadataStore;
import org.onosproject.cluster.ClusterMetadataStoreDelegate;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.DefaultControllerNode;
import org.onosproject.cluster.NodeId;
import org.onosproject.cluster.Partition;
import org.onosproject.store.AbstractStore;
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
import com.google.common.collect.Lists;
import com.google.common.io.Files;

/**
 * ClusterMetadataStore backed by a local file.
 */
@Component(immediate = true, enabled = true)
@Service
public class StaticClusterMetadataStore
    extends AbstractStore<ClusterMetadataEvent, ClusterMetadataStoreDelegate>
    implements ClusterMetadataStore {

    private final Logger log = getLogger(getClass());

    private static final String ONOS_IP = "ONOS_IP";
    private static final String ONOS_INTERFACE = "ONOS_INTERFACE";
    private static final String ONOS_ALLOW_IPV6 = "ONOS_ALLOW_IPV6";
    private static final String DEFAULT_ONOS_INTERFACE = "eth0";
    private static final String CLUSTER_METADATA_FILE = "../config/cluster.json";
    private static final int DEFAULT_ONOS_PORT = 9876;
    private final File metadataFile = new File(CLUSTER_METADATA_FILE);
    private AtomicReference<ClusterMetadata> metadata = new AtomicReference<>();
    private ObjectMapper mapper;
    private long version;

    @Activate
    public void activate() {
        mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addSerializer(NodeId.class, new NodeIdSerializer());
        module.addDeserializer(NodeId.class, new NodeIdDeserializer());
        module.addSerializer(ControllerNode.class, new ControllerNodeSerializer());
        module.addDeserializer(ControllerNode.class, new ControllerNodeDeserializer());
        mapper.registerModule(module);
        File metadataFile = new File(CLUSTER_METADATA_FILE);
        if (metadataFile.exists()) {
            try {
                metadata.set(mapper.readValue(metadataFile, ClusterMetadata.class));
                version = metadataFile.lastModified();
            } catch (IOException e) {
                Throwables.propagate(e);
            }
        } else {
            String localIp = getSiteLocalAddress();
            ControllerNode localNode =
                    new DefaultControllerNode(new NodeId(localIp), IpAddress.valueOf(localIp), DEFAULT_ONOS_PORT);
            metadata.set(ClusterMetadata.builder()
                    .withName("default")
                    .withControllerNodes(Arrays.asList(localNode))
                    .withPartitions(Lists.newArrayList(new Partition("p1", Lists.newArrayList(localNode.id()))))
                    .build());
            version = System.currentTimeMillis();
        }
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

    @Override
    public void setDelegate(ClusterMetadataStoreDelegate delegate) {
        checkNotNull(delegate, "Delegate cannot be null");
        this.delegate = delegate;
    }

    @Override
    public void unsetDelegate(ClusterMetadataStoreDelegate delegate) {
        this.delegate = null;
    }

    @Override
    public boolean hasDelegate() {
        return this.delegate != null;
    }

    @Override
    public Versioned<ClusterMetadata> getClusterMetadata() {
        return new Versioned<>(metadata.get(), version);
    }

    @Override
    public void setClusterMetadata(ClusterMetadata metadata) {
        checkNotNull(metadata);
        try {
            Files.createParentDirs(metadataFile);
            mapper.writeValue(metadataFile, metadata);
            this.metadata.set(metadata);
        } catch (IOException e) {
            Throwables.propagate(e);
        }
    }

    @Override
    public void setActiveReplica(String partitionId, NodeId nodeId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void unsetActiveReplica(String partitionId, NodeId nodeId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<NodeId> getActiveReplicas(String partitionId) {
        return metadata.get().getPartitions()
                       .stream()
                       .filter(r -> r.getName().equals(partitionId))
                       .findFirst()
                       .map(r -> r.getMembers())
                       .orElse(null);
    }

    private static class ControllerNodeSerializer extends JsonSerializer<ControllerNode> {
        @Override
        public void serialize(ControllerNode node, JsonGenerator jgen, SerializerProvider provider)
          throws IOException, JsonProcessingException {
            jgen.writeStartObject();
            jgen.writeStringField("id", node.id().toString());
            jgen.writeStringField("ip", node.ip().toString());
            jgen.writeNumberField("port", node.tcpPort());
            jgen.writeEndObject();
        }
    }

    private static class ControllerNodeDeserializer extends JsonDeserializer<ControllerNode> {
        @Override
        public ControllerNode deserialize(JsonParser jp, DeserializationContext ctxt)
                throws IOException, JsonProcessingException {
            JsonNode node = jp.getCodec().readTree(jp);
            NodeId nodeId = new NodeId(node.get("id").textValue());
            IpAddress ip = IpAddress.valueOf(node.get("ip").textValue());
            int port = node.get("port").asInt();
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


    private static String getSiteLocalAddress() {

        /*
         * If the IP ONOS should use is set via the environment variable we will assume it is valid and should be used.
         * Setting the IP address takes presidence over setting the interface via the environment.
         */
        String useOnosIp = System.getenv(ONOS_IP);
        if (useOnosIp != null) {
            return useOnosIp;
        }

        // Read environment variables for IP interface information or set to default
        String useOnosInterface = System.getenv(ONOS_INTERFACE);
        if (useOnosInterface == null) {
            useOnosInterface = DEFAULT_ONOS_INTERFACE;
        }

        // Capture if they want to limit IP address selection to only IPv4 (default).
        boolean allowIPv6 = (System.getenv(ONOS_ALLOW_IPV6) != null);

        Function<NetworkInterface, IpAddress> ipLookup = nif -> {
            IpAddress fallback = null;

            // nif can be null if the interface name specified doesn't exist on the node's host
            if (nif != null) {
                for (InetAddress address : Collections.list(nif.getInetAddresses())) {
                    if (address.isSiteLocalAddress() && (allowIPv6 || address instanceof Inet4Address)) {
                        return IpAddress.valueOf(address);
                    }
                    if (fallback == null && !address.isLoopbackAddress() && !address.isMulticastAddress()
                        && (allowIPv6 || address instanceof Inet4Address)) {
                        fallback = IpAddress.valueOf(address);
                    }
                }
            }
            return fallback;
        };
        try {
            IpAddress ip = ipLookup.apply(NetworkInterface.getByName(useOnosInterface));
            if (ip != null) {
                return ip.toString();
            }
            for (NetworkInterface nif : Collections.list(getNetworkInterfaces())) {
                if (!nif.getName().equals(useOnosInterface)) {
                    ip = ipLookup.apply(nif);
                    if (ip != null) {
                        return ip.toString();
                    }
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Unable to get network interfaces", e);
        }

        return IpAddress.valueOf(InetAddress.getLoopbackAddress()).toString();
    }
}
