package org.onlab.onos.store.service.impl;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Vector;

import net.kuujo.copycat.cluster.TcpClusterConfig;
import net.kuujo.copycat.cluster.TcpMember;
import net.kuujo.copycat.internal.log.ConfigurationEntry;
import net.kuujo.copycat.internal.log.CopycatEntry;
import net.kuujo.copycat.internal.log.OperationEntry;
import net.kuujo.copycat.internal.log.SnapshotEntry;
import net.kuujo.copycat.protocol.PingRequest;
import net.kuujo.copycat.protocol.PingResponse;
import net.kuujo.copycat.protocol.PollRequest;
import net.kuujo.copycat.protocol.PollResponse;
import net.kuujo.copycat.protocol.Response.Status;
import net.kuujo.copycat.protocol.SubmitRequest;
import net.kuujo.copycat.protocol.SubmitResponse;
import net.kuujo.copycat.protocol.SyncRequest;
import net.kuujo.copycat.protocol.SyncResponse;
import net.kuujo.copycat.spi.protocol.Protocol;
import net.kuujo.copycat.spi.protocol.ProtocolClient;
import net.kuujo.copycat.spi.protocol.ProtocolServer;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.onos.cluster.ClusterService;
import org.onlab.onos.cluster.ControllerNode;
import org.onlab.onos.store.cluster.messaging.ClusterCommunicationService;
import org.onlab.onos.store.cluster.messaging.MessageSubject;
import org.onlab.onos.store.serializers.ImmutableListSerializer;
import org.onlab.onos.store.serializers.ImmutableMapSerializer;
import org.onlab.onos.store.serializers.ImmutableSetSerializer;
import org.onlab.onos.store.serializers.KryoSerializer;
import org.onlab.onos.store.service.ReadRequest;
import org.onlab.onos.store.service.ReadResult;
import org.onlab.onos.store.service.VersionedValue;
import org.onlab.onos.store.service.WriteRequest;
import org.onlab.onos.store.service.WriteResult;
import org.onlab.util.KryoNamespace;
import org.slf4j.Logger;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.serializers.CollectionSerializer;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

/**
 * ONOS Cluster messaging based Copycat protocol.
 */
@Component(immediate = true)
@Service
public class ClusterMessagingProtocol
    implements DatabaseProtocolService, Protocol<TcpMember> {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterCommunicationService clusterCommunicator;

    public static final MessageSubject COPYCAT_PING =
            new MessageSubject("copycat-raft-consensus-ping");
    public static final MessageSubject COPYCAT_SYNC =
            new MessageSubject("copycat-raft-consensus-sync");
    public static final MessageSubject COPYCAT_POLL =
            new MessageSubject("copycat-raft-consensus-poll");
    public static final MessageSubject COPYCAT_SUBMIT =
            new MessageSubject("copycat-raft-consensus-submit");

    private static final KryoNamespace COPYCAT = KryoNamespace.newBuilder()
            .register(PingRequest.class)
            .register(PingResponse.class)
            .register(PollRequest.class)
            .register(PollResponse.class)
            .register(SyncRequest.class)
            .register(SyncResponse.class)
            .register(SubmitRequest.class)
            .register(SubmitResponse.class)
            .register(Status.class)
            .register(ConfigurationEntry.class)
            .register(SnapshotEntry.class)
            .register(CopycatEntry.class)
            .register(OperationEntry.class)
            .register(TcpClusterConfig.class)
            .register(TcpMember.class)
            .build();

    private static final KryoNamespace DATABASE = KryoNamespace.newBuilder()
            .register(ReadRequest.class)
            .register(WriteRequest.class)
            .register(WriteRequest.Type.class)
            .register(InternalReadResult.class)
            .register(InternalWriteResult.class)
            .register(InternalReadResult.Status.class)
            .register(WriteResult.class)
            .register(ReadResult.class)
            .register(InternalWriteResult.Status.class)
            .register(VersionedValue.class)
            .build();

    public static final KryoNamespace COMMON = KryoNamespace.newBuilder()
            .register(Arrays.asList().getClass(), new CollectionSerializer() {
                @Override
                @SuppressWarnings("rawtypes")
                protected Collection<?> create(Kryo kryo, Input input, Class<Collection> type) {
                    return new ArrayList();
                }
            })
            .register(ImmutableMap.class, new ImmutableMapSerializer())
            .register(ImmutableList.class, new ImmutableListSerializer())
            .register(ImmutableSet.class, new ImmutableSetSerializer())
            .register(
                    Vector.class,
                    ArrayList.class,
                    Arrays.asList().getClass(),
                    HashMap.class,
                    HashSet.class,
                    LinkedList.class,
                    Collections.singletonList("").getClass(),
                    byte[].class)
            .build();

    // serializer used for CopyCat Protocol
    public static final KryoSerializer SERIALIZER = new KryoSerializer() {
        @Override
        protected void setupKryoPool() {
            serializerPool = KryoNamespace.newBuilder()
                    .register(COPYCAT)
                    .register(COMMON)
                    .register(DATABASE)
                    .build()
                    .populate(1);
        }
    };

    @Activate
    public void activate() {
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

    @Override
    public ProtocolServer createServer(TcpMember member) {
        return new ClusterMessagingProtocolServer(clusterCommunicator);
    }

    @Override
    public ProtocolClient createClient(TcpMember member) {
        ControllerNode node = getControllerNode(member.host(), member.port());
        checkNotNull(node, "A valid controller node is expected");
        return new ClusterMessagingProtocolClient(
                clusterCommunicator, node);
    }

    private ControllerNode getControllerNode(String host, int port) {
        for (ControllerNode node : clusterService.getNodes()) {
            if (node.ip().toString().equals(host) && node.tcpPort() == port) {
                return node;
            }
        }
        return null;
    }
}
