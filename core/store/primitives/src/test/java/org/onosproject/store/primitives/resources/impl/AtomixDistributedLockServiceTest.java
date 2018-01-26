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
package org.onosproject.store.primitives.resources.impl;

import java.util.concurrent.atomic.AtomicLong;

import io.atomix.protocols.raft.ReadConsistency;
import io.atomix.protocols.raft.cluster.MemberId;
import io.atomix.protocols.raft.impl.RaftContext;
import io.atomix.protocols.raft.operation.OperationType;
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
import io.atomix.utils.concurrent.AtomixThreadFactory;
import io.atomix.utils.concurrent.SingleThreadContextFactory;
import io.atomix.utils.concurrent.ThreadContext;
import org.junit.Test;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.mock;
import static org.easymock.EasyMock.replay;
import static org.onosproject.store.service.DistributedPrimitive.Type.LEADER_ELECTOR;

/**
 * Distributed lock service test.
 */
public class AtomixDistributedLockServiceTest {
    @Test
    public void testSnapshot() throws Exception {
        SnapshotStore store = new SnapshotStore(RaftStorage.newBuilder()
            .withPrefix("test")
            .withStorageLevel(StorageLevel.MEMORY)
            .build());
        Snapshot snapshot = store.newSnapshot(ServiceId.from(1), "test", 2, new WallClockTimestamp());

        AtomicLong index = new AtomicLong();
        DefaultServiceContext context = mock(DefaultServiceContext.class);
        expect(context.serviceType()).andReturn(ServiceType.from(LEADER_ELECTOR.name())).anyTimes();
        expect(context.serviceName()).andReturn("test").anyTimes();
        expect(context.serviceId()).andReturn(ServiceId.from(1)).anyTimes();
        expect(context.executor()).andReturn(mock(ThreadContext.class)).anyTimes();
        expect(context.currentIndex()).andReturn(index.get()).anyTimes();
        expect(context.currentOperation()).andReturn(OperationType.COMMAND).anyTimes();

        RaftContext server = mock(RaftContext.class);
        expect(server.getProtocol()).andReturn(mock(RaftServerProtocol.class));

        replay(context, server);

        AtomixDistributedLockService service = new AtomixDistributedLockService();
        service.init(context);

        RaftSessionContext session = new RaftSessionContext(
            SessionId.from(1),
            MemberId.from("1"),
            "test",
            ServiceType.from(LEADER_ELECTOR.name()),
            ReadConsistency.LINEARIZABLE,
            100,
            5000,
            System.currentTimeMillis(),
            context,
            server,
            new SingleThreadContextFactory(new AtomixThreadFactory()));
        session.open();

        service.lock(new DefaultCommit<>(
            index.incrementAndGet(),
            AtomixDistributedLockOperations.LOCK,
            new AtomixDistributedLockOperations.Lock(1, 0),
            session,
            System.currentTimeMillis()));

        try (SnapshotWriter writer = snapshot.openWriter()) {
            service.snapshot(writer);
        }

        snapshot.complete();

        service = new AtomixDistributedLockService();
        try (SnapshotReader reader = snapshot.openReader()) {
            service.install(reader);
        }

        service.unlock(new DefaultCommit<>(
            index.incrementAndGet(),
            AtomixDistributedLockOperations.UNLOCK,
            new AtomixDistributedLockOperations.Unlock(1),
            session,
            System.currentTimeMillis()));
    }
}
