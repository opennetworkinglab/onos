package org.onlab.onos.store.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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

import org.onlab.onos.store.serializers.ImmutableListSerializer;
import org.onlab.onos.store.serializers.ImmutableMapSerializer;
import org.onlab.onos.store.serializers.ImmutableSetSerializer;
import org.onlab.onos.store.serializers.KryoSerializer;
import org.onlab.onos.store.service.ReadRequest;
import org.onlab.onos.store.service.ReadResult;
import org.onlab.onos.store.service.WriteRequest;
import org.onlab.onos.store.service.WriteResult;
import org.onlab.util.KryoNamespace;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.serializers.CollectionSerializer;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

/**
 * {@link Protocol} based on {@link org.onlab.netty.NettyMessagingService}.
 */
public class NettyProtocol implements Protocol<TcpMember> {

    public static final String COPYCAT_PING = "copycat-raft-consensus-ping";
    public static final String COPYCAT_SYNC = "copycat-raft-consensus-sync";
    public static final String COPYCAT_POLL = "copycat-raft-consensus-poll";
    public static final String COPYCAT_SUBMIT = "copycat-raft-consensus-submit";

    // TODO: make this configurable.
    public static final long RETRY_INTERVAL_MILLIS = 2000;

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

    // TODO: Move to the right place.
    private static final KryoNamespace CRAFT = KryoNamespace.newBuilder()
            .register(ReadRequest.class)
            .register(WriteRequest.class)
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
                    byte[].class)
            .build();

    public static final KryoSerializer SERIALIZER = new KryoSerializer() {
        @Override
        protected void setupKryoPool() {
            serializerPool = KryoNamespace.newBuilder()
                    .register(COPYCAT)
                    .register(COMMON)
                    .register(CRAFT)
                    .build()
                    .populate(1);
        }
    };

    private NettyProtocolServer server = null;

    // FIXME: This is a total hack.Assumes
    // ProtocolServer is initialized before ProtocolClient
    protected NettyProtocolServer getServer() {
        if (server == null) {
            throw new IllegalStateException("ProtocolServer is not initialized yet!");
        }
        return server;
    }

    @Override
    public ProtocolServer createServer(TcpMember member) {
        server = new NettyProtocolServer(member);
        return server;
    }

    @Override
    public ProtocolClient createClient(TcpMember member) {
        return new NettyProtocolClient(this, member);
    }
}
