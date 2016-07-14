/*
 * Copyright 2016-present Open Networking Laboratory
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

package org.onosproject.store.service;

import com.google.common.collect.Lists;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.junit.TestTools;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Junit test class for TestDistributedSet.
 */
public class DistributedSetTest {

    protected InternalSetListener listener = new InternalSetListener();
    private static final String SETNAME = "set1";
    private TestDistributedSet<String> set1;
    private Set<String> set2;

    /**
     * Test setup before each test.
     */
    @Before
    public void setup() {
        set1 = new TestDistributedSet<>(SETNAME);
        set2 = new HashSet<>();

        set1.addListener(listener);
    }

    /**
     * Test cleanup after each test.
     */
    @After
    public void teardown() {
        set1.removeListener(listener);
    }

    /**
     * Basic tests against TestDistributedSet.
     */
    @Test
    public void basicTests() {
        set1.add("item1");
        assertEquals("The set name should match.", SETNAME, set1.name());
        assertEquals("The set primitive type should match.", DistributedPrimitive.Type.SET, set1.primitiveType());

        set1.add("item2");
        set1.add("item3");
        assertTrue("set1 should contain 3 items", set1.asDistributedSet().size() == 3);
        set1.remove("item1");
        set1.remove("item2");
        set1.remove("item3");
        assertTrue("set1 should be empty.", set1.asDistributedSet(0).isEmpty());
        validateEvents(SetEvent.Type.ADD, SetEvent.Type.ADD, SetEvent.Type.ADD,
                       SetEvent.Type.REMOVE, SetEvent.Type.REMOVE, SetEvent.Type.REMOVE);
    }

    /**
     * TestDistributedSet clear method test.
     */
    @Test
    public void testClear() {
        set1.add("item1");
        set1.add("item2");
        set1.add("item3");
        set1.clear();
        assertTrue("set1 should be empty.", set1.asDistributedSet().isEmpty());
        validateEvents(SetEvent.Type.ADD, SetEvent.Type.ADD, SetEvent.Type.ADD,
                       SetEvent.Type.REMOVE, SetEvent.Type.REMOVE, SetEvent.Type.REMOVE);
    }

    /**
     * TestDistributedSet addAll method test.
     */
    @Test
    public void testAddAll() {
        set2.add("item1");
        set2.add("item2");
        set2.add("item3");
        set1.addAll(set2);
        assertTrue("set1 should contain 3 items.", set1.asDistributedSet().size() == set2.size());
        validateEvents(SetEvent.Type.ADD, SetEvent.Type.ADD, SetEvent.Type.ADD);
    }

    /**
     * TestDistributedSet removeAll method test.
     */
    @Test
    public void testRemoveAll() {
        set1.add("item1");
        set1.add("item2");
        set1.add("item3");
        set2.add("item1");
        set1.removeAll(set2);
        assertTrue("set1 should contain 2 items.", set1.asDistributedSet().size() == 2);
        validateEvents(SetEvent.Type.ADD, SetEvent.Type.ADD, SetEvent.Type.ADD,
                       SetEvent.Type.REMOVE);
    }

    /**
     * TestDistributedSet retainAll method test.
     */
    @Test
    public void testRetainAll() {
        set1.add("item1");
        set1.add("item2");
        set1.add("item3");
        set2.add("item4");
        set1.retainAll(set2);
        assertTrue("set1 should be empty.", set1.asDistributedSet().isEmpty());
        validateEvents(SetEvent.Type.ADD, SetEvent.Type.ADD, SetEvent.Type.ADD,
                       SetEvent.Type.REMOVE, SetEvent.Type.REMOVE, SetEvent.Type.REMOVE);

        set1.add("item1");
        set1.add("item2");
        set1.add("item3");
        set2.add("item1");
        set1.retainAll(set2);
        assertTrue("set1 size should be 1.", set1.asDistributedSet().size() == 1);
        validateEvents(SetEvent.Type.ADD, SetEvent.Type.ADD, SetEvent.Type.ADD,
                       SetEvent.Type.REMOVE, SetEvent.Type.REMOVE);

    }

    /**
     * TestDistributedSet contains method test.
     */
    @Test
    public void testContains() {
        set1.add("item1");
        set1.add("item2");
        set1.add("item3");
        assertTrue("set1 should contain item3.", set1.asDistributedSet().contains("item3"));
    }

    /**
     * TestDistributedSet containsAll method test.
     */
    @Test
    public void testContainsAll() {
        set1.add("item1");
        set1.add("item2");
        set1.add("item3");
        set2.add("item2");
        set2.add("item3");
        assertTrue("set1 should contain all of set2.", set1.asDistributedSet().containsAll(set2));
    }

    /**
     * TestDistributedSet getAsImmutableSet method test.
     */
    @Test
    public void testGetAsImmutableSet() {
        set1.add("item1");
        set1.add("item2");
        set1.add("item3");
        try {
            assertEquals("set1 size should be 3.", set1.getAsImmutableSet().get().size(),
                         set1.asDistributedSet().size());
        } catch (Exception e) {
            fail("Expected exception. " + e.getMessage());
        }
    }

    /**
     * Method to validate that actual versus expected set events were
     * received correctly.
     *
     * @param types expected set events.
     */
    private void validateEvents(Enum... types) {
        TestTools.assertAfter(100, () -> {
            int i = 0;
            assertEquals("wrong events received", types.length, listener.events.size());
            for (SetEvent event : listener.events) {
                assertEquals("incorrect event type", types[i], event.type());
                i++;
            }
            listener.events.clear();
        });
    }

    /**
     * Listener class to test set events.
     */
    private class InternalSetListener implements SetEventListener<String> {
        protected List<SetEvent> events = Lists.newArrayList();

        @Override
        public void event(SetEvent event) {
            events.add(event);
        }
    }
}
