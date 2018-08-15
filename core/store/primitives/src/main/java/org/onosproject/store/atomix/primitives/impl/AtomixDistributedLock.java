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

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.onosproject.store.service.AsyncDistributedLock;
import org.onosproject.store.service.Version;

import static org.onosproject.store.atomix.primitives.impl.AtomixFutures.adaptFuture;

/**
 * Atomix distributed lock.
 */
public class AtomixDistributedLock implements AsyncDistributedLock {
    private final io.atomix.core.lock.AsyncAtomicLock atomixLock;

    public AtomixDistributedLock(io.atomix.core.lock.AsyncAtomicLock atomixLock) {
        this.atomixLock = atomixLock;
    }

    @Override
    public String name() {
        return atomixLock.name();
    }

    @Override
    public CompletableFuture<Version> lock() {
        return adaptFuture(atomixLock.lock()).thenApply(this::toVersion);
    }

    @Override
    public CompletableFuture<Optional<Version>> tryLock() {
        return adaptFuture(atomixLock.tryLock()).thenApply(optional -> optional.map(this::toVersion));
    }

    @Override
    public CompletableFuture<Optional<Version>> tryLock(Duration timeout) {
        return adaptFuture(atomixLock.tryLock(timeout)).thenApply(optional -> optional.map(this::toVersion));
    }

    @Override
    public CompletableFuture<Void> unlock() {
        return adaptFuture(atomixLock.unlock());
    }

    private Version toVersion(io.atomix.utils.time.Version version) {
        return new Version(version.value());
    }
}
