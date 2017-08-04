/*
 * Copyright 2015-present Open Networking Foundation
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

import static org.junit.Assert.*;

/**
 * Tests of the group thread factory.
 */
public class GroupedThreadFactoryTest {

    @Test
    public void basics() {
        GroupedThreadFactory a = GroupedThreadFactory.groupedThreadFactory("foo");
        GroupedThreadFactory b = GroupedThreadFactory.groupedThreadFactory("foo");
        assertSame("factories should be same", a, b);

        assertTrue("wrong toString", a.toString().contains("foo"));
        Thread t = a.newThread(() -> TestTools.print("yo"));
        assertSame("wrong group", a.threadGroup(), t.getThreadGroup());
    }

    @Test
    public void hierarchical() {
        GroupedThreadFactory a = GroupedThreadFactory.groupedThreadFactory("foo/bar");
        GroupedThreadFactory b = GroupedThreadFactory.groupedThreadFactory("foo/goo");
        GroupedThreadFactory p = GroupedThreadFactory.groupedThreadFactory("foo");

        assertSame("groups should be same", p.threadGroup(), a.threadGroup().getParent());
        assertSame("groups should be same", p.threadGroup(), b.threadGroup().getParent());

        assertEquals("wrong name", "foo/bar", a.threadGroup().getName());
        assertEquals("wrong name", "foo/goo", b.threadGroup().getName());
        assertEquals("wrong name", "foo", p.threadGroup().getName());
    }

}