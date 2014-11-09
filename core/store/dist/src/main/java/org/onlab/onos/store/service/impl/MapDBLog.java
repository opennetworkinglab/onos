package org.onlab.onos.store.service.impl;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
import org.onlab.onos.store.serializers.StoreSerializer;
import org.slf4j.Logger;

import com.google.common.collect.Lists;

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

    public MapDBLog(String dbFileName, StoreSerializer serializer) {
        this.dbFile = new File(dbFileName);
        this.serializer = serializer;
    }

    @Override
    public void open() throws IOException {
        txMaker = DBMaker
                .newFileDB(dbFile)
                .makeTxMaker();
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
    public List<Long> appendEntries(List<Entry> entries) {
        assertIsOpen();
        checkArgument(entries != null, "expecting non-null entries");
        final List<Long> indices = Lists.newArrayList();

        txMaker.execute(new TxBlock() {
            @Override
            public void tx(DB db) {
                BTreeMap<Long, byte[]> log = getLogMap(db);
                Atomic.Long size = db.getAtomicLong(SIZE_FIELD_NAME);
                long nextIndex = log.isEmpty() ? 1 : log.lastKey() + 1;
                for (Entry entry : entries) {
                    byte[] entryBytes = serializer.encode(entry);
                    log.put(nextIndex, entryBytes);
                    size.addAndGet(entryBytes.length);
                    indices.add(nextIndex);
                    nextIndex++;
                }
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
            return log.isEmpty() ? null : serializer.decode(log.firstEntry().getValue());
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
                T entry = serializer.decode(log.get(i));
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
            return entryBytes == null ? null : serializer.decode(entryBytes);
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
            return log.isEmpty() ? null : serializer.decode(log.lastEntry().getValue());
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
                long startIndex = index + 1;
                long endIndex = log.lastKey();
                for (long i = startIndex; i <= endIndex; ++i) {
                    byte[] entryBytes = log.remove(i);
                    size.addAndGet(-1L * entryBytes.length);
                }
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
                long deletedBytes = headMap.keySet().stream().mapToLong(i -> log.remove(i).length).sum();
                size.addAndGet(-1 * deletedBytes);
                byte[] entryBytes = serializer.encode(entry);
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
