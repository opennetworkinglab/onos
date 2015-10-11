/*
 * Copyright 2015 Open Networking Laboratory
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
import org.junit.Before;
import org.junit.Test;
import org.junit.Ignore;

import java.util.ArrayList;
import java.util.Date;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.onlab.junit.TestTools.delay;

/**
 * Testing class for manually advancing timer.
 */
public class ManuallyAdvancingTimerTest {

    private ManuallyAdvancingTimer timer;

    /* Generates unique id's for TestTasks */
    private AtomicInteger idGenerator;

    /* Tracks TestTasks in order of creation, tasks are automatically added at creation. */
    private ArrayList<TestTask> taskList;

    /* Total number of tasks run */
    private AtomicInteger tasksRunCount;

    // FIXME if this class fails first try increasing the real time delay to account for heavy system load.
    private static final int REAL_TIME_DELAY = 1;

    /**
     * Sets up the testing environment.
     */
    @Before
    public void setup() {
        timer = new ManuallyAdvancingTimer();
        idGenerator = new AtomicInteger(1);
        tasksRunCount = new AtomicInteger(0);
        taskList = Lists.newArrayList();
    }

    /**
     * Tests the one time schedule with delay.
     *
     * @throws Exception throws an exception if the test fails
     */
    @Ignore("Ignored when running CircleCI")
    @Test
    public void testScheduleByDelay() throws Exception {
        /* Test scheduling in the future as normal. */
        timer.schedule(new TestTask(), 10);
        timer.advanceTimeMillis(5);
        assertFalse(taskList.get(0).hasRun());
        timer.advanceTimeMillis(10, REAL_TIME_DELAY);
        assertTrue(taskList.get(0).hasRun());

        /* Test scheduling with negative numbers */
        timer.schedule(new TestTask(), -10);
        timer.advanceTimeMillis(5);
        assertFalse(taskList.get(1).hasRun());
        timer.advanceTimeMillis(10, REAL_TIME_DELAY);
        assertTrue(taskList.get(1).hasRun());

        /* Reset list, counter and timer for next test */
        taskList.clear();
        idGenerator.set(1);
        tasksRunCount.set(0);

        for (int i = 0; i < 50; i++) {
            timer.schedule(new TestTask(), i);
        }
        /* Test that a task scheduled for present is run and not placed in the queue */
        assertEquals("Only the first task should have run.", 1, tasksRunCount.get());

        for (int i = 2; i <= 50; i++) {
            timer.advanceTimeMillis(1, REAL_TIME_DELAY);
            assertEquals("One task should be executed per loop", i, tasksRunCount.get());
        }
        /* Below tests ordered insertion, this will only be done once, it is the same for all schedule methods. */

        tasksRunCount.set(0);

        for (int i = 0; i < 10; i++) {
            timer.schedule(new TestTask(), 500);
        }

        assertEquals("No new tasks should have been run  since run count reset.", 0, tasksRunCount.get());
        timer.schedule(new TestTask(), 10);
        assertEquals("No new tasks should have been run  since run count reset.", 0, tasksRunCount.get());
        timer.advanceTimeMillis(10, REAL_TIME_DELAY);
        assertEquals("One new tasks should have been run  since run count reset.", 1, tasksRunCount.get());
        timer.advanceTimeMillis(510, REAL_TIME_DELAY);
        assertEquals("Eleven new tasks should have been run  since run count reset.", 11, tasksRunCount.get());
    }

    /**
     * Tests scheduling for a particular date or time which may be in the past.
     *
     * @throws Exception throws an exception if the test fails
     */
    @Test
    public void testScheduleByDate() throws Exception {
        /* Tests basic scheduling for future times. */
        timer.schedule(new TestTask(), new Date(10));
        timer.advanceTimeMillis(5);
        assertFalse(taskList.get(0).hasRun());
        timer.advanceTimeMillis(10, REAL_TIME_DELAY);
        assertTrue(taskList.get(0).hasRun());

        /* Test scheduling with past times numbers */
        timer.schedule(new TestTask(), new Date(0));
        delay(REAL_TIME_DELAY);
        assertTrue(taskList.get(1).hasRun());

        /* Tests cancellation on non-periodic events */
        TestTask task = new TestTask();
        timer.schedule(task, new Date(timer.currentTimeInMillis() + 10));
        task.cancel();
        timer.advanceTimeMillis(12, REAL_TIME_DELAY);
        assertFalse(task.hasRun());

    }

