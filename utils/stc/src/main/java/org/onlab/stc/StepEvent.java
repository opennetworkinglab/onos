/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onlab.stc;

import org.onlab.stc.Coordinator.Status;

import static java.lang.Long.parseLong;

/**
 * Represents an event of execution of a scenario step or group.
 */
public class StepEvent {

    private final String name;
    private final long time;
    private final Status status;

    /**
     * Creates a new step record.
     *
     * @param name   test step or group name
     * @param time   time in millis since start of epoch
     * @param status step completion status
     */
    public StepEvent(String name, long time, Status status) {
        this.name = name;
        this.time = time;
        this.status = status;
    }

    /**
     * Creates a new step record for non-running status.
     *
     * @param name   test step or group name
     * @param status status
     */
    public StepEvent(String name, Status status) {
        this(name, System.currentTimeMillis(), status);
    }

    /**
     * Returns the test step or test group name.
     *
     * @return step or group name
     */
    public String name() {
        return name;
    }

    /**
     * Returns the step event  time.
     *
     * @return time in millis since start of epoch
     */
    public long time() {
        return time;
    }

    /**
     * Returns the step completion status.
     *
     * @return completion status
     */
    public Status status() {
        return status;
    }


    @Override
    public String toString() {
        return name + ":" + time + ":" + status;
    }

    /**
     * Returns a record parsed from the specified string.
     *
     * @param string string encoding
     * @return step record
     */
    public static StepEvent fromString(String string) {
        String[] fields = string.split(":");
        return new StepEvent(fields[0], parseLong(fields[1]), Status.valueOf(fields[2]));
    }
}
