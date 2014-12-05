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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import net.kuujo.copycat.internal.log.OperationEntry;
import net.kuujo.copycat.log.Entry;
import net.kuujo.copycat.log.Log;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.store.serializers.StoreSerializer;

import com.google.common.testing.EqualsTester;

/**
 * Test the MapDBLog implementation.
 */
public class MapDBLogTest {

    private static final StoreSerializer SERIALIZER = ClusterMessagingProtocol.DB_SERIALIZER;
    private static final Entry TEST_ENTRY1 = new OperationEntry(1, "test1");
    private static final Entry TEST_ENTRY2 = new OperationEntry(2, "test12");
    private static final Entry TEST_ENTRY3 = new OperationEntry(3, "test123");
    private static final Entry TEST_ENTRY4 = new OperationEntry(4, "test1234");

    private static final Entry TEST_SNAPSHOT_ENTRY = new OperationEntry(5, "snapshot");

    private static final long TEST_ENTRY1_SIZE = SERIALIZER.encode(TEST_ENTRY1).length;
    private static final long TEST_ENTRY2_SIZE = SERIALIZER.encode(TEST_ENTRY2).length;
    private static final long TEST_ENTRY3_SIZE = SERIALIZER.encode(TEST_ENTRY3).length;
    private static final long TEST_ENTRY4_SIZE = SERIALIZER.encode(TEST_ENTRY4).length;

    private static final long TEST_SNAPSHOT_ENTRY_SIZE = SERIALIZER.encode(TEST_SNAPSHOT_ENTRY).length;

    private String dbFileName;


    @Before
    public void setUp() throws Exception {
        File logFile = File.createTempFile("mapdbTest", null);
        dbFileName = logFile.getAbsolutePath();
    }

    @After
    public void tearDown() throws Exception {
        Files.deleteIfExists(new File(dbFileName).toPath());
        Files.deleteIfExists(new File(dbFileName + ".t").toPath());
        Files.deleteIfExists(new File(dbFileName + ".p").toPath());
    }

    @Test(expected = IllegalStateException.class)
    public void testAssertOpen() {
        Log log = new MapDBLog(dbFileName, SERIALIZER);
        log.size();
    }

    @Test
    public void testAppendEntry() throws IOException {
        Log log = new MapDBLog(dbFileName, SERIALIZER);
        log.open();
        log.appendEntry(TEST_ENTRY1);
        OperationEntry first = log.firstEntry();
        OperationEntry last = log.lastEntry();
        new EqualsTester()
            .addEqualityGroup(first, last, TEST_ENTRY1)
            .testEquals();
        Assert.assertEquals(TEST_ENTRY1_SIZE, log.size());
        Assert.assertEquals(1, log.firstIndex());
        Assert.assertEquals(1, log.lastIndex());
    }

    @Test
    public void testAppendEntries() throws IOException {
        Log log = new MapDBLog(dbFileName, SERIALIZER);
        log.open();
        log.appendEntries(TEST_ENTRY1, TEST_ENTRY2, TEST_ENTRY3);
        OperationEntry first = log.firstEntry();
        OperationEntry last = log.lastEntry();
        new EqualsTester()
            .addEqualityGroup(first, TEST_ENTRY1)
            .addEqualityGroup(last, TEST_ENTRY3)
            .testEquals();
        Assert.assertEquals(TEST_ENTRY1_SIZE + TEST_ENTRY2_SIZE, TEST_ENTRY3_SIZE, log.size());
        Assert.assertEquals(1, log.firstIndex());
        Assert.assertEquals(3, log.lastIndex());
        Assert.assertTrue(log.containsEntry(1));
        Assert.assertTrue(log.containsEntry(2));
    }

    @Test
    public void testDelete() throws IOException {
        Log log = new MapDBLog(dbFileName, SERIALIZER);
        log.open();
        log.appendEntries(TEST_ENTRY1, TEST_ENTRY2);
        log.delete();
        Assert.assertEquals(0, log.size());
        Assert.assertTrue(log.isEmpty());
        Assert.assertEquals(0, log.firstIndex());
        Assert.assertNull(log.firstEntry());
        Assert.assertEquals(0, log.lastIndex());
        Assert.assertNull(log.lastEntry());
    }

