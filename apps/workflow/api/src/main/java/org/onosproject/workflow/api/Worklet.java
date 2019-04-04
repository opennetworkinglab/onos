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


import org.onosproject.event.Event;

/**
 * An interface representing worklet. A workflow is composed of worklets.
 */
public interface Worklet {

    int MAX_WORKS = 10000;

    /**
     * Returns tag name of worklet. class name is usually used.
     * @return tag name
     */
    String tag();

    /**
     * Processes tasks of the worklet under the workflow context.
     * @param context workflow context
     * @throws WorkflowException workflow exception
     */
    void process(WorkflowContext context) throws WorkflowException;

    /**
     * Checks whether is this worklet next worklet to be done under the workflow context.
     * @param context workflow context
     * @return true means this worklet is the next worklet to be processed
     * @throws WorkflowException workflow exception
     */
    boolean isNext(WorkflowContext context) throws WorkflowException;

    /**
     * Checks whether is this worklet completed or not. 'isCompleted' checking is triggered by an event task.
     * @param context workflow context
     * @param event an event triggering this 'isCompleted' checking
     * @return completed or not
     * @throws WorkflowException workflow exception
     */
    boolean isCompleted(WorkflowContext context, Event event) throws WorkflowException;

    /**
     * Completion event timeout handler.
     * @param context workflow context
     * @throws WorkflowException workflow exception
     */
    void timeout(WorkflowContext context) throws WorkflowException;

    /**
     * Common worklet enum.
     */
    enum Common implements Worklet {

        /**
         * Init worklet.
         */
        INIT {
            @Override
            public String tag() {
                return INIT.name();
            }

            @Override
            public void process(WorkflowContext context) throws WorkflowException {
                throw new WorkflowException("(" + tag() + ").process should not be called");
            }

            @Override
            public boolean isNext(WorkflowContext context) throws WorkflowException {
                throw new WorkflowException("(" + tag() + ").isNext should not be called");
            }

            @Override
            public boolean isCompleted(WorkflowContext context, Event event)throws WorkflowException {
                throw new WorkflowException("(" + tag() + ").isCompleted should not be called");
            }

            @Override
            public void timeout(WorkflowContext context) throws WorkflowException {
                throw new WorkflowException("(" + tag() + ").timeout should not be called");
            }
        },

        /**
         * Completed worklet.
         */
        COMPLETED {
            @Override
            public String tag() {
                return COMPLETED.name();
            }

            @Override
            public void process(WorkflowContext context) throws WorkflowException {
                throw new WorkflowException("(" + tag() + ").process should not be called");
            }

            @Override
            public boolean isNext(WorkflowContext context) throws WorkflowException {
                throw new WorkflowException("(" + tag() + ").isNext should not be called");
            }

            @Override
            public boolean isCompleted(WorkflowContext context, Event event)throws WorkflowException {
                throw new WorkflowException("(" + tag() + ").isCompleted should not be called");
            }

            @Override
            public void timeout(WorkflowContext context) throws WorkflowException {
                throw new WorkflowException("(" + tag() + ").timeout should not be called");
            }
        },

        /**
         * Interrupted worklet.
         */
        INTERRUPTED {
            @Override
            public String tag() {
                return INTERRUPTED.name();
            }

            @Override
            public void process(WorkflowContext context) throws WorkflowException {
                throw new WorkflowException("(" + tag() + ").process should not be called");
            }

            @Override
            public boolean isNext(WorkflowContext context) throws WorkflowException {
                throw new WorkflowException("(" + tag() + ").isNext should not be called");
            }

            @Override
            public boolean isCompleted(WorkflowContext context, Event event)throws WorkflowException {
                throw new WorkflowException("(" + tag() + ").isCompleted should not be called");
            }

            @Override
            public void timeout(WorkflowContext context) throws WorkflowException {
                throw new WorkflowException("(" + tag() + ").timeout should not be called");
            }
        }
    }
}

