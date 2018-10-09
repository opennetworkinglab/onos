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
package org.onosproject.workflow.api;

import java.util.Date;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Class for time chain timer.
 */
public class TimerChain {

    private PriorityQueue<TimerChainTask> taskQueue = new PriorityQueue<>();

    private Timer impendingTimer;
    private TimerChainTask impendingTask;

    /**
     * Constructor of timer chain.
     */
    public TimerChain() {

    }

    /**
     * Schedules timer event.
     * @param afterMs millisecond which time event happens.
     * @param runnable runnable to be executed after 'afterMs'
     */
    public void schedule(long afterMs, Runnable runnable) {
        schedule(new Date((new Date()).getTime() + afterMs), runnable);
    }

    /**
     * Schedules timer event.
     * @param date date which timer event happens.
     * @param runnable runnable to be executed on 'date'
     */
    public void schedule(Date date, Runnable runnable) {
        schedule(new TimerChainTask(this, date, runnable));
    }

    /**
     * Schedule timer chain task.
     * @param task task to be scheduled.
     */
    private void schedule(TimerChainTask task) {
        synchronized (this) {
            if (taskQueue.isEmpty()) {
                scheduleImpending(task);
                return;
            }

            if (task.date().getTime() < head().date().getTime()) {
                impendingTimer.cancel();
                impendingTask.cancel();
                TimerChainTask prevImpendingTask = pop().copy();
                taskQueue.offer(prevImpendingTask);

                scheduleImpending(task);
            } else {
                taskQueue.offer(task);
            }
        }
    }

    /**
     * Schedule impending timer task.
     * @param task impending timer chain task
     * @return timer chain task
     */
    private TimerChainTask scheduleImpending(TimerChainTask task) {
        taskQueue.offer(task);
        Timer timer = new Timer();
        this.setImpendingTask(task);
        this.setImpendingTimer(timer);
        timer.schedule(task, task.date());
        return task;
    }

    /**
     * Gets impending timer.
     * @return impending timer
     */
    public Timer implendingTimer() {
        return impendingTimer;
    }

    /**
     * Sets impending timer.
     * @param impendingTimer impending timer
     */
    public void setImpendingTimer(Timer impendingTimer) {
        this.impendingTimer = impendingTimer;
    }

    /**
     * Gets impending timer task.
     * @return impending timer task
     */
    public TimerTask impendingTask() {
        return impendingTask;
    }

    /**
     * Sets impending timer task.
     * @param impendingTask impending timer task
     */
    public void setImpendingTask(TimerChainTask impendingTask) {
        this.impendingTask = impendingTask;
    }

    /**
     * Gets head of timer chain task queue.
     * @return timer chain task
     */
    public TimerChainTask head() {
        if (!taskQueue.isEmpty()) {
            return taskQueue.peek();
        } else {
            return null;
        }
    }

    /**
     * Pops head of timer chain task queue.
     * @return timer chain task
     */
    public TimerChainTask pop() {
        if (!taskQueue.isEmpty()) {
            return taskQueue.poll();
        } else {
            return null;
        }
    }

    /**
     * Class for timer chain task.
     */
    public static class TimerChainTask extends TimerTask implements Comparable<TimerChainTask> {

        private final TimerChain timerchain;
        private final Date date;
        private final Runnable runnable;

        /**
         * Constructor of timer chain task.
         * @param timerchain timer chain
         * @param date date to be scheduled
         * @param runnable runnable to be executed by timer
         */
        public TimerChainTask(TimerChain timerchain, Date date, Runnable runnable) {
            this.timerchain = timerchain;
            this.date = date;
            this.runnable = runnable;
        }

        /**
         * Gets date.
         * @return date of timer chain task
         */
        public Date date() {
            return this.date;
        }

        /**
         * Gets runnable.
         * @return runnable of timer chain task
         */
        public Runnable runnable() {
            return this.runnable;
        }

        @Override
        public void run() {

            TimerChainTask nextTask;
            synchronized (timerchain) {
                if (timerchain.impendingTask() != this) {
                    runnable().run();
                    return;
                }

                timerchain.implendingTimer().cancel();
                timerchain.pop();

                nextTask = timerchain.head();

                if (nextTask != null) {
                    Timer nextTimer = new Timer();
                    this.timerchain.setImpendingTask(nextTask);
                    this.timerchain.setImpendingTimer(nextTimer);
                    nextTimer.schedule(nextTask, nextTask.date());
                }
            }

            runnable().run();

        }

        @Override
        public int compareTo(TimerChainTask target) {
            return date().compareTo(target.date());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof TimerChainTask)) {
                return false;
            }
            TimerChainTask that = (TimerChainTask) o;

            return this.date().equals(that.date());
        }

        @Override
        public int hashCode() {
            return Objects.hash(date);
        }

        /**
         * Copies timer chain task.
         * @return timer chain task
         */
        public TimerChainTask copy() {
            return new TimerChainTask(timerchain, date, runnable);
        }
    }
}