    /**
     * Test scheduling beginning after a delay and recurring periodically.
     *
     * @throws Exception throws an exception if the test fails
     */
    @Test
    public void testScheduleByDelayPeriodic() throws Exception {
        /* Test straightforward periodic execution */
        timer.schedule(new TestTask(), 0, 10);
        delay(REAL_TIME_DELAY);
        assertEquals("Task should have run once when added.", 1, taskList.get(0).timesRun());

        /* Tests whether things that are not added to the queue are scheduled for future executions (ones which execute
        immediately on add). */
        timer.advanceTimeMillis(10, REAL_TIME_DELAY);
        assertEquals("Task should have run once when added.", 2, taskList.get(0).timesRun());

        /* Tests whether cancellation works on periodic events. */
        taskList.get(0).cancel();

        timer.advanceTimeMillis(10, REAL_TIME_DELAY);
        assertEquals("The task should not have run another time.", 2, taskList.get(0).timesRun());

        TestTask task = new TestTask();
        timer.schedule(task, 0, 10);
        timer.advanceTimeMillis(100, REAL_TIME_DELAY);
        assertEquals("Should have run immeditaley and subsequently once during the larger skip", task.timesRun(), 2);

    }

    /**
     * Test scheduling beginning at a specified date and recurring periodically.
     *
     * @throws Exception throws an exception if the test fails
     */
    @Ignore("Ignored when running CircleCI")
    @Test
    public void testScheduleByDatePeriodic() throws Exception {
        /* Test straightforward periodic execution */
        timer.schedule(new TestTask(), new Date(timer.currentTimeInMillis()), 10);
        delay(REAL_TIME_DELAY);
        assertEquals("Task should have run once when added.", 1, taskList.get(0).timesRun());

        /* Tests whether things that are not added to the queue are scheduled for future executions (ones which execute
        immediately on add). */
        timer.advanceTimeMillis(10, REAL_TIME_DELAY);
        assertEquals("Task should have run once when added.", 2, taskList.get(0).timesRun());

        /* Tests whether cancellation works on periodic events. */
        taskList.get(0).cancel();

        timer.advanceTimeMillis(10, REAL_TIME_DELAY);
        assertEquals("The task should not have run another time.", 2, taskList.get(0).timesRun());

        TestTask task = new TestTask();
        timer.schedule(task, new Date(timer.currentTimeInMillis()), 10);
        timer.advanceTimeMillis(100, REAL_TIME_DELAY);
        assertEquals("Should have run immediately and subsequently once during the larger skip", task.timesRun(), 2);
    }

    /* Schedule at fixed rate runs exactly like the two scheduling methods just tested so tests are not included */

    /**
     * Timer task with added functions to make it better for testing.
     */
    private class TestTask extends TimerTask {

        /* Remains true once the task has been run at least once */
        private boolean hasRun;

        /* Unique id per event. */
        private int id;

        /* Specifies the number of times an event has run */
        private int timesRun;

        /**
         * Constructor initializes id, timesRun, and id fields.
         */
        public TestTask() {
            id = idGenerator.getAndIncrement();
            timesRun = 0;
            hasRun = false;
            taskList.add(this);
        }

        @Override
        public void run() {
            this.hasRun = true;
            tasksRunCount.incrementAndGet();
            timesRun++;
        }

        /**
         * Returns whether this event has run.
         *
         * @return true if the event has run, false otherwise.
         */
        public boolean hasRun() {
            return hasRun;
        }

        /**
         * Returns the number of times this task has run.
         *
         * @return an int representing the number of times this task has been run
         */
        public int timesRun() {
            return timesRun;
        }

        /**
         * Returns the unique identifier of this task.
         *
         * @return a unique integer identifier
         */
        public int getId() {
            return id;
        }
    }
}
