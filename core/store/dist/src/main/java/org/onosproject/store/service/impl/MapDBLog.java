/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onosproject.store.service.impl;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Verify.verifyNotNull;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentNavigableMap;

import net.kuujo.copycat.log.Entry;
import net.kuujo.copycat.log.Log;
import net.kuujo.copycat.log.LogIndexOutOfBoundsException;

import org.mapdb.Atomic;
import org.mapdb.BTreeMap;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;
import org.mapdb.TxBlock;
import org.mapdb.TxMaker;
import org.onosproject.store.serializers.StoreSerializer;
import org.slf4j.Logger;

/**
 * MapDB based log implementation.
 */
public class MapDBLog implements Log {

    private final Logger log = getLogger(getClass());

    private final File dbFile;
    private TxMaker txMaker;
    private final StoreSerializer serializer;
    private static final String LOG_NAME = "log";
    private static final String SIZE_FIELD_NAME = "size";

    private int cacheSize = 256;

    public MapDBLog(String dbFileName, StoreSerializer serializer) {
        this.dbFile = new File(dbFileName);
        this.serializer = serializer;
    }

    @Override
    public void open() throws IOException {
        txMaker = DBMaker
                .newFileDB(dbFile)
                .mmapFileEnableIfSupported()
                .cacheSize(cacheSize)
                .makeTxMaker();
        log.info("Raft log file: {}", dbFile.getCanonicalPath());
    }

    @Override
    public void close() throws IOException {
        assertIsOpen();
        txMaker.close();
        txMaker = null;
    }

    @Override
    public boolean isOpen() {
        return txMaker != null;
    }

    protected void assertIsOpen() {
        checkState(isOpen(), "The log is not currently open.");
    }

    @Override
    public long appendEntry(Entry entry) {
        checkArgument(entry != null, "expecting non-null entry");
        return appendEntries(entry).get(0);
    }

    @Override
    public List<Long> appendEntries(Entry... entries) {
        checkArgument(entries != null, "expecting non-null entries");
        return appendEntries(Arrays.asList(entries));
    }

    @Override
    public synchronized List<Long> appendEntries(List<Entry> entries) {
        assertIsOpen();
        checkArgument(entries != null, "expecting non-null entries");
        final List<Long> indices = new ArrayList<>(entries.size());

        txMaker.execute(new TxBlock() {
            @Override
            public void tx(DB db) {
                BTreeMap<Long, byte[]> log = getLogMap(db);
                Atomic.Long size = db.getAtomicLong(SIZE_FIELD_NAME);
                long nextIndex = log.isEmpty() ? 1 : log.lastKey() + 1;
                long addedBytes = 0;
                for (Entry entry : entries) {
                    byte[] entryBytes = verifyNotNull(serializer.encode(entry),
                                                      "Writing LogEntry %s failed", nextIndex);
                    log.put(nextIndex, entryBytes);
                    addedBytes += entryBytes.length;
                    indices.add(nextIndex);
                    nextIndex++;
                }
                size.addAndGet(addedBytes);
            }
        });

        return indices;
    }

    @Override
    public boolean containsEntry(long index) {
        assertIsOpen();
        DB db = txMaker.makeTx();
        try {
            BTreeMap<Long, byte[]> log = getLogMap(db);
            return log.containsKey(index);
        } finally {
            db.close();
        }
    }

    @Override
    public void delete() throws IOException {
        assertIsOpen();
        txMaker.execute(new TxBlock() {
            @Override
            public void tx(DB db) {
                BTreeMap<Long, byte[]> log = getLogMap(db);
                Atomic.Long size = db.getAtomicLong(SIZE_FIELD_NAME);
                log.clear();
                size.set(0);
            }
        });
    }

    @Override
    public <T extends Entry> T firstEntry() {
        assertIsOpen();
        DB db = txMaker.makeTx();
        try {
            BTreeMap<Long, byte[]> log = getLogMap(db);
            return log.isEmpty() ? null : verifyNotNull(decodeEntry(log.firstEntry().getValue()));
        } finally {
            db.close();
        }
    }

    @Override
    public long firstIndex() {
        assertIsOpen();
        DB db = txMaker.makeTx();
        try {
            BTreeMap<Long, byte[]> log = getLogMap(db);
            return log.isEmpty() ? 0 : log.firstKey();
        } finally {
            db.close();
        }
    }

    private <T extends Entry> T decodeEntry(final byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        return serializer.decode(bytes.clone());
    }

