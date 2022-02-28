/*
 * Copyright 2019-present Open Networking Foundation
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

package org.onosproject.provider.general.device.impl;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Striped;
import org.onosproject.net.DeviceId;
import org.slf4j.Logger;

import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Allows submitting tasks related to a specific device. Takes care of executing
 * pending tasks sequentially for each device in a FIFO order, while using a
 * delegate executor. It also avoids executing duplicate tasks when arriving
 * back-to-back.
 *
 * @param <T> enum describing the type of task
 */

class DeviceTaskExecutor<T extends Enum> {
    /**
     * Minimum interval between duplicate back-to-back tasks.
     */
    private static final int DUPLICATE_MIN_INTERVAL_MILLIS = 1000;

    private final Logger log = getLogger(getClass());

    private final ExecutorService delegate;
    private final AtomicBoolean canceled = new AtomicBoolean(false);
    private final Set<DeviceId> busyDevices = Sets.newConcurrentHashSet();
    private final Set<DeviceId> pendingDevices = Sets.newConcurrentHashSet();
    private final Striped<Lock> deviceLocks = Striped.lock(30);
    private final LoadingCache<DeviceId, TaskQueue> taskQueues = CacheBuilder.newBuilder()
            .expireAfterAccess(1, TimeUnit.MINUTES)
            .removalListener((RemovalListener<DeviceId, TaskQueue>) notification -> {
                if (!notification.getValue().isEmpty()) {
                    log.warn("Cache evicted non-empty task queue for {} ({} pending tasks)",
                             notification.getKey(), notification.getValue().size());
                }
            })
            .build(new CacheLoader<DeviceId, TaskQueue>() {
                @SuppressWarnings("NullableProblems")
                @Override
                public TaskQueue load(DeviceId deviceId) {
                    return new TaskQueue();
                }
            });
    /**
     * Type of tasks allowed to be back to back.
     */
    private final Set<T> allowList;

    /**
     * Creates a new executor with the given delegate executor service
     * and the allowed back to back task types.
     *
     * @param delegate executor service
     * @param allowed tasks allowed to be back to back
     */
    DeviceTaskExecutor(ExecutorService delegate, Set<T> allowed) {
        checkNotNull(delegate);
        this.delegate = delegate;
        this.allowList = allowed;
    }

    /**
     * Submit a tasks.
     *
     * @param deviceId device associated with the task
     * @param type     type of task (used to remove eventual back-to-back
     *                 duplicates)
     * @param runnable runnable to execute
     */
    void submit(DeviceId deviceId, T type, Runnable runnable) {
        checkNotNull(deviceId);
        checkNotNull(type);
        checkNotNull(runnable);

        if (canceled.get()) {
            log.warn("Executor was cancelled, cannot submit task {} for {}",
                     type, deviceId);
            return;
        }

        final DeviceTask task = new DeviceTask(deviceId, type, runnable);
        deviceLocks.get(deviceId).lock();
        try {
            if (taskQueues.get(deviceId).isBackToBackDuplicate(type) && !allowList.contains(type)) {
                if (log.isDebugEnabled()) {
                    log.debug("Dropping back-to-back duplicate task {} for {}",
                              type, deviceId);
                }
                return;
            }
            if (taskQueues.get(deviceId).offer(task)) {
                pendingDevices.add(deviceId);
                if (!busyDevices.contains(deviceId)) {
                    // The task was submitted to the queue and we are not
                    // performing any other task for this device. There is at
                    // least one task that is ready to be executed.
                    delegate.execute(this::performTaskIfAny);
                }
            } else {
                log.warn("Unable to submit task {} for {}",
                         task.type, task.deviceId);
            }
        } catch (ExecutionException e) {
            log.warn("Exception while accessing task queue cache", e);
        } finally {
            deviceLocks.get(task.deviceId).unlock();
        }
    }

    /**
     * Prevents the executor from executing any more tasks.
     */
    void cancel() {
        canceled.set(true);
    }

    private void performTaskIfAny() {
        final DeviceTask task = pollTask();
        if (task == null) {
            // No tasks.
            return;
        }
        if (canceled.get()) {
            log.warn("Executor was cancelled, dropping task {} for {}",
                     task.type, task.deviceId);
            return;
        }
        if (log.isTraceEnabled()) {
            log.trace("STARTING task {} for {}...", task.type.name(), task.deviceId);
        }
        try {
            task.runnable.run();
        } catch (DeviceTaskException e) {
            log.error("Unable to complete task {} for {}: {}",
                      task.type, task.deviceId, e.getMessage());
        } catch (Throwable t) {
            log.error(format(
                    "Uncaught exception when executing task %s for %s",
                    task.type, task.deviceId), t);
        }
        if (log.isTraceEnabled()) {
            log.trace("COMPLETED task {} for {}", task.type.name(), task.deviceId);
        }
        busyDevices.remove(task.deviceId);
        delegate.execute(this::performTaskIfAny);
    }

    private DeviceTask pollTask() {
        for (DeviceId deviceId : pendingDevices) {
            final DeviceTask task;
            deviceLocks.get(deviceId).lock();
            try {
                if (busyDevices.contains(deviceId)) {
                    // Next device.
                    continue;
                }
                task = taskQueues.get(deviceId).poll();
                if (task == null) {
                    // Next device.
                    continue;
                }
                if (taskQueues.get(deviceId).isEmpty()) {
                    pendingDevices.remove(deviceId);
                }
                busyDevices.add(deviceId);
                return task;
            } catch (ExecutionException e) {
                log.warn("Exception while accessing task queue cache", e);
            } finally {
                deviceLocks.get(deviceId).unlock();
            }
        }
        return null;
    }

    /**
     * Device task as stored in the task queue.
     */
    private class DeviceTask {

        private final DeviceId deviceId;
        private final T type;
        private final Runnable runnable;

        DeviceTask(DeviceId deviceId, T type, Runnable runnable) {
            this.deviceId = deviceId;
            this.type = type;
            this.runnable = runnable;
        }
    }

    /**
     * A queue that keeps track of the last task added to detects back-to-back
     * duplicates.
     */
    private class TaskQueue extends ConcurrentLinkedQueue<DeviceTask> {

        private T lastTaskAdded;
        private long lastAddedMillis;

        @Override
        public boolean offer(DeviceTask deviceTask) {
            lastTaskAdded = deviceTask.type;
            lastAddedMillis = currentTimeMillis();
            return super.offer(deviceTask);
        }

        boolean isBackToBackDuplicate(T taskType) {
            return lastTaskAdded != null
                    && lastTaskAdded.equals(taskType)
                    && (currentTimeMillis() - lastAddedMillis) <= DUPLICATE_MIN_INTERVAL_MILLIS;
        }
    }

    /**
     * Signals an error that prevented normal execution of the task.
     */
    static class DeviceTaskException extends RuntimeException {

        /**
         * Creates a new exception.
         *
         * @param message explanation
         */
        DeviceTaskException(String message) {
            super(message);
        }
    }
}
