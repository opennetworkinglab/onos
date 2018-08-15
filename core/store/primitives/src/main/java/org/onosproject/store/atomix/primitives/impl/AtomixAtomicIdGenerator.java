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
package org.onosproject.store.atomix.primitives.impl;

import java.util.concurrent.CompletableFuture;

import org.onosproject.store.service.AsyncAtomicIdGenerator;

import static org.onosproject.store.atomix.primitives.impl.AtomixFutures.adaptFuture;

/**
 * Atomix atomic ID generator.
 */
public class AtomixAtomicIdGenerator implements AsyncAtomicIdGenerator {
    private final io.atomix.core.idgenerator.AsyncAtomicIdGenerator atomixIdGenerator;

    public AtomixAtomicIdGenerator(io.atomix.core.idgenerator.AsyncAtomicIdGenerator atomixIdGenerator) {
        this.atomixIdGenerator = atomixIdGenerator;
    }

    @Override
    public String name() {
        return atomixIdGenerator.name();
    }

    @Override
    public CompletableFuture<Long> nextId() {
        return adaptFuture(atomixIdGenerator.nextId());
    }
}
