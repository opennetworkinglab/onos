/*
 * Copyright 2015 Open Networking Laboratory
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

import java.util.concurrent.atomic.AtomicLong;

/**
 * Test implementation of atomic counter.
 */
public final class TestAtomicCounter implements AtomicCounter {
    final AtomicLong value;

    private TestAtomicCounter() {
        value = new AtomicLong();
    }

    @Override
    public long incrementAndGet() {
        return value.incrementAndGet();
    }

    @Override
    public long getAndIncrement() {
        return value.getAndIncrement();
    }

    @Override
    public long getAndAdd(long delta) {
        return value.getAndAdd(delta);
    }

    @Override
    public long addAndGet(long delta) {
        return value.addAndGet(delta);
    }

    @Override
    public void set(long value) {
        this.value.set(value);
    }

    @Override
    public boolean compareAndSet(long expectedValue, long updateValue) {
        return value.compareAndSet(expectedValue, updateValue);
    }

    @Override
    public long get() {
        return value.get();
    }

    public static AtomicCounterBuilder builder() {
        return new Builder();
    }

    public static class Builder implements AtomicCounterBuilder {
        @Override
        public AtomicCounterBuilder withName(String name) {
            return this;
        }

        @Override
        public AtomicCounterBuilder withPartitionsDisabled() {
            return this;
        }

        @Override
        public AtomicCounterBuilder withMeteringDisabled() {
            return this;
        }

        @Override
        public AsyncAtomicCounter buildAsyncCounter() {
            throw new UnsupportedOperationException("Async Counter is not supported");
        }

        @Override
        public AtomicCounter build() {
            return new TestAtomicCounter();
        }
    }
}
