/*
 * Copyright 2015 Open Networking Laboratory
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

package org.onosproject.store.consistent.impl;

import java.nio.ByteBuffer;

import org.onlab.util.KryoNamespace;
import org.onosproject.cluster.NodeId;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.serializers.KryoSerializer;
import org.onosproject.store.service.DatabaseUpdate;
import org.onosproject.store.service.Transaction;
import org.onosproject.store.service.Versioned;

import net.kuujo.copycat.cluster.internal.MemberInfo;
import net.kuujo.copycat.raft.protocol.AppendRequest;
import net.kuujo.copycat.raft.protocol.AppendResponse;
import net.kuujo.copycat.raft.protocol.CommitRequest;
import net.kuujo.copycat.raft.protocol.CommitResponse;
import net.kuujo.copycat.raft.protocol.PollRequest;
import net.kuujo.copycat.raft.protocol.PollResponse;
import net.kuujo.copycat.raft.protocol.QueryRequest;
import net.kuujo.copycat.raft.protocol.QueryResponse;
import net.kuujo.copycat.raft.protocol.ReplicaInfo;
import net.kuujo.copycat.raft.protocol.SyncRequest;
import net.kuujo.copycat.raft.protocol.SyncResponse;
import net.kuujo.copycat.raft.protocol.VoteRequest;
import net.kuujo.copycat.raft.protocol.VoteResponse;
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
            .register(VoteRequest.class)
            .register(VoteResponse.class)
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
            .register(DatabaseUpdate.class)
            .register(DatabaseUpdate.Type.class)
            .register(Result.class)
            .register(UpdateResult.class)
            .register(Result.Status.class)
            .register(DefaultTransaction.class)
            .register(Transaction.State.class)
            .register(org.onosproject.store.consistent.impl.CommitResponse.class)
            .register(Match.class)
            .register(NodeId.class)
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