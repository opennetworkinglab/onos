/*
 * Copyright 2017-present Open Networking Laboratory
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
package org.onosproject.store.primitives.impl;

import java.net.ConnectException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import io.atomix.protocols.raft.RaftException;
import io.atomix.protocols.raft.cluster.MemberId;
import org.onosproject.cluster.NodeId;
import org.onosproject.store.cluster.messaging.ClusterCommunicationService;
import org.onosproject.store.cluster.messaging.MessageSubject;
import org.onosproject.store.cluster.messaging.MessagingException;
import org.onosproject.store.service.Serializer;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Abstract base class for Raft protocol client/server.
 */
public abstract class RaftCommunicator {
    protected final RaftMessageContext context;
    protected final Serializer serializer;
    protected final ClusterCommunicationService clusterCommunicator;

    public RaftCommunicator(
            RaftMessageContext context,
            Serializer serializer,
            ClusterCommunicationService clusterCommunicator) {
        this.context = checkNotNull(context, "context cannot be null");
        this.serializer = checkNotNull(serializer, "serializer cannot be null");
        this.clusterCommunicator = checkNotNull(clusterCommunicator, "clusterCommunicator cannot be null");
    }

    protected <T, U> CompletableFuture<U> sendAndReceive(MessageSubject subject, T request, MemberId memberId) {
        CompletableFuture<U> future = new CompletableFuture<>();
        clusterCommunicator.<T, U>sendAndReceive(
                request, subject, serializer::encode, serializer::decode, NodeId.nodeId(memberId.id()))
                .whenComplete((result, error) -> {
                    if (error == null) {
                        future.complete(result);
                    } else {
                        if (error instanceof CompletionException) {
                            error = error.getCause();
                        }
                        if (error instanceof MessagingException.NoRemoteHandler) {
                            error = new ConnectException(error.getMessage());
                        } else if (error instanceof MessagingException.RemoteHandlerFailure
                                || error instanceof MessagingException.ProtocolException) {
                            error = new RaftException.ProtocolException(error.getMessage());
                        }
                        future.completeExceptionally(error);
                    }
                });
        return future;
    }
}
