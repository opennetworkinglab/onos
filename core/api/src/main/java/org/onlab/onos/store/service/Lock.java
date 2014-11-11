package org.onlab.onos.store.service;

/**
 * A lock is a tool for controlling access to a shared resource by multiple processes.
 * Commonly, a lock provides exclusive access to a resource such as a network device
 * or exclusive permission to a controller to perform a particular role such as serve
 * as the master controller for a device.
 * At any given time one and only process can acquire the lock.
 */
public interface Lock {

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
     * (0, MAX_LEASE_MILLIS]
     */
    void lock(long leaseDurationMillis);

    /**
     * Acquires the lock only if it is free at the time of invocation.
     * @param leaseDurationMillis the number of milliseconds the must be
     * locked after it is granted, before automatically releasing it if it hasn't
     * already been released by an invocation of unlock(). Must be in the range
     * (0, MAX_LEASE_MILLIS]
     * @return true if the lock was acquired and false otherwise
     */
    boolean tryLock(long leaseDurationMillis);

    /**
     * Acquires the lock if it is free within the given waiting
     * time and the current thread has not been interrupted.
     * @param waitTimeMillis the maximum time (in milliseconds) to wait for the lock
     * @param leaseDurationMillis the number of milliseconds to hold the
     * lock after granting it, before automatically releasing it if it hasn't
     * already been released by invoking unlock(Object). Must be in the range
     * (0, MAX_LEASE_MILLIS]
     * @return true if the lock was acquired and false if the waiting time
     * elapsed before the lock was acquired
     */
    boolean tryLock(long waitTimeMillis, long leaseDurationMillis);

    /**
     * Returns true if this Lock instance currently holds the lock.
     * @return true if this instance is the owner of the lock.
     */
    boolean isLocked();

    /**
     * Releases the lock.
     */
    void unlock();

    /**
     * Extends the lease for this lock.
     * @param extensionDurationMillis is the amount of additional
     * time to add to the end of the current expiration time. For example,
     * if the lock is currently set to expire at time T, a successful call to
     * extendLease with an argument of 5000 will cause the lock to
     * now expire at 5 seconds past T.
     * @return true if the extension is successful, false otherwise. Note
     * that a failure to extend the lease does not result in unlocking. The lock
     * will be released either by an explicit call to unlock or when previously
     * acquired lease runs out.
     */
    boolean extendLease(long extensionDurationMillis);
}