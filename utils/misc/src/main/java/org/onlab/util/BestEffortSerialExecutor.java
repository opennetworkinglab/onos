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
package org.onlab.util;

import java.util.LinkedList;
import java.util.concurrent.Executor;

/**
 * Executor that executes tasks in serial on a shared thread pool, falling back to parallel execution when threads
 * are blocked.
 * <p>
 * This executor attempts to execute tasks in serial as if they occur on a single thread. However, in the event tasks
 * are blocking a thread (a thread is in the {@link Thread.State#WAITING} or {@link Thread.State#TIMED_WAITING} state)
 * the executor will execute tasks on parallel on the underlying {@link Executor}. This is useful for ensuring blocked
 * threads cannot block events, but mimics a single-threaded model otherwise.
 */
public class BestEffortSerialExecutor implements Executor {
    private final Executor parent;
    private final LinkedList<Runnable> tasks = new LinkedList<>();
    private volatile Thread thread;

    public BestEffortSerialExecutor(Executor parent) {
        this.parent = parent;
    }

    private void run() {
        synchronized (tasks) {
            thread = Thread.currentThread();
        }
        for (;;) {
            if (!runTask()) {
                synchronized (tasks) {
                    thread = null;
                }
                return;
            }
        }
    }

    private boolean runTask() {
        final Runnable task;
        synchronized (tasks) {
            task = tasks.poll();
            if (task == null) {
                return false;
            }
        }
        task.run();
        return true;
    }

    @Override
    public void execute(Runnable command) {
        synchronized (tasks) {
            tasks.add(command);
            if (thread == null) {
                parent.execute(this::run);
            } else if (thread.getState() == Thread.State.WAITING || thread.getState() == Thread.State.TIMED_WAITING) {
                parent.execute(this::runTask);
            }
        }
    }
}
