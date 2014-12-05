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
package org.onosproject.store.service.impl;

import static com.google.common.base.Verify.verify;
import static org.slf4j.LoggerFactory.getLogger;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.joda.time.DateTime;
import org.onosproject.cluster.ClusterService;
import org.onosproject.store.service.DatabaseException;
import org.onosproject.store.service.DatabaseService;
import org.onosproject.store.service.Lock;
import org.onosproject.store.service.VersionedValue;
import org.slf4j.Logger;

/**
 * A distributed lock implementation.
 */
public class DistributedLock implements Lock {

    private final Logger log = getLogger(getClass());

    private final DistributedLockManager lockManager;
    private final DatabaseService databaseService;
    private final String path;
    private DateTime lockExpirationTime;
    private AtomicBoolean isLocked = new AtomicBoolean(false);
    private volatile long epoch = 0;
    private byte[] lockId;

    public DistributedLock(
            String path,
            DatabaseService databaseService,
            ClusterService clusterService,
            DistributedLockManager lockManager) {

        this.path = path;
        this.databaseService = databaseService;
        this.lockManager = lockManager;
        this.lockId =
                (UUID.randomUUID().toString() + "::" +
                        clusterService.getLocalNode().id().toString()).
                        getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public String path() {
        return path;
    }

    @Override
    public void lock(int leaseDurationMillis) throws InterruptedException {
        try {
            lockAsync(leaseDurationMillis).get();
        } catch (ExecutionException e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    public CompletableFuture<Void> lockAsync(int leaseDurationMillis) {
        try {
            if (isLocked() || tryLock(leaseDurationMillis)) {
                return CompletableFuture.<Void>completedFuture(null);
            }
            return lockManager.lockIfAvailable(this, leaseDurationMillis);
        } catch (DatabaseException e) {
            CompletableFuture<Void> lockFuture = new CompletableFuture<>();
            lockFuture.completeExceptionally(e);
            return lockFuture;
        }
    }

    @Override
    public boolean tryLock(int leaseDurationMillis) {
        if (databaseService.putIfAbsent(
                DistributedLockManager.ONOS_LOCK_TABLE_NAME,
                path,
                lockId)) {
            VersionedValue vv =
                    databaseService.get(DistributedLockManager.ONOS_LOCK_TABLE_NAME, path);
            verify(Arrays.equals(vv.value(), lockId));
            epoch = vv.version();
            isLocked.set(true);
            lockExpirationTime = DateTime.now().plusMillis(leaseDurationMillis);
            return true;
        }
        return false;
    }

    @Override
    public boolean tryLock(
            int waitTimeMillis,
            int leaseDurationMillis) throws InterruptedException {
        if (isLocked() || tryLock(leaseDurationMillis)) {
            return true;
        }

        CompletableFuture<Void> future =
                lockManager.lockIfAvailable(this, waitTimeMillis, leaseDurationMillis);
        try {
            future.get(waitTimeMillis, TimeUnit.MILLISECONDS);
            return true;
        } catch (ExecutionException e) {
            throw new DatabaseException(e);
        } catch (TimeoutException e) {
            log.debug("Timed out waiting to acquire lock for {}", path);
            return false;
        }
    }

    @Override
    public boolean isLocked() {
        if (isLocked.get()) {
            // We rely on local information to check
            // if the lock expired.
            // This should should make this call
            // light weight, while still retaining the
            // safety guarantees.
            if (DateTime.now().isAfter(lockExpirationTime)) {
                isLocked.set(false);
                return false;
            } else {
                return true;
            }
        }
        return false;
    }

    @Override
    public long epoch() {
        return epoch;
    }

    @Override
    public void unlock() {
        if (!isLocked()) {
            return;
        } else {
            if (databaseService.removeIfValueMatches(DistributedLockManager.ONOS_LOCK_TABLE_NAME, path, lockId)) {
                isLocked.set(false);
            }
        }
    }

    @Override
    public boolean extendExpiration(int leaseDurationMillis) {
        if (!isLocked()) {
            log.warn("Ignoring request to extend expiration for lock {}."
                    + " ExtendExpiration must be called for locks that are already acquired.", path);
            return false;
        }

        if (databaseService.putIfValueMatches(
                DistributedLockManager.ONOS_LOCK_TABLE_NAME,
                path,
                lockId,
                lockId)) {
            lockExpirationTime = DateTime.now().plusMillis(leaseDurationMillis);
            log.debug("Succeeded in extending lock {} expiration time to {}", lockExpirationTime);
            return true;
        } else {
            log.info("Failed to extend expiration for {}", path);
            return false;
        }
    }
}
