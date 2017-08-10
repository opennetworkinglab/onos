/*
 * Copyright 2017-present Open Networking Foundation
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

import java.util.Optional;

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
import org.onosproject.store.service.DocumentPath;
import org.onosproject.store.service.Ordering;
import org.onosproject.store.service.Versioned;

import static org.easymock.EasyMock.mock;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotNull;
import static org.onosproject.store.primitives.resources.impl.AtomixDocumentTreeOperations.GET;
import static org.onosproject.store.primitives.resources.impl.AtomixDocumentTreeOperations.UPDATE;

/**
 * Document tree service test.
 */
public class AtomixDocumentTreeServiceTest {

    @Test
    public void testNaturalOrderedSnapshot() throws Exception {
        testSnapshot(Ordering.NATURAL);
    }

    @Test
    public void testInsertionOrderedSnapshot() throws Exception {
        testSnapshot(Ordering.INSERTION);
    }

    private void testSnapshot(Ordering ordering) throws Exception {
        SnapshotStore store = new SnapshotStore(RaftStorage.newBuilder()
                .withPrefix("test")
                .withStorageLevel(StorageLevel.MEMORY)
                .build());
        Snapshot snapshot = store.newSnapshot(ServiceId.from(1), 2, new WallClockTimestamp());

        AtomixDocumentTreeService service = new AtomixDocumentTreeService(ordering);
        service.update(new DefaultCommit<>(
                2,
                UPDATE,
                new AtomixDocumentTreeOperations.Update(
                        DocumentPath.from("root|foo"),
                        Optional.of("Hello world!".getBytes()),
                        Match.any(),
                        Match.ifNull()),
                mock(RaftSessionContext.class),
                System.currentTimeMillis()));

        try (SnapshotWriter writer = snapshot.openWriter()) {
            service.snapshot(writer);
        }

        snapshot.complete();

        service = new AtomixDocumentTreeService(ordering);
        try (SnapshotReader reader = snapshot.openReader()) {
            service.install(reader);
        }

        Versioned<byte[]> value = service.get(new DefaultCommit<>(
                2,
                GET,
                new AtomixDocumentTreeOperations.Get(DocumentPath.from("root|foo")),
                mock(RaftSessionContext.class),
                System.currentTimeMillis()));
        assertNotNull(value);
        assertArrayEquals("Hello world!".getBytes(), value.value());
    }
}
