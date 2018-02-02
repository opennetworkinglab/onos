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
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

import io.atomix.protocols.raft.service.AbstractRaftService;
import io.atomix.protocols.raft.service.Commit;
import io.atomix.protocols.raft.service.RaftServiceExecutor;
import io.atomix.protocols.raft.session.RaftSession;
import io.atomix.protocols.raft.storage.snapshot.SnapshotReader;
import io.atomix.protocols.raft.storage.snapshot.SnapshotWriter;
import io.atomix.utils.concurrent.Scheduled;
import org.onlab.util.KryoNamespace;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.Serializer;

import static com.google.common.base.MoreObjects.toStringHelper;
import static org.onosproject.store.primitives.resources.impl.AtomixDistributedLockEvents.FAILED;
import static org.onosproject.store.primitives.resources.impl.AtomixDistributedLockEvents.LOCKED;
import static org.onosproject.store.primitives.resources.impl.AtomixDistributedLockOperations.LOCK;
import static org.onosproject.store.primitives.resources.impl.AtomixDistributedLockOperations.Lock;
import static org.onosproject.store.primitives.resources.impl.AtomixDistributedLockOperations.UNLOCK;
import static org.onosproject.store.primitives.resources.impl.AtomixDistributedLockOperations.Unlock;

/**
 * Raft atomic value service.
 */
public class AtomixDistributedLockService extends AbstractRaftService {
    private static final Serializer SERIALIZER = Serializer.using(KryoNamespace.newBuilder()
        .register(KryoNamespaces.BASIC)
        .register(AtomixDistributedLockOperations.NAMESPACE)
        .register(AtomixDistributedLockEvents.NAMESPACE)
        .register(LockHolder.class)
        .register(ArrayDeque.class)
        .build());

    private LockHolder lock;
    private Queue<LockHolder> queue = new ArrayDeque<>();
    private final Map<Long, Scheduled> timers = new HashMap<>();

    @Override
    protected void configure(RaftServiceExecutor executor) {
        executor.register(LOCK, SERIALIZER::decode, this::lock);
        executor.register(UNLOCK, SERIALIZER::decode, this::unlock);
    }

    @Override
    public void snapshot(SnapshotWriter writer) {
        writer.writeObject(lock, SERIALIZER::encode);
        writer.writeObject(queue, SERIALIZER::encode);
    }

    @Override
    public void install(SnapshotReader reader) {
        lock = reader.readObject(SERIALIZER::decode);
        queue = reader.readObject(SERIALIZER::decode);

        // After the snapshot is installed, we need to cancel any existing timers and schedule new ones based on the
        // state provided by the snapshot.
        timers.values().forEach(Scheduled::cancel);
        timers.clear();
        for (LockHolder holder : queue) {
            if (holder.expire > 0) {
                timers.put(holder.index,
                    scheduler().schedule(Duration.ofMillis(holder.expire - wallClock().getTime().unixTimestamp()),
                        () -> {
                            timers.remove(holder.index);
                            queue.remove(holder);
                            RaftSession session = sessions().getSession(holder.session);
                            if (session != null && session.getState().active()) {
                                session.publish(FAILED, SERIALIZER::encode, new LockEvent(holder.id, holder.index));
                            }
                        }));
            }
        }
    }

    @Override
    public void onExpire(RaftSession session) {
        releaseSession(session);
    }

    @Override
    public void onClose(RaftSession session) {
        releaseSession(session);
    }

