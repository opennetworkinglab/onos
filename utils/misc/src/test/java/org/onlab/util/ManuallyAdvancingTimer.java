/*
 * Copyright 2015-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onlab.util;

import com.google.common.collect.Lists;
import org.onlab.junit.TestUtils;
import org.slf4j.Logger;

import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.junit.TestTools.delay;
import static org.slf4j.LoggerFactory.getLogger;


/**
 * Provides manually scheduled timer utility. All schedulable methods are subject to overflow (you can set a period of
 * max long).  Additionally if a skip skips a period of time greater than one period for a periodic task that task will
 * only be executed once for that skip and scheduled it's period after the last execution.
 */
public class ManuallyAdvancingTimer extends java.util.Timer {

    /* States whether or not the static values from timer task have been set ensures population will only occur once.*/
    private boolean staticsPopulated = false;

    /* Virgin value from timer task */
    private int virginState;

    /* Scheduled value from timer task */
    private int scheduledState;

    /* Executed value from timer task */
    private int executedState;

    /* Cancelled value from timer task */
    private int cancelledState;

    private final Logger logger = getLogger(getClass());

    /* Service for executing timer tasks */
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    /* Internal time representation independent of system time, manually advanced */
    private final TimerKeeper timerKeeper = new TimerKeeper();

    /* Data structure for tracking tasks */
    private final TaskQueue queue = new TaskQueue();

    /* Whether execution should execute on the executor thread or the calling thread. */
    private final boolean runLocally;

    public ManuallyAdvancingTimer(boolean runLocally) {
        this.runLocally = runLocally;
    }


    @Override
    public void schedule(TimerTask task, long delay) {
        if (!staticsPopulated) {
            populateStatics(task);
        }
        if (!submitTask(task, delay > 0 ? timerKeeper.currentTimeInMillis() + delay :
                timerKeeper.currentTimeInMillis() - delay, 0)) {
            logger.error("Failed to submit task");
        }
    }

    @Override
    public void schedule(TimerTask task, Date time) {
        if (!staticsPopulated) {
            populateStatics(task);
        }
        if (!submitTask(task, time.getTime(), 0)) {
            logger.error("Failed to submit task");
        }
    }

    @Override
    public void schedule(TimerTask task, long delay, long period) {
        if (!staticsPopulated) {
            populateStatics(task);
        }
        if (!submitTask(task, delay > 0 ? timerKeeper.currentTimeInMillis() + delay :
                timerKeeper.currentTimeInMillis() - delay, period)) {
            logger.error("Failed to submit task");
        }
    }

    @Override
    public void schedule(TimerTask task, Date firstTime, long period) {
        if (!staticsPopulated) {
            populateStatics(task);
        }
        if (!submitTask(task, firstTime.getTime(), period)) {
            logger.error("Failed to submit task");
        }
    }

    /*################################################WARNING################################################*/
    /* Schedule at fixed rate methods do not work exactly as in the java timer. They are clones of the periodic
    *scheduling methods. */
    @Override
    public void scheduleAtFixedRate(TimerTask task, long delay, long period) {
        if (!staticsPopulated) {
            populateStatics(task);
        }
        if (!submitTask(task, delay > 0 ? timerKeeper.currentTimeInMillis() + delay :
                timerKeeper.currentTimeInMillis() - delay, period)) {
            logger.error("Failed to submit task");
        }
    }

    @Override
    public void scheduleAtFixedRate(TimerTask task, Date firstTime, long period) {
        if (!staticsPopulated) {
            populateStatics(task);
        }
        if (!submitTask(task, firstTime.getTime(), period)) {
            logger.error("Failed to submit task");
        }
    }

    @Override
    public void cancel() {
        executorService.shutdown();
        queue.clear();
    }

    @Override
    public int purge() {
        return queue.removeCancelled();
    }

    /**
     * Returns the virtual current time in millis.
     *
     * @return long representing simulated current time.
     */
    public long currentTimeInMillis() {
        return timerKeeper.currentTimeInMillis();
    }

    /**
     * Returns the new simulated current time in millis after advancing the absolute value of millis to advance.
     * Triggers event execution of all events scheduled for execution at times up to and including the returned time.
     * Passing in the number zero has no effect.
     *
     * @param millisToAdvance the number of millis to advance.
     * @return a long representing the current simulated time in millis
     */
    public long advanceTimeMillis(long millisToAdvance) {
        return timerKeeper.advanceTimeMillis(millisToAdvance);
    }

    /**
     * Advances the virtual time a certain number of millis triggers execution delays a certain amount to
     * allow time for execution.  If runLocally is true then all real time delays are ignored.
     *
     * @param virtualTimeAdvance the time to be advances in millis of simulated time.
     * @param realTimeDelay      the time to delay in real time to allow for processing.
     */
    public void advanceTimeMillis(long virtualTimeAdvance, int realTimeDelay) {
        timerKeeper.advanceTimeMillis(virtualTimeAdvance);
        if (!runLocally) {
            delay(realTimeDelay);
        }
    }

