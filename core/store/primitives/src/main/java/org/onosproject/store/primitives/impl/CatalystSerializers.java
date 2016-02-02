/*
 * Copyright 2016 Open Networking Laboratory
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

import io.atomix.catalyst.serializer.CatalystSerializable;
import io.atomix.catalyst.serializer.Serializer;
import io.atomix.catalyst.serializer.TypeSerializerFactory;
import io.atomix.copycat.client.Query;
import io.atomix.manager.state.GetResource;
import io.atomix.manager.state.GetResourceKeys;
import io.atomix.resource.ResourceQuery;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Scanner;

import org.onlab.util.Match;
import org.onosproject.cluster.NodeId;
import org.onosproject.event.Change;
import org.onosproject.store.primitives.resources.impl.AtomixConsistentMapCommands;
import org.onosproject.store.primitives.resources.impl.AtomixConsistentMapState;
import org.onosproject.store.primitives.resources.impl.AtomixLeaderElectorCommands;
import org.onosproject.store.primitives.resources.impl.CommitResult;
import org.onosproject.store.primitives.resources.impl.MapEntryUpdateResult;
import org.onosproject.store.primitives.resources.impl.MapUpdate;
import org.onosproject.store.primitives.resources.impl.PrepareResult;
import org.onosproject.store.primitives.resources.impl.RollbackResult;
import org.onosproject.store.primitives.resources.impl.TransactionId;
import org.onosproject.store.primitives.resources.impl.TransactionalMapUpdate;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.MapEvent;
import org.onosproject.store.service.Versioned;

import com.google.common.base.Throwables;
import com.google.common.collect.Maps;

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
                                MapUpdate.class,
                                MapUpdate.Type.class,
                                TransactionalMapUpdate.class,
                                TransactionId.class,
                                PrepareResult.class,
                                CommitResult.class,
                                RollbackResult.class,
                                AtomixConsistentMapCommands.Get.class,
                                AtomixConsistentMapCommands.ContainsKey.class,
                                AtomixConsistentMapCommands.ContainsValue.class,
                                AtomixConsistentMapCommands.Size.class,
                                AtomixConsistentMapCommands.IsEmpty.class,
                                AtomixConsistentMapCommands.KeySet.class,
                                AtomixConsistentMapCommands.EntrySet.class,
                                AtomixConsistentMapCommands.Values.class,
                                AtomixConsistentMapCommands.UpdateAndGet.class,
                                AtomixConsistentMapCommands.TransactionPrepare.class,
                                AtomixConsistentMapCommands.TransactionCommit.class,
                                AtomixConsistentMapCommands.TransactionRollback.class,
                                AtomixLeaderElectorCommands.GetLeadership.class,
                                AtomixLeaderElectorCommands.GetAllLeaderships.class,
                                AtomixLeaderElectorCommands.GetElectedTopics.class,
                                AtomixLeaderElectorCommands.Run.class,
                                AtomixLeaderElectorCommands.Withdraw.class,
                                AtomixLeaderElectorCommands.Anoint.class,
                                GetResource.class,
                                GetResourceKeys.class,
                                ResourceQuery.class,
                                Query.ConsistencyLevel.class));
        // ONOS classes
        serializer.register(Change.class, factory);
        serializer.register(NodeId.class, factory);
        serializer.register(Match.class, factory);
        serializer.register(MapEntryUpdateResult.class, factory);
        serializer.register(MapEntryUpdateResult.Status.class, factory);
        serializer.register(TransactionalMapUpdate.class, factory);
        serializer.register(PrepareResult.class, factory);
        serializer.register(CommitResult.class, factory);
        serializer.register(RollbackResult.class, factory);
        serializer.register(TransactionId.class, factory);
        serializer.register(MapUpdate.class, factory);
        serializer.register(Versioned.class, factory);
        serializer.register(MapEvent.class, factory);
        serializer.register(Maps.immutableEntry("a", "b").getClass(), factory);
        serializer.register(AtomixConsistentMapState.class, factory);

        serializer.register(ResourceQuery.class, factory);
        serializer.register(GetResource.class, factory);
        serializer.register(GetResourceKeys.class, factory);

        // ConsistentMap
        serializer.register(AtomixConsistentMapCommands.UpdateAndGet.class, factory);
        serializer.register(AtomixConsistentMapCommands.Clear.class);
        serializer.register(AtomixConsistentMapCommands.Listen.class);
        serializer.register(AtomixConsistentMapCommands.Unlisten.class);
        serializer.register(AtomixConsistentMapCommands.Get.class);
        serializer.register(AtomixConsistentMapCommands.ContainsKey.class);
        serializer.register(AtomixConsistentMapCommands.ContainsValue.class);
        serializer.register(AtomixConsistentMapCommands.EntrySet.class);
        serializer.register(AtomixConsistentMapCommands.IsEmpty.class);
        serializer.register(AtomixConsistentMapCommands.KeySet.class);
        serializer.register(AtomixConsistentMapCommands.Size.class);
        serializer.register(AtomixConsistentMapCommands.Values.class);
        serializer.register(AtomixConsistentMapCommands.TransactionPrepare.class);
        serializer.register(AtomixConsistentMapCommands.TransactionCommit.class);
        serializer.register(AtomixConsistentMapCommands.TransactionRollback.class);
        // LeaderElector
        serializer.register(AtomixLeaderElectorCommands.Run.class, factory);
        serializer.register(AtomixLeaderElectorCommands.Withdraw.class, factory);
        serializer.register(AtomixLeaderElectorCommands.Anoint.class, factory);
        serializer.register(AtomixLeaderElectorCommands.GetElectedTopics.class, factory);
        serializer.register(AtomixLeaderElectorCommands.GetElectedTopics.class, factory);
        serializer.register(AtomixLeaderElectorCommands.GetLeadership.class, factory);
        serializer.register(AtomixLeaderElectorCommands.GetAllLeaderships.class, factory);
        serializer.register(AtomixLeaderElectorCommands.Listen.class);
        serializer.register(AtomixLeaderElectorCommands.Unlisten.class);
        // Atomix types
        try {
            ClassLoader cl = CatalystSerializable.class.getClassLoader();
            Enumeration<URL> urls = cl.getResources(
                    String.format("META-INF/services/%s", CatalystSerializable.class.getName()));
            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();
                try (Scanner scanner = new Scanner(url.openStream(), "UTF-8")) {
                    scanner.useDelimiter("\n").forEachRemaining(line -> {
                        if (!line.trim().startsWith("#")) {
                            line = line.trim();
                            if (line.length() > 0) {
                                try {
                                    serializer.register(cl.loadClass(line));
                                } catch (ClassNotFoundException e) {
                                    Throwables.propagate(e);
                                }
                            }
                        }
                    });
                }
            }
        } catch (IOException e) {
            Throwables.propagate(e);
        }
        return serializer;
    }
}