    @Override
    public <T extends Entry> List<T> getEntries(long from, long to) {
        assertIsOpen();
        DB db = txMaker.makeTx();
        try {
            BTreeMap<Long, byte[]> log = getLogMap(db);
            if (log.isEmpty()) {
                throw new LogIndexOutOfBoundsException("Log is empty");
            } else if (from < log.firstKey()) {
                throw new LogIndexOutOfBoundsException("From index out of bounds.");
            } else if (to > log.lastKey()) {
                throw new LogIndexOutOfBoundsException("To index out of bounds.");
            }
            List<T> entries = new ArrayList<>((int) (to - from + 1));
            for (long i = from; i <= to; i++) {
                T entry = verifyNotNull(decodeEntry(log.get(i)), "LogEntry %s was null", i);
                entries.add(entry);
            }
            return entries;
        } finally {
            db.close();
        }
    }

    @Override
    public <T extends Entry> T getEntry(long index) {
        assertIsOpen();
        DB db = txMaker.makeTx();
        try {
            BTreeMap<Long, byte[]> log = getLogMap(db);
            byte[] entryBytes = log.get(index);
            return entryBytes == null ? null : verifyNotNull(decodeEntry(entryBytes),
                                                             "LogEntry %s was null", index);
        } finally {
            db.close();
        }
    }

    @Override
    public boolean isEmpty() {
        assertIsOpen();
        DB db = txMaker.makeTx();
        try {
            BTreeMap<Long, byte[]> log = getLogMap(db);
            return log.isEmpty();
        } finally {
            db.close();
        }
    }

    @Override
    public <T extends Entry> T lastEntry() {
        assertIsOpen();
        DB db = txMaker.makeTx();
        try {
            BTreeMap<Long, byte[]> log = getLogMap(db);
            return log.isEmpty() ? null : verifyNotNull(decodeEntry(log.lastEntry().getValue()));
        } finally {
            db.close();
        }
    }

    @Override
    public long lastIndex() {
        assertIsOpen();
        DB db = txMaker.makeTx();
        try {
            BTreeMap<Long, byte[]> log = getLogMap(db);
            return log.isEmpty() ? 0 : log.lastKey();
        } finally {
            db.close();
        }
    }

    @Override
    public void removeAfter(long index) {
        assertIsOpen();
        txMaker.execute(new TxBlock() {
            @Override
            public void tx(DB db) {
                BTreeMap<Long, byte[]> log = getLogMap(db);
                Atomic.Long size = db.getAtomicLong(SIZE_FIELD_NAME);
                long removedBytes = 0;
                ConcurrentNavigableMap<Long, byte[]> tailMap = log.tailMap(index, false);
                Iterator<Map.Entry<Long, byte[]>> it = tailMap.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<Long, byte[]> entry = it.next();
                    removedBytes += entry.getValue().length;
                    it.remove();
                }
                size.addAndGet(-removedBytes);
            }
        });
    }

    @Override
    public long size() {
        assertIsOpen();
        DB db = txMaker.makeTx();
        try {
            Atomic.Long size = db.getAtomicLong(SIZE_FIELD_NAME);
            return size.get();
        } finally {
            db.close();
        }
    }

    @Override
    public void sync() throws IOException {
        assertIsOpen();
    }

    @Override
    public void compact(long index, Entry entry) throws IOException {

        assertIsOpen();
        txMaker.execute(new TxBlock() {
            @Override
            public void tx(DB db) {
                BTreeMap<Long, byte[]> log = getLogMap(db);
                Atomic.Long size = db.getAtomicLong(SIZE_FIELD_NAME);
                ConcurrentNavigableMap<Long, byte[]> headMap = log.headMap(index);
                Iterator<Map.Entry<Long, byte[]>> it = headMap.entrySet().iterator();

                long deletedBytes = 0;
                while (it.hasNext()) {
                    Map.Entry<Long, byte[]> e = it.next();
                    deletedBytes += e.getValue().length;
                    it.remove();
                }
                size.addAndGet(-deletedBytes);
                byte[] entryBytes = verifyNotNull(serializer.encode(entry));
                byte[] existingEntry = log.put(index, entryBytes);
                if (existingEntry != null) {
                    size.addAndGet(entryBytes.length - existingEntry.length);
                } else {
                    size.addAndGet(entryBytes.length);
                }
                db.compact();
            }
        });
    }

    private BTreeMap<Long, byte[]> getLogMap(DB db) {
        return db.createTreeMap(LOG_NAME)
                    .valuesOutsideNodesEnable()
                    .keySerializerWrap(Serializer.LONG)
                    .valueSerializer(Serializer.BYTE_ARRAY)
                    .makeOrGet();
    }
}
