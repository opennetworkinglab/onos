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

import java.util.HashMap;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

import com.google.common.collect.Sets;
import org.junit.Test;
import org.onosproject.cluster.PartitionId;
import org.onosproject.store.primitives.DistributedPrimitiveCreator;
import org.onosproject.store.primitives.PartitionService;
import org.onosproject.store.primitives.TransactionId;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.AsyncConsistentMap;
import org.onosproject.store.service.ConsistentMapBuilder;
import org.onosproject.store.service.MapEventListener;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.mock;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertSame;

/**
 * Transaction manager test.
 */
public class TransactionManagerTest {

    @Test
    public void testTransactionMapCaching() throws Exception {
        AsyncConsistentMap asyncMap = mock(AsyncConsistentMap.class);
        expect(asyncMap.name()).andReturn("foo");
        expect(asyncMap.addListener(anyObject(MapEventListener.class), anyObject(Executor.class)))
                .andReturn(CompletableFuture.completedFuture(null)).anyTimes();
        asyncMap.addStatusChangeListener(anyObject(Consumer.class));
        expectLastCall().anyTimes();
        expect(asyncMap.entrySet()).andReturn(CompletableFuture.completedFuture(new HashMap<>().entrySet())).anyTimes();

        ConsistentMapBuilder mapBuilder = mock(ConsistentMapBuilder.class);
        expect(mapBuilder.withName(anyString())).andReturn(mapBuilder).anyTimes();
        expect(mapBuilder.withSerializer(anyObject(Serializer.class))).andReturn(mapBuilder).anyTimes();
        expect(mapBuilder.buildAsyncMap()).andReturn(asyncMap).anyTimes();

        DistributedPrimitiveCreator primitiveCreator = mock(DistributedPrimitiveCreator.class);
        expect(primitiveCreator.newAsyncConsistentMap(anyString(), anyObject(Serializer.class)))
                .andReturn(asyncMap).anyTimes();

        StorageService storageService = mock(StorageService.class);
        expect(storageService.consistentMapBuilder()).andReturn(mapBuilder);

        PartitionService partitionService = mock(PartitionService.class);
        Set<PartitionId> partitionIds = Sets.newHashSet(PartitionId.from(1), PartitionId.from(2), PartitionId.from(3));
        expect(partitionService.getAllPartitionIds())
                .andReturn(partitionIds).anyTimes();
        expect(partitionService.getNumberOfPartitions())
                .andReturn(partitionIds.size()).anyTimes();
        expect(partitionService.getDistributedPrimitiveCreator(anyObject(PartitionId.class)))
                .andReturn(primitiveCreator).anyTimes();

        replay(storageService, partitionService, asyncMap, primitiveCreator, mapBuilder);

        TransactionManager transactionManager = new TransactionManager(storageService, partitionService);
        TransactionId transactionId = TransactionId.from(UUID.randomUUID().toString());
        TransactionCoordinator transactionCoordinator = new TransactionCoordinator(transactionId, transactionManager);
        Serializer serializer = Serializer.using(KryoNamespaces.API);

        PartitionedTransactionalMap<String, String> transactionalMap1 = (PartitionedTransactionalMap)
                transactionManager.getTransactionalMap("foo", serializer, transactionCoordinator);
        PartitionedTransactionalMap<String, String> transactionalMap2 = (PartitionedTransactionalMap)
                transactionManager.getTransactionalMap("foo", serializer, transactionCoordinator);

        assertSame(transactionalMap1.partitions.get(PartitionId.from(1)).transaction.transactionalObject,
                transactionalMap2.partitions.get(PartitionId.from(1)).transaction.transactionalObject);
        assertSame(transactionalMap1.partitions.get(PartitionId.from(2)).transaction.transactionalObject,
                transactionalMap2.partitions.get(PartitionId.from(2)).transaction.transactionalObject);
        assertSame(transactionalMap1.partitions.get(PartitionId.from(3)).transaction.transactionalObject,
                transactionalMap2.partitions.get(PartitionId.from(3)).transaction.transactionalObject);
    }

}
