/*
 * Copyright 2014 Open Networking Laboratory
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

/**
 * A lock is a tool for controlling access to a shared resource by multiple processes.
 * Commonly, a lock provides exclusive access to a resource such as a network device
 * or exclusive permission to a controller to perform a particular role such as serve
 * as the master controller for a device.
 * At any given time one and only process can acquire the lock.
 */
public interface Lock {

    /**
     * Returns the path this lock will be used to guard from concurrent access.
     * @return path.
     */
    String path();

    /**
     * Acquires the lock.
     * If the lock is not available then the caller thread becomes
     * disabled for thread scheduling purposes and lies dormant until
     * the lock has been acquired.
     * <p>
     * Locks are reentrant. A thread invoking this method multiple times
     * without an intervening unlock or lease expiration must invoke unlock()
     * the same number of times before the lock is released (unless the lease expires).
     * When this method is invoked for a lock that is already acquired,
     * the lease time will be set to the maximum of the remaining lease time
     * from the previous invocation, or leaseDurationMillis.
     * @param leaseDurationMillis the number of milliseconds to hold the
     * lock after granting it, before automatically releasing it if it hasn't
     * already been released by invoking unlock(). Must be in the range
     * (0, LockManager.MAX_LEASE_MILLIS]
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    void lock(int leaseDurationMillis) throws InterruptedException;

    /**
     * Acquires the lock asynchronously.
     * @param leaseDurationMillis leaseDurationMillis the number of milliseconds the lock
     * will be reserved before it becomes available for others.
     * @return Future that can be used for blocking until lock is acquired.
     */
    CompletableFuture<Void> lockAsync(int leaseDurationMillis);

    /**
     * Acquires the lock only if it is free at the time of invocation.
     * @param leaseDurationMillis the number of milliseconds the must be
     * locked after it is granted, before automatically releasing it if it hasn't
     * already been released by an invocation of unlock(). Must be in the range
     * (0, LockManager.MAX_LEASE_MILLIS]
     * @return true if the lock was acquired and false otherwise
     */
    boolean tryLock(int leaseDurationMillis);

    /**
     * Acquires the lock if it is free within the given waiting
     * time and the current thread has not been interrupted.
     * @param waitTimeMillis the maximum time (in milliseconds) to wait for the lock
     * @param leaseDurationMillis the number of milliseconds to hold the
     * lock after granting it, before automatically releasing it if it hasn't
     * already been released by invoking unlock(Object). Must be in the range
     * (0, LockManager.MAX_LEASE_MILLIS]
     * @return true if the lock was acquired and false if the waiting time
     * elapsed before the lock was acquired
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    boolean tryLock(int waitTimeMillis, int leaseDurationMillis) throws InterruptedException;

    /**
     * Returns true if this Lock instance currently holds the lock.
     * @return true if this instance is the owner of the lock.
     */
    boolean isLocked();

    /**
     * Returns the epoch for this lock.
     * If this lock is currently locked i.e. isLocked() returns true, epoch signifies the logical time
     * when the lock was acquired. The concept of epoch lets one come up with a global ordering for all
     * lock acquisition events
     * @return epoch
     */
    long epoch();

    /**
     * Releases the lock.
     */
    void unlock();

    /**
     * Extends the expiration time for a lock that is currently owned
     * by a specified duration. The new expiration time is computed
     * by adding the specified duration to the current time. If this point
     * in time is earlier than the existing expiration time then this method
     * has no effect.
     * @param leaseDurationMillis extension duration.
     * @return true if successfully extended expiration, false if attempt to
     * extend expiration fails or if the path is currently not locked by this instance.
     */
    boolean extendExpiration(int leaseDurationMillis);
}
