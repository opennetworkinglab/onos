package org.onlab.onos.store.service.impl;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.joda.time.DateTime;
import org.onlab.onos.cluster.ClusterService;
import org.onlab.onos.store.service.DatabaseService;
import org.onlab.onos.store.service.Lock;
import org.onlab.onos.store.service.OptimisticLockException;

/**
 * A distributed lock implementation.
 */
public class DistributedLock implements Lock {

    private final DistributedLockManager lockManager;
    private final DatabaseService databaseService;
    private final String path;
    private DateTime lockExpirationTime;
    private AtomicBoolean isLocked = new AtomicBoolean(false);
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
                (UUID.randomUUID().toString() + "::" + clusterService.getLocalNode().id().toString()).getBytes();
    }

    @Override
    public String path() {
        return path;
    }

    @Override
    public void lock(int leaseDurationMillis) {

        if (isLocked() && lockExpirationTime.isAfter(DateTime.now().plusMillis(leaseDurationMillis))) {
            // Nothing to do.
            // Current expiration time is beyond what is requested.
            return;
        } else {
            tryLock(Long.MAX_VALUE, leaseDurationMillis);
        }
    }

    @Override
    public boolean tryLock(int leaseDurationMillis) {
        try {
            databaseService.putIfAbsent(DistributedLockManager.ONOS_LOCK_TABLE_NAME, path, lockId);
            return true;
        } catch (OptimisticLockException e) {
            return false;
        }
    }

    @Override
    public boolean tryLock(
            long waitTimeMillis,
            int leaseDurationMillis) {
        if (!tryLock(leaseDurationMillis)) {
            CompletableFuture<Void> future =
                    lockManager.lockIfAvailable(this, waitTimeMillis, leaseDurationMillis);
            try {
                future.get(waitTimeMillis, TimeUnit.MILLISECONDS);
            } catch (ExecutionException | InterruptedException e) {
                // TODO: ExecutionException could indicate something
                // wrong with the backing database.
                // Throw an exception?
                return false;
            } catch (TimeoutException e) {
                return false;
            }
        }
        lockExpirationTime = DateTime.now().plusMillis(leaseDurationMillis);
        return true;
    }

    @Override
    public boolean isLocked() {
        if (isLocked.get()) {
            // We rely on local information to check
            // if the expired.
            // This should should make this call
            // light weight, which still retaining the same
            // safety guarantees.
            if (DateTime.now().isAfter(lockExpirationTime)) {
                isLocked.set(false);
                return false;
            }
        }
        return true;
    }

    @Override
    public void unlock() {
        if (!isLocked()) {
            return;
        } else {
            databaseService.removeIfValueMatches(DistributedLockManager.ONOS_LOCK_TABLE_NAME, path, lockId);
        }
    }

    @Override
    public boolean extendExpiration(int leaseDurationMillis) {
        if (isLocked() && lockExpirationTime.isAfter(DateTime.now().plusMillis(leaseDurationMillis))) {
            return true;
        } else {
            return tryLock(leaseDurationMillis);
        }
    }
}