    /**
     * Sets up the task and submits it to the queue.
     *
     * @param task    the task to be added to the queue
     * @param runtime the first runtime of the task
     * @param period  the period between runs thereafter
     * @return returns true if the task was successfully submitted, false otherwise
     */
    private boolean submitTask(TimerTask task, long runtime, long period) {
        checkNotNull(task);
        try {
            TestUtils.setField(task, "state", scheduledState);
            TestUtils.setField(task, "nextExecutionTime", runtime);
            TestUtils.setField(task, "period", period);
        } catch (TestUtils.TestUtilsException e) {
            e.printStackTrace();
            return false;
        }
        queue.insertOrdered(task);
        return true;
    }

    /**
     * Executes the given task (only if it is in the scheduled state) and proceeds to reschedule it or mark it as
     * executed.  Does not remove from the queue (this must be done outside).
     *
     * @param task the timer task to be executed
     */
    private boolean executeTask(TimerTask task) {
        checkNotNull(task);
        int currentState;
        try {
            currentState = TestUtils.getField(task, "state");
        } catch (TestUtils.TestUtilsException e) {
            logger.error("Could not get state of task.");
            e.printStackTrace();
            return false;
        }
        //If cancelled or already executed stop here.
        if (currentState == executedState || currentState == cancelledState) {
            return false;
        } else if (currentState == virginState) {
            logger.error("Task was set for execution without being scheduled.");
            return false;
        } else if (currentState == scheduledState) {
            long period;

            try {
                period = TestUtils.getField(task, "period");
            } catch (TestUtils.TestUtilsException e) {
                logger.error("Could not read period of task.");
                e.printStackTrace();
                return false;
            }
            //Period of zero means one time execution.
            if (period == 0) {
                try {
                    TestUtils.setField(task, "state", executedState);
                } catch (TestUtils.TestUtilsException e) {
                    logger.error("Could not set executed state.");
                    e.printStackTrace();
                    return false;
                }
                if (runLocally) {
                    task.run();
                } else {
                    executorService.execute(task);
                }
                return true;
            } else {
                //Calculate next execution time, using absolute value of period
                long nextTime = (period > 0) ? (timerKeeper.currentTimeInMillis() + period) :
                        (timerKeeper.currentTimeInMillis() - period);
                try {
                    TestUtils.setField(task, "nextExecutionTime", nextTime);
                } catch (TestUtils.TestUtilsException e) {
                    logger.error("Could not set next execution time.");
                    e.printStackTrace();
                    return false;
                }
                //Schedule next execution
                queue.insertOrdered(task);
                if (runLocally) {
                    task.run();
                } else {
                    executorService.execute(task);
                }
                return true;
            }
        }
        logger.error("State property of {} is in an illegal state and did not execute.", task);
        return false;
    }

    /**
     * Executes all tasks in the queue scheduled for execution up to and including the current time.
     *
     * @return the total number of tasks run, -1 if failure
     */
    private int executeEventsUpToPresent() {
        int totalRun = 0;
        if (queue.isEmpty()) {
            return -1;
        }
        TimerTask currTask = queue.peek();
        long currExecTime;
        try {
            currExecTime = TestUtils.getField(currTask, "nextExecutionTime");
        } catch (TestUtils.TestUtilsException e) {
            e.printStackTrace();
            throw new IllegalStateException("Could not get nextExecutionTime");
        }
        while (currExecTime <= timerKeeper.currentTimeInMillis()) {
            if (executeTask(queue.pop())) {
                totalRun++;
            }
            if (queue.isEmpty()) {
                break;
            }
            currTask = queue.peek();
            try {
                currExecTime = TestUtils.getField(currTask, "nextExecutionTime");
            } catch (TestUtils.TestUtilsException e) {
                e.printStackTrace();
                throw new IllegalStateException("Could not get nextExecutionTime");
            }
        }
        return totalRun;
    }

    /**
     * Populates the static fields from timer task. Should only be called once.
     */
    private void populateStatics(TimerTask task) {
        try {
            virginState = TestUtils.getField(task, "VIRGIN");
            scheduledState = TestUtils.getField(task, "SCHEDULED");
            executedState = TestUtils.getField(task, "EXECUTED");
            cancelledState = TestUtils.getField(task, "CANCELLED");
            staticsPopulated = true;
        } catch (TestUtils.TestUtilsException e) {
            e.printStackTrace();
        }
    }

    /**
     * A class used to maintain the virtual time.
     */
    private class TimerKeeper {

        private long currentTime = 0;

        /**
         * Returns the virtual current time in millis.
         *
         * @return long representing simulated current time.
         */
        long currentTimeInMillis() {
            return currentTime;
        }

