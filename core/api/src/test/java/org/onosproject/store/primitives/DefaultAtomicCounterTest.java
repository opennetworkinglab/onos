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
import org.onosproject.store.service.AsyncAtomicCounter;
import org.onosproject.store.service.StorageException;

import java.util.concurrent.CompletableFuture;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.onosproject.store.primitives.TestingCompletableFutures.ErrorState.NONE;


/**
 * Unit tests for the DefaultAtomicCounter class.
 */

public class DefaultAtomicCounterTest {

    private static final long INITIAL_VALUE = 33L;
    private static final long ADDED_VALUE = 44L;

    private DefaultAtomicCounter atomicCounter;


    private DefaultAtomicCounter create() {
        AsyncAtomicCounter atomicCounter = AsyncAtomicCounterAdapter.builder().build();
        DefaultAtomicCounter counter = new DefaultAtomicCounter(atomicCounter, 1000);
        counter.set(INITIAL_VALUE);
        assertThat(counter.get(), is(INITIAL_VALUE));
        return counter;
    }

    @Before
    public void setUpCounter() {
        atomicCounter = create();
    }

    @Test
    public void testConstruction() {
        assertThat(atomicCounter, notNullValue());
    }

    @Test
    public void testIncrements() {
        long beforeIncrement = atomicCounter.getAndIncrement();
        assertThat(beforeIncrement, is(INITIAL_VALUE));
        assertThat(atomicCounter.get(), is(INITIAL_VALUE + 1));

        atomicCounter.set(INITIAL_VALUE);
        long afterIncrement = atomicCounter.incrementAndGet();
        assertThat(afterIncrement, is(INITIAL_VALUE + 1));
        assertThat(atomicCounter.get(), is(INITIAL_VALUE + 1));
    }

    @Test
    public void testAdds() {
        long beforeIncrement = atomicCounter.getAndAdd(ADDED_VALUE);
        assertThat(beforeIncrement, is(INITIAL_VALUE));
        assertThat(atomicCounter.get(), is(INITIAL_VALUE + ADDED_VALUE));

        atomicCounter.set(INITIAL_VALUE);
        long afterIncrement = atomicCounter.addAndGet(ADDED_VALUE);
        assertThat(afterIncrement, is(INITIAL_VALUE + ADDED_VALUE));
        assertThat(atomicCounter.get(), is(INITIAL_VALUE + ADDED_VALUE));
    }

    @Test
    public void testCompareAndSet() {
        boolean compareTrue = atomicCounter.compareAndSet(INITIAL_VALUE, ADDED_VALUE);
        assertThat(compareTrue, is(true));

        boolean compareFalse = atomicCounter.compareAndSet(INITIAL_VALUE, ADDED_VALUE);
        assertThat(compareFalse, is(false));
    }



    class AtomicCounterWithErrors extends AsyncAtomicCounterAdapter {

        TestingCompletableFutures.ErrorState errorState = NONE;

        void setErrorState(TestingCompletableFutures.ErrorState errorState) {
            this.errorState = errorState;
        }

        AtomicCounterWithErrors() {
            super();
        }

        @Override
        public CompletableFuture<Long> get() {
            return TestingCompletableFutures.createFuture(errorState);
        }
    }

    @Test(expected = StorageException.Timeout.class)
    public void testTimeout() {
        AtomicCounterWithErrors atomicCounter = new AtomicCounterWithErrors();
        atomicCounter.setErrorState(TestingCompletableFutures.ErrorState.TIMEOUT_EXCEPTION);
        DefaultAtomicCounter counter = new DefaultAtomicCounter(atomicCounter, 1000);

        counter.get();
    }

    @Test(expected = StorageException.Interrupted.class)
    public void testInterrupted() {
        AtomicCounterWithErrors atomicCounter = new AtomicCounterWithErrors();
        atomicCounter.setErrorState(TestingCompletableFutures.ErrorState.INTERRUPTED_EXCEPTION);
        DefaultAtomicCounter counter = new DefaultAtomicCounter(atomicCounter, 1000);

        counter.get();
    }

    @Test(expected = StorageException.class)
    public void testExecutionError() {
        AtomicCounterWithErrors atomicCounter = new AtomicCounterWithErrors();
        atomicCounter.setErrorState(TestingCompletableFutures.ErrorState.EXECUTION_EXCEPTION);
        DefaultAtomicCounter counter = new DefaultAtomicCounter(atomicCounter, 1000);

        counter.get();
    }
}