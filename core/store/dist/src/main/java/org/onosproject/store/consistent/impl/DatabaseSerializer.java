package org.onosproject.store.consistent.impl;

import java.nio.ByteBuffer;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.onlab.util.KryoNamespace;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.serializers.KryoSerializer;
import org.onosproject.store.service.Versioned;

import net.kuujo.copycat.cluster.internal.MemberInfo;
import net.kuujo.copycat.protocol.rpc.AppendRequest;
import net.kuujo.copycat.protocol.rpc.AppendResponse;
import net.kuujo.copycat.protocol.rpc.CommitRequest;
import net.kuujo.copycat.protocol.rpc.CommitResponse;
import net.kuujo.copycat.protocol.rpc.PollRequest;
import net.kuujo.copycat.protocol.rpc.PollResponse;
import net.kuujo.copycat.protocol.rpc.QueryRequest;
import net.kuujo.copycat.protocol.rpc.QueryResponse;
import net.kuujo.copycat.protocol.rpc.ReplicaInfo;
import net.kuujo.copycat.protocol.rpc.SyncRequest;
import net.kuujo.copycat.protocol.rpc.SyncResponse;
import net.kuujo.copycat.util.serializer.SerializerConfig;

/**
 * Serializer for DatabaseManager's interaction with Copycat.
 */
public class DatabaseSerializer extends SerializerConfig {

    private static final KryoNamespace COPYCAT = KryoNamespace.newBuilder()
            .nextId(KryoNamespace.FLOATING_ID)
            .register(AppendRequest.class)
            .register(AppendResponse.class)
            .register(SyncRequest.class)
            .register(SyncResponse.class)
            .register(PollRequest.class)
            .register(PollResponse.class)
            .register(QueryRequest.class)
            .register(QueryResponse.class)
            .register(CommitRequest.class)
            .register(CommitResponse.class)
            .register(ReplicaInfo.class)
            .register(MemberInfo.class)
            .build();

    private static final KryoNamespace ONOS_STORE = KryoNamespace.newBuilder()
            .nextId(KryoNamespace.FLOATING_ID)
            .register(Versioned.class)
            .register(Pair.class)
            .register(ImmutablePair.class)
            .build();

    private static final KryoSerializer SERIALIZER = new KryoSerializer() {
        @Override
        protected void setupKryoPool() {
            serializerPool = KryoNamespace.newBuilder()
                    .register(KryoNamespaces.BASIC)
                    .register(COPYCAT)
                    .register(ONOS_STORE)
                    .build();
        }
    };

    @Override
    public ByteBuffer writeObject(Object object) {
        return ByteBuffer.wrap(SERIALIZER.encode(object));
    }

    @Override
    public <T> T readObject(ByteBuffer buffer) {
        return SERIALIZER.decode(buffer);
    }
}