/*
 * Copyright 2014-present Open Networking Foundation
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableSet;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadFactory;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
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
        assertEquals("0xffffffffffffffff", Tools.toHexWithPrefix(0xffffffffffffffffL));

    }

    @Test
    public void getBytesUtf8() {
        assertThat(Tools.getBytesUtf8("Hi!"),
                   is(equalTo(new byte[] {0x48, 0x69, 0x21})));
    }

    @Test
    public void toStringUtf8() {
        assertThat(Tools.toStringUtf8(new byte[] {0x48, 0x69, 0x21}),
                   is(equalTo("Hi!")));
    }

    @Test
    public void copyOf() {
        byte[] input = new byte[] {1, 2, 3};
        assertThat(Tools.copyOf(input), is(equalTo(input)));
        assertNotSame(input, Tools.copyOf(input));
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
    public void minPriority() {
        ThreadFactory f = Tools.minPriority(Tools.namedThreads("foo-%d"));
        Thread t = f.newThread(() -> TestTools.print("yo"));
        assertThat(t.getPriority(), is(equalTo(Thread.MIN_PRIORITY)));
    }

    @Test
    public void maxPriority() {
        ThreadFactory f = Tools.maxPriority(Tools.namedThreads("foo-%d"));
        Thread t = f.newThread(() -> TestTools.print("yo"));
        assertThat(t.getPriority(), is(equalTo(Thread.MAX_PRIORITY)));
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
    public void testIsNullOrEmpty() {
        assertTrue(Tools.isNullOrEmpty(null));
        assertTrue(Tools.isNullOrEmpty(Collections.emptyList()));
    }

    @Test(expected = ItemNotFoundException.class)
    public void testNullIsNotFoundThrow() {
        Tools.nullIsNotFound(null, "Not found!");
        fail("Should've thrown some thing");
    }

    @Test
    public void testNullIsNotFound() {
        String input = "Foo";
        String output = Tools.nullIsNotFound(input, "Not found!");
        assertEquals(input, output);
        assertSame(input, output);
    }

    @Test(expected = ItemNotFoundException.class)
    public void testEmptyIsNotFoundNullThrow() {
        Tools.emptyIsNotFound(null, "Not found!");
        fail("Should've thrown some thing");
    }

    @Test(expected = ItemNotFoundException.class)
    public void testEmptyIsNotFoundEmptyThrow() {
        Tools.emptyIsNotFound(Collections.emptySet(), "Not found!");
        fail("Should've thrown some thing");
    }

    @Test
    public void testEmptyIsNotFound() {
        Set<String> input = ImmutableSet.of("Foo", "Bar");
        Set<String> output = Tools.emptyIsNotFound(input, "Not found!");
        assertEquals(input, output);
        assertSame(input, output);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullIsIllegalThrow() {
        Tools.nullIsIllegal(null, "Not found!");
        fail("Should've thrown some thing");
    }

    @Test
    public void testNullIsIllegal() {
        String input = "Foo";
        String output = Tools.nullIsIllegal(input, "Not found!");
        assertEquals(input, output);
        assertSame(input, output);
    }

    @Test
    public void testReadTreeFromStream() throws Exception {
        InputStream stream = new ByteArrayInputStream(Tools.getBytesUtf8("{\"foo\" : \"bar\"}"));
        ObjectNode obj = Tools.readTreeFromStream(new ObjectMapper(), stream);
        assertTrue(obj.has("foo"));
        assertThat(obj.get("foo").asText(), is("bar"));
    }

    @Test
    public void testDictonary() {
        Map<String, Object> map = new Hashtable<>();
        map.put("foo", "bar");
        map.put("one", Integer.valueOf(1));
        map.put("empty", "");
        map.put("enabled", "true");
        map.put("Enabled", true);
        map.put("disabled", "disabled");
        map.put("Disabled", false);
        Dictionary<?, ?> dict = (Dictionary<?, ?>) map;

        assertThat(Tools.get(dict, "foo"), is(equalTo("bar")));
        assertThat(Tools.get(dict, "don't exist"), is(nullValue()));
        assertThat(Tools.get(dict, "one"), is(equalTo("1")));
        assertThat(Tools.get(dict, "empty"), is(nullValue()));

        assertThat(Tools.getIntegerProperty(dict, "one"), is(equalTo(1)));
        assertThat(Tools.getIntegerProperty(dict, "empty"), is(nullValue()));
        assertThat(Tools.getIntegerProperty(dict, "foo"), is(nullValue()));

        assertThat(Tools.getIntegerProperty(dict, "one", 2), is(equalTo(1)));
        assertThat(Tools.getIntegerProperty(dict, "empty", 2), is(equalTo(2)));

        assertThat(Tools.isPropertyEnabled(dict, "enabled"), is(equalTo(Boolean.TRUE)));
        assertThat(Tools.isPropertyEnabled(dict, "Enabled"), is(equalTo(Boolean.TRUE)));
        assertThat(Tools.isPropertyEnabled(dict, "disabled"), is(equalTo(Boolean.FALSE)));
        assertThat(Tools.isPropertyEnabled(dict, "Disabled"), is(equalTo(Boolean.FALSE)));
        assertThat(Tools.isPropertyEnabled(dict, "empty"), is(nullValue()));

        assertThat(Tools.isPropertyEnabled(dict, "enabled", false), is(true));
        assertThat(Tools.isPropertyEnabled(dict, "disabled", true), is(false));
        assertThat(Tools.isPropertyEnabled(dict, "empty", false), is(false));

        assertThat(Tools.getLongProperty(dict, "one"), is(1L));
        assertThat(Tools.getLongProperty(dict, "foo"), is(nullValue()));
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
