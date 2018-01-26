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
package org.onosproject.store.primitives.resources.impl;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import io.atomix.protocols.raft.proxy.RaftProxy;
import io.atomix.protocols.raft.service.RaftService;
import org.junit.Test;
import org.onosproject.store.service.Version;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Raft lock test.
 */
public class AtomixDistributedLockTest extends AtomixTestBase<AtomixDistributedLock> {
    @Override
    protected RaftService createService() {
        return new AtomixDistributedLockService();
    }

    @Override
    protected AtomixDistributedLock createPrimitive(RaftProxy proxy) {
        return new AtomixDistributedLock(proxy);
    }

    /**
     * Tests locking and unlocking a lock.
     */
    @Test
    public void testLockUnlock() throws Throwable {
        AtomixDistributedLock lock = newPrimitive("test-lock-unlock");
        lock.lock().join();
        lock.unlock().join();
    }

    /**
     * Tests releasing a lock when the client's session is closed.
     */
    @Test
    public void testReleaseOnClose() throws Throwable {
        AtomixDistributedLock lock1 = newPrimitive("test-lock-release-on-close");
        AtomixDistributedLock lock2 = newPrimitive("test-lock-release-on-close");
        lock1.lock().join();
        CompletableFuture<Version> future = lock2.lock();
        lock1.close();
        future.join();
    }

    /**
     * Tests attempting to acquire a lock.
     */
    @Test
    public void testTryLockFail() throws Throwable {
        AtomixDistributedLock lock1 = newPrimitive("test-try-lock-fail");
        AtomixDistributedLock lock2 = newPrimitive("test-try-lock-fail");

        lock1.lock().join();

        assertFalse(lock2.tryLock().join().isPresent());
    }

    /**
     * Tests attempting to acquire a lock.
     */
    @Test
    public void testTryLockSucceed() throws Throwable {
        AtomixDistributedLock lock = newPrimitive("test-try-lock-succeed");
        assertTrue(lock.tryLock().join().isPresent());
    }

    /**
     * Tests attempting to acquire a lock with a timeout.
     */
    @Test
    public void testTryLockFailWithTimeout() throws Throwable {
        AtomixDistributedLock lock1 = newPrimitive("test-try-lock-fail-with-timeout");
        AtomixDistributedLock lock2 = newPrimitive("test-try-lock-fail-with-timeout");

        lock1.lock().join();

        assertFalse(lock2.tryLock(Duration.ofSeconds(1)).join().isPresent());
    }

    /**
     * Tests attempting to acquire a lock with a timeout.
     */
    @Test
    public void testTryLockSucceedWithTimeout() throws Throwable {
        AtomixDistributedLock lock1 = newPrimitive("test-try-lock-succeed-with-timeout");
        AtomixDistributedLock lock2 = newPrimitive("test-try-lock-succeed-with-timeout");

        lock1.lock().join();

        CompletableFuture<Optional<Version>> future = lock2.tryLock(Duration.ofSeconds(1));
        lock1.unlock().join();
        assertTrue(future.join().isPresent());
    }

    /**
     * Tests unlocking a lock with a blocking call in the event thread.
     */
    @Test
    public void testBlockingUnlock() throws Throwable {
        AtomixDistributedLock lock1 = newPrimitive("test-blocking-unlock");
        AtomixDistributedLock lock2 = newPrimitive("test-blocking-unlock");

        lock1.lock().thenRun(() -> {
            lock1.unlock().join();
        }).join();

        lock2.lock().join();
    }
}
