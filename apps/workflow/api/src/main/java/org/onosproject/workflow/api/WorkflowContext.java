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

import java.net.URI;
import java.util.Set;

/**
 * An abstract class representing WorkflowContext.
 */
public abstract class WorkflowContext extends WorkflowData {

    /**
     * Constructor of workflow context.
     * @param data data model tree
     */
    public WorkflowContext(DataModelTree data) {
        super(data);
    }

    /**
     * Returns workflow id of this workflow context.
     * @return workflow id
     */
    public abstract URI workflowId();

    /**
     * Returns workplace name.
     * @return workplace name
     */
    public abstract String workplaceName();

    /**
     * Returns the current state of workflow context.
     * @return current state of workflow context
     */
    public abstract WorkflowState state();

    /**
     * Sets the current state of workflow context.
     * @param state current state of workflow context
     */
    public abstract void setState(WorkflowState state);

    /**
     * Sets the current program counter of workflow context.
     * @param pc current program counter
     */
    public abstract void setCurrent(ProgramCounter pc);

    /**
     * Returns the current program counter of workflow.
     * @return the current program counter of workflow
     */
    public abstract ProgramCounter current();

    /**
     * Returns the cause string of exception state.
     * @return cause string
     */
    public abstract String cause();

    /**
     * Sets the cause string of exception state.
     * @param cause cause string
     */
    public abstract void setCause(String cause);

    /**
     * Indicates the worklet process become completed.
     * By calling this, workflow triggers the next worklet selection
     */
    public abstract void completed();

    /**
     * Waits an event which have 'eventHint' after executing executor.
     * If the event happens, Worklet.isCompleted will be called.
     * If the event does not happen for timeoutMs, Worklet.timeout will be called.
     * @param eventType the class of event to wait
     * @param eventHint the event of the event to wait
     * @param eventGenerator a method reference to be executed after executing executor
     * @param timeoutMs timeout millisecond
     */
    public abstract void waitCompletion(Class<? extends Event> eventType, String eventHint,
                                        WorkExecutor eventGenerator, long timeoutMs);


    /**
     * Waits an event which has any one of eventHint from Set 'eventHintSet' after executing executor.
     * If the event happens, Worklet.isCompleted will be called.
     * If the event does not happen for timeoutMs, Worklet.timeout will be called.
     * @param eventType the class of event to wait
     * @param eventHintSet the Set of eventHints of the event to wait
     * @param eventGenerator a method reference to be executed after executing executor
     * @param timeoutMs timeout millisecond
     */
    public abstract void waitAnyCompletion(Class<? extends Event> eventType, Set<String> eventHintSet,
                                           WorkExecutor eventGenerator, long timeoutMs);


    /**
     * Waits timeout milliseconds. After timeoutMs Worklet.timeout will be called.
     * @param timeoutMs timeout millisecond
     */
    public abstract void waitFor(long timeoutMs);

    /**
     * Returns the class of a completion event to wait.
     * @return the class of a completion event
     */
    public abstract Class<? extends Event> completionEventType();

    /**
     * Returns the set of event hint string to wait.
     * @return the event hint string set
     */
    public abstract Set<String> completionEventHints();

    /**
     * Returns method reference for generating completion event.
     * @return a method reference
     */
    public abstract WorkExecutor completionEventGenerator();

    /**
     * Returns completion event timeout.
     * @return completion event timeout
     */
    public abstract long completionEventTimeout();

    /**
     * Sets workflow service.
     * @param workflowExecutionService workflow service
     */
    public abstract void setWorkflowExecutionService(WorkflowExecutionService workflowExecutionService);

    /**
     * Gets workflow service.
     * @return workflow service
     */
    public abstract WorkflowExecutionService workflowService();

    /**
     * Sets workflow store.
     * @param workflowStore workflow store.
     */
    public abstract void setWorkflowStore(WorkflowStore workflowStore);

    /**
     * Gets worklow store.
     * @return workflow store
     */
    public abstract WorkflowStore workflowStore();

    /**
     * Sets workplace store.
     * @param workplaceStore work place store.
     */
    public abstract void setWorkplaceStore(WorkplaceStore workplaceStore);

    /**
     * Gets workplace store.
     * @return workplace store
     */
    public abstract WorkplaceStore workplaceStore();

    /**
     * Get service.
     * @param serviceClass service class
     * @param <T> service class type
     * @return service reference
     * @throws WorkflowException workflow exception
     */
    public abstract <T> T getService(Class<T> serviceClass) throws WorkflowException;
}
