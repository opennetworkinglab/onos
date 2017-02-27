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
package org.onosproject.store.primitives.impl;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import io.atomix.catalyst.serializer.Serializer;
import io.atomix.catalyst.serializer.TypeSerializerFactory;
import io.atomix.manager.util.ResourceManagerTypeResolver;
import io.atomix.variables.internal.LongCommands;
import org.onlab.util.Match;
import org.onosproject.cluster.Leader;
import org.onosproject.cluster.Leadership;
import org.onosproject.cluster.NodeId;
import org.onosproject.event.Change;
import org.onosproject.store.primitives.MapUpdate;
import org.onosproject.store.primitives.TransactionId;
import org.onosproject.store.primitives.resources.impl.AtomixConsistentMapCommands;
import org.onosproject.store.primitives.resources.impl.AtomixConsistentMapFactory;
import org.onosproject.store.primitives.resources.impl.AtomixConsistentMultimapCommands;
import org.onosproject.store.primitives.resources.impl.AtomixConsistentSetMultimapFactory;
import org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapCommands;
import org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapFactory;
import org.onosproject.store.primitives.resources.impl.AtomixDocumentTreeCommands;
import org.onosproject.store.primitives.resources.impl.AtomixDocumentTreeFactory;
import org.onosproject.store.primitives.resources.impl.AtomixLeaderElectorCommands;
import org.onosproject.store.primitives.resources.impl.AtomixLeaderElectorFactory;
import org.onosproject.store.primitives.resources.impl.AtomixWorkQueueCommands;
import org.onosproject.store.primitives.resources.impl.AtomixWorkQueueFactory;
import org.onosproject.store.primitives.resources.impl.CommitResult;
import org.onosproject.store.primitives.resources.impl.DocumentTreeUpdateResult;
import org.onosproject.store.primitives.resources.impl.MapEntryUpdateResult;
import org.onosproject.store.primitives.resources.impl.PrepareResult;
import org.onosproject.store.primitives.resources.impl.RollbackResult;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.DocumentPath;
import org.onosproject.store.service.DocumentTreeEvent;
import org.onosproject.store.service.MapEvent;
import org.onosproject.store.service.TransactionLog;
import org.onosproject.store.service.MultimapEvent;
import org.onosproject.store.service.Task;
import org.onosproject.store.service.Versioned;
import org.onosproject.store.service.WorkQueueStats;

import java.util.Arrays;
import java.util.Optional;

/**
 * Serializer utility for Atomix Catalyst.
 */
public final class CatalystSerializers {

    private CatalystSerializers() {
    }

    public static Serializer getSerializer() {
        Serializer serializer = new Serializer();
        TypeSerializerFactory factory =
                new DefaultCatalystTypeSerializerFactory(
                        org.onosproject.store.service.Serializer.using(Arrays.asList((KryoNamespaces.API)),
                                                                       MapEntryUpdateResult.class,
                                                                       MapEntryUpdateResult.Status.class,
                                                                       Transaction.State.class,
                                                                       PrepareResult.class,
                                                                       CommitResult.class,
                                                                       DocumentPath.class,
                                                                       DocumentTreeUpdateResult.class,
                                                                       DocumentTreeUpdateResult.Status.class,
                                                                       DocumentTreeEvent.class,
                                                                       DocumentTreeEvent.Type.class,
                                                                       RollbackResult.class));
        // ONOS classes
        serializer.register(Change.class, factory);
        serializer.register(Leader.class, factory);
        serializer.register(Leadership.class, factory);
        serializer.register(NodeId.class, factory);
        serializer.register(Match.class, factory);
        serializer.register(MapEntryUpdateResult.class, factory);
        serializer.register(MapEntryUpdateResult.Status.class, factory);
        serializer.register(Transaction.State.class, factory);
        serializer.register(PrepareResult.class, factory);
        serializer.register(CommitResult.class, factory);
        serializer.register(RollbackResult.class, factory);
        serializer.register(TransactionId.class, factory);
        serializer.register(MapUpdate.class, factory);
        serializer.register(MapUpdate.Type.class, factory);
        serializer.register(TransactionLog.class, factory);
        serializer.register(Versioned.class, factory);
        serializer.register(MapEvent.class, factory);
        serializer.register(MultimapEvent.class, factory);
        serializer.register(MultimapEvent.Type.class, factory);
        serializer.register(Task.class, factory);
        serializer.register(WorkQueueStats.class, factory);
        serializer.register(DocumentPath.class, factory);
        serializer.register(DocumentTreeUpdateResult.class, factory);
        serializer.register(DocumentTreeUpdateResult.Status.class, factory);
        serializer.register(DocumentTreeEvent.class, factory);
        serializer.register(Maps.immutableEntry("a", "b").getClass(), factory);
        serializer.register(ImmutableList.of().getClass(), factory);
        serializer.register(ImmutableList.of("a").getClass(), factory);
        serializer.register(Arrays.asList().getClass(), factory);
        serializer.register(HashMultiset.class, factory);
        serializer.register(Optional.class, factory);

        serializer.resolve(new LongCommands.TypeResolver());
        serializer.resolve(new AtomixConsistentMapCommands.TypeResolver());
        serializer.resolve(new AtomixLeaderElectorCommands.TypeResolver());
        serializer.resolve(new AtomixWorkQueueCommands.TypeResolver());
        serializer.resolve(new AtomixDocumentTreeCommands.TypeResolver());
        serializer.resolve(new ResourceManagerTypeResolver());
        serializer.resolve(new AtomixConsistentTreeMapCommands.TypeResolver());
        serializer.resolve(new AtomixConsistentMultimapCommands.TypeResolver());

        serializer.registerClassLoader(AtomixConsistentMapFactory.class)
                .registerClassLoader(AtomixLeaderElectorFactory.class)
                .registerClassLoader(AtomixWorkQueueFactory.class)
                .registerClassLoader(AtomixDocumentTreeFactory.class)
                .registerClassLoader(AtomixConsistentTreeMapFactory.class)
                .registerClassLoader(AtomixConsistentSetMultimapFactory.class);

        return serializer;
    }
}