        /**
         * Returns the new simulated current time in millis after advancing the absolute value of millis to advance.
         * Triggers event execution of all events scheduled for execution at times up to and including the returned
         * time. Passing in the number zero has no effect.
         *
         * @param millisToAdvance the number of millis to advance.
         * @return a long representing the current simulated time in millis
         */
        long advanceTimeMillis(long millisToAdvance) {
            currentTime = (millisToAdvance >= 0) ? (currentTime + millisToAdvance) : (currentTime - millisToAdvance);
            if (millisToAdvance != 0) {
                executeEventsUpToPresent();
            }
            return currentTime;
        }
    }

    /**
     * A queue backed by a linked list. Keeps elements sorted in ascending order of execution time.  All calls are safe
     * even on empty queue's.
     */
    private class TaskQueue {
        private final LinkedList<TimerTask> taskList = Lists.newLinkedList();

        /**
         * Adds the task to the queue in ascending order of scheduled execution. If execution time has already passed
         * execute immediately.
         *
         * @param task the task to be added to the queue
         */
        void insertOrdered(TimerTask task) {
            //Using O(N) insertion because random access is expensive in linked lists worst case is 2N links followed
            // for binary insertion vs N for simple insertion.
            checkNotNull(task);
            if (!staticsPopulated) {
                populateStatics(task);
            }
            long insertTime;
            try {
                insertTime = TestUtils.getField(task, "nextExecutionTime");
                TestUtils.setField(task, "state", scheduledState);
            } catch (TestUtils.TestUtilsException e) {
                e.printStackTrace();
                return;
            }
            //If the task was scheduled in the past or for the current time run it immediately and do not add to the
            // queue, subsequent executions will be scheduled as normal
            if (insertTime <= timerKeeper.currentTimeInMillis()) {
                executeTask(task);
                return;
            }

            Iterator<TimerTask> iter = taskList.iterator();
            int positionCounter = 0;
            long nextTaskTime;
            TimerTask currentTask;
            while (iter.hasNext()) {
                currentTask = iter.next();
                try {
                    nextTaskTime = TestUtils.getField(currentTask, "nextExecutionTime");
                } catch (TestUtils.TestUtilsException e) {
                    e.printStackTrace();
                    return;
                }
                if (insertTime < nextTaskTime) {
                    taskList.add(positionCounter, task);
                    return;
                }
                positionCounter++;
            }
            taskList.addLast(task);
        }

        /**
         * Returns the first item in the queue (next scheduled for execution) without removing it, returns null if the
         * queue is empty.
         *
         * @return the next TimerTask to run or null if the queue is empty
         */
        TimerTask peek() {
            if (taskList.isEmpty()) {
                return null;
            }
            return taskList.getFirst();
        }

        /**
         * Returns and removes the first item in the queue or null if it is empty.
         *
         * @return the first element of the queue or null if the queue is empty
         */
        TimerTask pop() {
            if (taskList.isEmpty()) {
                return null;
            }
            return taskList.pop();
        }

        /**
         * Performs a sort on the set of timer tasks, earliest task is first. Does nothing if queue is empty.
         */
        void sort() {
            if (taskList.isEmpty()) {
                return;
            }
            taskList.sort((o1, o2) -> {
                checkNotNull(o1);
                checkNotNull(o2);
                long executionTimeOne;
                long executionTimeTwo;
                try {
                    executionTimeOne = TestUtils.getField(o1, "nextExecutionTime");
                    executionTimeTwo = TestUtils.getField(o2, "nextExecutionTime");
                } catch (TestUtils.TestUtilsException e) {
                    e.printStackTrace();
                    throw new IllegalStateException("Could not get next execution time.");
                }
                if (executionTimeOne == executionTimeTwo) {
                    return 0;
                } else if (executionTimeOne < executionTimeTwo) {
                    return -1;
                } else {
                    return 1;
                }
            });
        }

        /**
         * Returns whether the queue is currently empty.
         *
         * @return true if the queue is empty, false otherwise
         */
        boolean isEmpty() {
            return taskList.isEmpty();
        }

        /**
         * Clears the underlying list of the queue.
         */
        void clear() {
            taskList.clear();
        }

        /**
         * Removes all cancelled tasks from the queue. Has no effect on behavior.
         *
         * @return returns the total number of items removed, -1 if list is empty or failure occurs.
         */
        int removeCancelled() {
            if (taskList.isEmpty()) {
                return -1;
            }
            int removedCount = 0;
            Iterator<TimerTask> taskIterator = taskList.iterator();
            TimerTask currTask;
            int currState;
            while (taskIterator.hasNext()) {
                currTask = taskIterator.next();
                try {
                    currState = TestUtils.getField(currTask, "state");
                } catch (TestUtils.TestUtilsException e) {
                    logger.error("Could not get task state.");
                    e.printStackTrace();
                    return -1;
                }
                if (currState == cancelledState) {
                    removedCount++;
                    taskIterator.remove();
                }
            }
            return removedCount;
        }
    }
}
