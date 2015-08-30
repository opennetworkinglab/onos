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

/**
 * Entity capable of receiving notifications of process step execution events.
 */
public interface StepProcessListener {

    /**
     * Indicates that process step has started.
     *
     * @param step    subject step
     * @param command actual command executed; includes run-time substitutions
     */
    default void onStart(Step step, String command) {
    }

    /**
     * Indicates that process step has completed.
     *
     * @param step   subject step
     * @param status step completion status
     */
    default void onCompletion(Step step, Coordinator.Status status) {
    }

    /**
     * Notifies when a new line of output becomes available.
     *
     * @param step subject step
     * @param line line of output
     */
    default void onOutput(Step step, String line) {
    }

}
