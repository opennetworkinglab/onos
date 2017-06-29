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

import io.atomix.protocols.raft.service.ServiceId;
import io.atomix.protocols.raft.service.impl.DefaultCommit;
import io.atomix.protocols.raft.session.impl.RaftSessionContext;
import io.atomix.protocols.raft.storage.RaftStorage;
import io.atomix.protocols.raft.storage.snapshot.Snapshot;
import io.atomix.protocols.raft.storage.snapshot.SnapshotReader;
import io.atomix.protocols.raft.storage.snapshot.SnapshotStore;
import io.atomix.protocols.raft.storage.snapshot.SnapshotWriter;
import io.atomix.storage.StorageLevel;
import io.atomix.time.WallClockTimestamp;
import org.junit.Test;
import org.onlab.util.Match;
import org.onosproject.store.service.Versioned;

import static org.easymock.EasyMock.mock;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotNull;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentMapOperations.GET;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentMapOperations.UPDATE_AND_GET;

/**
 * Consistent map service test.
 */
public class AtomixConsistentMapServiceTest {
    @Test
    @SuppressWarnings("unchecked")
    public void testSnapshot() throws Exception {
        SnapshotStore store = new SnapshotStore(RaftStorage.newBuilder()
                .withPrefix("test")
                .withStorageLevel(StorageLevel.MEMORY)
                .build());
        Snapshot snapshot = store.newSnapshot(ServiceId.from(1), 2, new WallClockTimestamp());

        AtomixConsistentMapService service = new AtomixConsistentMapService();
        service.updateAndGet(new DefaultCommit<>(
                2,
                UPDATE_AND_GET,
                new AtomixConsistentMapOperations.UpdateAndGet("foo", "Hello world!".getBytes(), Match.ANY, Match.ANY),
                mock(RaftSessionContext.class),
                System.currentTimeMillis()));

        try (SnapshotWriter writer = snapshot.openWriter()) {
            service.snapshot(writer);
        }

        snapshot.complete();

        service = new AtomixConsistentMapService();
        try (SnapshotReader reader = snapshot.openReader()) {
            service.install(reader);
        }

        Versioned<byte[]> value = service.get(new DefaultCommit<>(
                2,
                GET,
                new AtomixConsistentMapOperations.Get("foo"),
                mock(RaftSessionContext.class),
                System.currentTimeMillis()));
        assertNotNull(value);
        assertArrayEquals("Hello world!".getBytes(), value.value());
    }
}
