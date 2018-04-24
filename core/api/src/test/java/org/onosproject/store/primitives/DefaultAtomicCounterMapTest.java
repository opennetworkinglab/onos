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

package org.onosproject.store.primitives;

import org.junit.Before;
import org.junit.Test;
import org.onosproject.store.service.ConsistentMapException;

import java.util.concurrent.CompletableFuture;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.onosproject.store.primitives.TestingCompletableFutures.ErrorState.NONE;

/**
 * Unit tests for the DefaultAtomicCounter class.
 */

public class DefaultAtomicCounterMapTest {

    private static final String KEY1 = "myKey1";
    private static final long VALUE1 = 444L;
    private static final long DELTA1 = 555L;

    private DefaultAtomicCounterMap<String> atomicCounterMap;

    private DefaultAtomicCounterMap<String> createMap() {
        AsyncAtomicCounterMapAdapter<String> asyncMap = new AsyncAtomicCounterMapAdapter<>();
        DefaultAtomicCounterMap<String> map = new DefaultAtomicCounterMap<>(asyncMap, 1000L);
        assertThat(map, notNullValue());
        assertThat(map.isEmpty(), is(true));
        return map;
    }

    @Before
    public void setUpMap() {
        atomicCounterMap = createMap();
    }

    @Test
    public void testConstruction() {
        assertThat(atomicCounterMap.size(), is(0));
    }

    @Test
    public void testPutAndGet() {
        atomicCounterMap.put(KEY1, VALUE1);
        long value = atomicCounterMap.get(KEY1);
        assertThat(value, is(VALUE1));
    }

    @Test
    public void testGetAndIncrement() {
        atomicCounterMap.put(KEY1, VALUE1);
        Long beforeIncrement = atomicCounterMap.getAndIncrement(KEY1);
        assertThat(beforeIncrement, is(VALUE1));
        Long afterIncrement = atomicCounterMap.get(KEY1);
        assertThat(afterIncrement, is(VALUE1 + 1));
    }

    @Test
    public void testIncrementAndGet() {
        atomicCounterMap.put(KEY1, VALUE1);
        Long afterIncrement = atomicCounterMap.incrementAndGet(KEY1);
        assertThat(afterIncrement, is(VALUE1 + 1));
    }

    @Test
    public void testGetAndDecrement() {
        atomicCounterMap.put(KEY1, VALUE1);
        Long beforeDecrement = atomicCounterMap.getAndDecrement(KEY1);
        assertThat(beforeDecrement, is(VALUE1));
        Long afterDecrement = atomicCounterMap.get(KEY1);
        assertThat(afterDecrement, is(VALUE1 - 1));
    }

    @Test
    public void testDecrementAndGet() {
        atomicCounterMap.put(KEY1, VALUE1);
        Long afterIncrement = atomicCounterMap.decrementAndGet(KEY1);
        assertThat(afterIncrement, is(VALUE1 - 1));
    }

    @Test
    public void testGetAndAdd() {
        atomicCounterMap.put(KEY1, VALUE1);
        Long beforeIncrement = atomicCounterMap.getAndAdd(KEY1, DELTA1);
        assertThat(beforeIncrement, is(VALUE1));
        Long afterIncrement = atomicCounterMap.get(KEY1);
        assertThat(afterIncrement, is(VALUE1 + DELTA1));
    }

    @Test
    public void testAddAndGet() {
        atomicCounterMap.put(KEY1, VALUE1);
        Long afterIncrement = atomicCounterMap.addAndGet(KEY1, DELTA1);
        assertThat(afterIncrement, is(VALUE1 + DELTA1));
    }

    @Test
    public void testPutIfAbsent() {
        atomicCounterMap.putIfAbsent(KEY1, VALUE1);
        Long afterIncrement = atomicCounterMap.addAndGet(KEY1, DELTA1);
        assertThat(afterIncrement, is(VALUE1 + DELTA1));
    }

    @Test
    public void testClear() {
        atomicCounterMap.putIfAbsent(KEY1, VALUE1);
        assertThat(atomicCounterMap.size(), is(1));
        atomicCounterMap.clear();
        assertThat(atomicCounterMap.size(), is(0));
    }

    @Test
    public void testReplace() {
        atomicCounterMap.putIfAbsent(KEY1, VALUE1);

        boolean replaced = atomicCounterMap.replace(KEY1, VALUE1, VALUE1 * 2);
        assertThat(replaced, is(true));
        Long afterReplace = atomicCounterMap.get(KEY1);
        assertThat(afterReplace, is(VALUE1 * 2));

        boolean notReplaced = atomicCounterMap.replace(KEY1, VALUE1, VALUE1 * 2);
        assertThat(notReplaced, is(false));
        Long afterNotReplaced = atomicCounterMap.get(KEY1);
        assertThat(afterNotReplaced, is(VALUE1 * 2));
    }

    @Test
    public void testRemove() {
        atomicCounterMap.putIfAbsent(KEY1, VALUE1);
        assertThat(atomicCounterMap.size(), is(1));
        atomicCounterMap.remove(KEY1);
        assertThat(atomicCounterMap.size(), is(0));
    }

    @Test
    public void testRemoveVale() {
        atomicCounterMap.putIfAbsent(KEY1, VALUE1);
        assertThat(atomicCounterMap.size(), is(1));
        atomicCounterMap.remove(KEY1, VALUE1 * 2);
        assertThat(atomicCounterMap.size(), is(1));
        atomicCounterMap.remove(KEY1, VALUE1);
        assertThat(atomicCounterMap.size(), is(0));
    }

    class AtomicCounterMapWithErrors<K> extends AsyncAtomicCounterMapAdapter<K> {

        TestingCompletableFutures.ErrorState errorState = NONE;

        void setErrorState(TestingCompletableFutures.ErrorState errorState) {
            this.errorState = errorState;
        }

        AtomicCounterMapWithErrors() {
            super();
        }

        @Override
        public CompletableFuture<Long> get(K key) {
            return TestingCompletableFutures.createFuture(errorState);
        }
    }

    @Test(expected = ConsistentMapException.Timeout.class)
    public void testTimeout() {
        AtomicCounterMapWithErrors<String> atomicCounterMap =
                new AtomicCounterMapWithErrors<>();
        atomicCounterMap.setErrorState(TestingCompletableFutures.ErrorState.TIMEOUT_EXCEPTION);
        DefaultAtomicCounterMap<String> map =
                new DefaultAtomicCounterMap<>(atomicCounterMap, 1000);

        map.get(KEY1);
    }


    @Test(expected = ConsistentMapException.Interrupted.class)
    public void testInterrupted() {
        AtomicCounterMapWithErrors<String> atomicCounterMap =
                new AtomicCounterMapWithErrors<>();
        atomicCounterMap.setErrorState(TestingCompletableFutures.ErrorState.INTERRUPTED_EXCEPTION);
        DefaultAtomicCounterMap<String> map =
                new DefaultAtomicCounterMap<>(atomicCounterMap, 1000);

        map.get(KEY1);
    }

    @Test(expected = ConsistentMapException.class)
    public void testExecutionError() {
        AtomicCounterMapWithErrors<String> atomicCounterMap =
                new AtomicCounterMapWithErrors<>();
        atomicCounterMap.setErrorState(TestingCompletableFutures.ErrorState.EXECUTION_EXCEPTION);
        DefaultAtomicCounterMap<String> map =
                new DefaultAtomicCounterMap<>(atomicCounterMap, 1000);

        map.get(KEY1);
    }

}