    /**
     * Applies a lock commit.
     *
     * @param commit the lock commit
     */
    protected void lock(Commit<Lock> commit) {
        // If the lock is not already owned, immediately grant the lock to the requester.
        // Note that we still have to publish an event to the session. The event is guaranteed to be received
        // by the client-side primitive after the LOCK response.
        if (lock == null) {
            lock = new LockHolder(
                commit.value().id(),
                commit.index(),
                commit.session().sessionId().id(),
                0);
            commit.session().publish(
                LOCKED,
                SERIALIZER::encode,
                new LockEvent(commit.value().id(), commit.index()));
        // If the timeout is 0, that indicates this is a tryLock request. Immediately fail the request.
        } else if (commit.value().timeout() == 0) {
            commit.session().publish(FAILED, SERIALIZER::encode, new LockEvent(commit.value().id(), commit.index()));
        // If a timeout exists, add the request to the queue and set a timer. Note that the lock request expiration
        // time is based on the *state machine* time - not the system time - to ensure consistency across servers.
        } else if (commit.value().timeout() > 0) {
            LockHolder holder = new LockHolder(
                commit.value().id(),
                commit.index(),
                commit.session().sessionId().id(),
                wallClock().getTime().unixTimestamp() + commit.value().timeout());
            queue.add(holder);
            timers.put(commit.index(), scheduler().schedule(Duration.ofMillis(commit.value().timeout()), () -> {
                // When the lock request timer expires, remove the request from the queue and publish a FAILED
                // event to the session. Note that this timer is guaranteed to be executed in the same thread as the
                // state machine commands, so there's no need to use a lock here.
                timers.remove(commit.index());
                queue.remove(holder);
                if (commit.session().getState().active()) {
                    commit.session().publish(
                        FAILED,
                        SERIALIZER::encode,
                        new LockEvent(commit.value().id(), commit.index()));
                }
            }));
        // If the lock is -1, just add the request to the queue with no expiration.
        } else {
            LockHolder holder = new LockHolder(
                commit.value().id(),
                commit.index(),
                commit.session().sessionId().id(),
                0);
            queue.add(holder);
        }
    }

    /**
     * Applies an unlock commit.
     *
     * @param commit the unlock commit
     */
    protected void unlock(Commit<Unlock> commit) {
        if (lock != null) {
            // If the commit's session does not match the current lock holder, ignore the request.
            if (lock.session != commit.session().sessionId().id()) {
                return;
            }

            // If the current lock ID does not match the requested lock ID, ignore the request. This ensures that
            // internal releases of locks that were never acquired by the client-side primitive do not cause
            // legitimate locks to be unlocked.
            if (lock.id != commit.value().id()) {
                return;
            }

            // The lock has been released. Populate the lock from the queue.
            lock = queue.poll();
            while (lock != null) {
                // If the waiter has a lock timer, cancel the timer.
                Scheduled timer = timers.remove(lock.index);
                if (timer != null) {
                    timer.cancel();
                }

                // If the lock session is for some reason inactive, continue on to the next waiter. Otherwise,
                // publish a LOCKED event to the new lock holder's session.
                RaftSession session = sessions().getSession(lock.session);
                if (session == null || !session.getState().active()) {
                    lock = queue.poll();
                } else {
                    session.publish(
                        LOCKED,
                        SERIALIZER::encode,
                        new LockEvent(lock.id, commit.index()));
                    break;
                }
            }
        }
    }

    /**
     * Handles a session that has been closed by a client or expired by the cluster.
     * <p>
     * When a session is removed, if the session is the current lock holder then the lock is released and the next
     * session waiting in the queue is granted the lock. Additionally, all pending lock requests for the session
     * are removed from the lock queue.
     *
     * @param session the closed session
     */
    private void releaseSession(RaftSession session) {
        // Remove all instances of the session from the lock queue.
        queue.removeIf(lock -> lock.session == session.sessionId().id());

        // If the removed session is the current holder of the lock, nullify the lock and attempt to grant it
        // to the next waiter in the queue.
        if (lock != null && lock.session == session.sessionId().id()) {
            lock = queue.poll();
            while (lock != null) {
                // If the waiter has a lock timer, cancel the timer.
                Scheduled timer = timers.remove(lock.index);
                if (timer != null) {
                    timer.cancel();
                }

                // If the lock session is inactive, continue on to the next waiter. Otherwise,
                // publish a LOCKED event to the new lock holder's session.
                RaftSession lockSession = sessions().getSession(lock.session);
                if (lockSession == null || !lockSession.getState().active()) {
                    lock = queue.poll();
                } else {
                    lockSession.publish(
                        LOCKED,
                        SERIALIZER::encode,
                        new LockEvent(lock.id, lock.index));
                    break;
                }
            }
        }
    }

    private class LockHolder {
        private final int id;
        private final long index;
        private final long session;
        private final long expire;

        public LockHolder(int id, long index, long session, long expire) {
            this.id = id;
            this.index = index;
            this.session = session;
            this.expire = expire;
        }

        @Override
        public String toString() {
            return toStringHelper(this)
                .add("id", id)
                .add("index", index)
                .add("session", session)
                .add("expire", expire)
                .toString();
        }
    }
}