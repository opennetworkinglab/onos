/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.store.atomix.primitives.impl;

import java.time.Duration;

import io.atomix.core.Atomix;
import io.atomix.primitive.Recovery;
import io.atomix.protocols.raft.MultiRaftProtocol;
import org.onosproject.cluster.NodeId;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.AsyncLeaderElector;
import org.onosproject.store.service.LeaderElectorBuilder;
import org.onosproject.store.service.Serializer;

/**
 * Default {@link org.onosproject.store.service.AsyncLeaderElector} builder.
 */
public class AtomixLeaderElectorBuilder extends LeaderElectorBuilder {
    private static final int MAX_RETRIES = 5;
    private final Atomix atomix;
    private final String group;
    private final NodeId localNodeId;

    public AtomixLeaderElectorBuilder(Atomix atomix, String group, NodeId localNodeId) {
        this.atomix = atomix;
        this.group = group;
        this.localNodeId = localNodeId;
    }

    @Override
    public AsyncLeaderElector build() {
        Serializer serializer = Serializer.using(KryoNamespaces.API);
        return new AtomixLeaderElector(atomix.<NodeId>leaderElectorBuilder(name())
            .withProtocol(MultiRaftProtocol.builder(group)
                .withRecoveryStrategy(Recovery.RECOVER)
                .withMaxRetries(MAX_RETRIES)
                .withMaxTimeout(Duration.ofMillis(electionTimeoutMillis()))
                .build())
            .withReadOnly(readOnly())
            .withCacheEnabled(relaxedReadConsistency())
            .withSerializer(new AtomixSerializerAdapter(serializer))
            .build()
            .async(), localNodeId);
    }
}