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
package org.onosproject.store.primitives.resources.impl;

import java.util.Arrays;
import java.util.Collection;

import io.atomix.protocols.raft.ReadConsistency;
import io.atomix.protocols.raft.cluster.MemberId;
import io.atomix.protocols.raft.impl.RaftServerContext;
import io.atomix.protocols.raft.protocol.RaftServerProtocol;
import io.atomix.protocols.raft.service.ServiceId;
import io.atomix.protocols.raft.service.ServiceType;
import io.atomix.protocols.raft.service.impl.DefaultCommit;
import io.atomix.protocols.raft.service.impl.DefaultServiceContext;
import io.atomix.protocols.raft.session.SessionId;
import io.atomix.protocols.raft.session.impl.RaftSessionContext;
import io.atomix.protocols.raft.storage.RaftStorage;
import io.atomix.protocols.raft.storage.snapshot.Snapshot;
import io.atomix.protocols.raft.storage.snapshot.SnapshotReader;
import io.atomix.protocols.raft.storage.snapshot.SnapshotStore;
import io.atomix.protocols.raft.storage.snapshot.SnapshotWriter;
import io.atomix.storage.StorageLevel;
import io.atomix.time.WallClockTimestamp;
import io.atomix.utils.concurrent.ThreadContext;
import org.junit.Test;
import org.onosproject.store.service.Task;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.mock;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.onosproject.store.primitives.resources.impl.AtomixWorkQueueOperations.ADD;
import static org.onosproject.store.primitives.resources.impl.AtomixWorkQueueOperations.TAKE;
import static org.onosproject.store.service.DistributedPrimitive.Type.WORK_QUEUE;

/**
 * Work queue service test.
 */
public class AtomixWorkQueueServiceTest {
    @Test
    public void testSnapshot() throws Exception {
        SnapshotStore store = new SnapshotStore(RaftStorage.newBuilder()
                .withPrefix("test")
                .withStorageLevel(StorageLevel.MEMORY)
                .build());
        Snapshot snapshot = store.newSnapshot(ServiceId.from(1), 2, new WallClockTimestamp());

        DefaultServiceContext context = mock(DefaultServiceContext.class);
        expect(context.serviceType()).andReturn(ServiceType.from(WORK_QUEUE.name())).anyTimes();
        expect(context.serviceName()).andReturn("test").anyTimes();
        expect(context.serviceId()).andReturn(ServiceId.from(1)).anyTimes();
        expect(context.executor()).andReturn(mock(ThreadContext.class)).anyTimes();

        RaftServerContext server = mock(RaftServerContext.class);
        expect(server.getProtocol()).andReturn(mock(RaftServerProtocol.class));

        replay(context, server);

        RaftSessionContext session = new RaftSessionContext(
                SessionId.from(1),
                MemberId.from("1"),
                "test",
                ServiceType.from(WORK_QUEUE.name()),
                ReadConsistency.LINEARIZABLE,
                5000,
                context,
                server);

        AtomixWorkQueueService service = new AtomixWorkQueueService();
        service.init(context);

        service.add(new DefaultCommit<>(
                2,
                ADD,
                new AtomixWorkQueueOperations.Add(Arrays.asList("Hello world!".getBytes())),
                session,
                System.currentTimeMillis()));

        try (SnapshotWriter writer = snapshot.openWriter()) {
            service.snapshot(writer);
        }

        snapshot.complete();

        service = new AtomixWorkQueueService();
        service.init(context);

        try (SnapshotReader reader = snapshot.openReader()) {
            service.install(reader);
        }

        Collection<Task<byte[]>> value = service.take(new DefaultCommit<>(
                2,
                TAKE,
                new AtomixWorkQueueOperations.Take(1),
                session,
                System.currentTimeMillis()));
        assertNotNull(value);
        assertEquals(1, value.size());
        assertArrayEquals("Hello world!".getBytes(), value.iterator().next().payload());
    }
}
