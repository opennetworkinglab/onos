/*
 * Copyright 2014-present Open Networking Laboratory
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
package org.onlab.util;

import org.junit.Test;
import org.onlab.junit.TestTools;

import java.util.Optional;
import java.util.concurrent.ThreadFactory;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.onlab.junit.TestTools.assertAfter;

/**
 * Test of the miscellaneous tools.
 */
public class ToolsTest {

    @Test
    public void fromHex() throws Exception {
        assertEquals(15, Tools.fromHex("0f"));
        assertEquals(16, Tools.fromHex("10"));
        assertEquals(65535, Tools.fromHex("ffff"));
        assertEquals(4096, Tools.fromHex("1000"));
        assertEquals(0xffffffffffffffffL, Tools.fromHex("ffffffffffffffff"));
    }

    @Test
    public void toHex() throws Exception {
        assertEquals("0f", Tools.toHex(15, 2));
        assertEquals("ffff", Tools.toHex(65535, 4));
        assertEquals("1000", Tools.toHex(4096, 4));
        assertEquals("000000000000000f", Tools.toHex(15));
        assertEquals("ffffffffffffffff", Tools.toHex(0xffffffffffffffffL));

    }

    @Test
    public void namedThreads() {
        ThreadFactory f = Tools.namedThreads("foo-%d");
        Thread t = f.newThread(() -> TestTools.print("yo"));
        assertTrue("wrong pattern", t.getName().startsWith("foo-"));
    }

    @Test
    public void groupedThreads() {
        ThreadFactory f = Tools.groupedThreads("foo/bar-me", "foo-%d");
        Thread t = f.newThread(() -> TestTools.print("yo"));
        assertTrue("wrong pattern", t.getName().startsWith("foo-bar-me-foo-"));
        assertTrue("wrong group", "foo/bar-me".equals(t.getThreadGroup().getName()));
    }

    @Test
    public void exceptionHandler() throws InterruptedException {
        ThreadFactory f = Tools.namedThreads("foo");
        Thread t = f.newThread(() -> {
            throw new IllegalStateException("BOOM!");
        });
        assertNotNull("thread should have exception handler", t.getUncaughtExceptionHandler());
        t.start();
        assertAfter(100, () -> assertEquals("incorrect thread state", Thread.State.TERMINATED, t.getState()));
    }

    @Test
    public void testOptionalStream() {
        Stream<Object> empty = Tools.stream(Optional.empty());
        assertThat(empty.count(), is(0L));

        String value = "value";
        Stream<String> stream = Tools.stream(Optional.of(value));
        assertThat(stream.allMatch(value::equals), is(true));
    }

}
