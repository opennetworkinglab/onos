/*
 * Copyright 2017-present Open Networking Laboratory
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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Test implementation of atomic ID generator.
 */
public final class TestAtomicIdGenerator implements AsyncAtomicIdGenerator {
    final AtomicLong value;

    @Override
    public String name() {
        return null;
    }

    private TestAtomicIdGenerator() {
        value = new AtomicLong();
    }

    @Override
    public CompletableFuture<Long> nextId() {
        return CompletableFuture.completedFuture(value.incrementAndGet());
    }

    public static AtomicIdGeneratorBuilder builder() {
        return new Builder();
    }

    public static class Builder extends AtomicIdGeneratorBuilder {
        @Override
        public TestAtomicIdGenerator build() {
            return new TestAtomicIdGenerator();
        }
    }
}