    @Test
    public void testGetEntries() throws IOException {
        Log log = new MapDBLog(dbFileName, SERIALIZER);
        log.open();
        log.appendEntries(TEST_ENTRY1, TEST_ENTRY2, TEST_ENTRY3, TEST_ENTRY4);
        Assert.assertEquals(
                TEST_ENTRY1_SIZE +
                TEST_ENTRY2_SIZE +
                TEST_ENTRY3_SIZE +
                TEST_ENTRY4_SIZE, log.size());

        List<Entry> entries = log.getEntries(2, 3);
        new EqualsTester()
            .addEqualityGroup(log.getEntry(4), TEST_ENTRY4)
            .addEqualityGroup(entries.get(0), TEST_ENTRY2)
            .addEqualityGroup(entries.get(1), TEST_ENTRY3)
            .testEquals();
    }

    @Test
    public void testRemoveAfter() throws IOException {
        Log log = new MapDBLog(dbFileName, SERIALIZER);
        log.open();
        log.appendEntries(TEST_ENTRY1, TEST_ENTRY2, TEST_ENTRY3, TEST_ENTRY4);
        log.removeAfter(1);
        Assert.assertEquals(TEST_ENTRY1_SIZE, log.size());
        new EqualsTester()
            .addEqualityGroup(log.firstEntry(), log.lastEntry(), TEST_ENTRY1)
            .testEquals();
    }

    @Test
    public void testAddAfterRemove() throws IOException {
        Log log = new MapDBLog(dbFileName, SERIALIZER);
        log.open();
        log.appendEntries(TEST_ENTRY1, TEST_ENTRY2, TEST_ENTRY3, TEST_ENTRY4);
        log.removeAfter(1);
        log.appendEntry(TEST_ENTRY4);
        Assert.assertEquals(TEST_ENTRY1_SIZE + TEST_ENTRY4_SIZE, log.size());
        new EqualsTester()
            .addEqualityGroup(log.firstEntry(), TEST_ENTRY1)
            .addEqualityGroup(log.lastEntry(), TEST_ENTRY4)
            .addEqualityGroup(log.size(), TEST_ENTRY1_SIZE + TEST_ENTRY4_SIZE)
            .testEquals();
    }

    @Test
    public void testClose() throws IOException {
        Log log = new MapDBLog(dbFileName, SERIALIZER);
        Assert.assertFalse(log.isOpen());
        log.open();
        Assert.assertTrue(log.isOpen());
        log.close();
        Assert.assertFalse(log.isOpen());
    }

    @Test
    public void testReopen() throws IOException {
        Log log = new MapDBLog(dbFileName, SERIALIZER);
        log.open();
        log.appendEntries(TEST_ENTRY1, TEST_ENTRY2, TEST_ENTRY3, TEST_ENTRY4);
        log.close();
        log.open();

        new EqualsTester()
            .addEqualityGroup(log.firstEntry(), TEST_ENTRY1)
            .addEqualityGroup(log.getEntry(2), TEST_ENTRY2)
            .addEqualityGroup(log.lastEntry(), TEST_ENTRY4)
            .addEqualityGroup(log.size(),
                    TEST_ENTRY1_SIZE +
                    TEST_ENTRY2_SIZE +
                    TEST_ENTRY3_SIZE +
                    TEST_ENTRY4_SIZE)
            .testEquals();
    }

    @Test
    public void testCompact() throws IOException {
        Log log = new MapDBLog(dbFileName, SERIALIZER);
        log.open();
        log.appendEntries(TEST_ENTRY1, TEST_ENTRY2, TEST_ENTRY3, TEST_ENTRY4);
        log.compact(3, TEST_SNAPSHOT_ENTRY);
        new EqualsTester()
        .addEqualityGroup(log.firstEntry(), TEST_SNAPSHOT_ENTRY)
        .addEqualityGroup(log.lastEntry(), TEST_ENTRY4)
        .addEqualityGroup(log.size(),
                TEST_SNAPSHOT_ENTRY_SIZE +
                TEST_ENTRY4_SIZE)
        .testEquals();
    }
}
