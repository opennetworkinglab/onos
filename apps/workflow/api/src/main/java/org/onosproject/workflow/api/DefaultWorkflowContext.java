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

import com.google.common.base.MoreObjects;
import org.onosproject.event.Event;

import java.net.URI;

/**
 * Default implementation of WorkflowContext.
 */
public class DefaultWorkflowContext extends WorkflowContext {

    /**
     * ID of workflow.
     */
    private URI workflowId;

    /**
     * Workplace name of the workflow.
     */
    private String workplaceName;

    /**
     * State of workflow.
     */
    private WorkflowState state;

    /**
     * Current worklet of the workflow.
     */
    private String current;

    /**
     * Cause of workflow exception.
     */
    private String cause;

    /**
     * Completion event type.
     */
    private transient Class<? extends Event> completionEventType;

    /**
     * Completion event hint.
     */
    private transient String completionEventHint;

    /**
     * Completion event generator method reference.
     */
    private transient WorkExecutor completionEventGenerator;

    /**
     * Completion event timeout milliseconds.
     */
    private transient long completionEventTimeoutMs;

    /**
     * Service reference for workflow service.
     */
    private transient WorkflowExecutionService workflowExecutionService;

    /**
     * Service reference for workflow store.
     */
    private transient WorkflowStore workflowStore;

    /**
     * Service reference for workplace store.
     */
    private transient WorkplaceStore workplaceStore;

    /**
     * Constructor of DefaultWorkflowContext.
     * @param workflowId ID of workflow
     * @param workplaceName name of workplace
     * @param data data model tree
     */
    public DefaultWorkflowContext(URI workflowId, String workplaceName, DataModelTree data) {
        super(data);
        this.workflowId = workflowId;
        this.workplaceName = workplaceName;
        this.state = WorkflowState.IDLE;
        this.current = Worklet.Common.INIT.name();
    }

    /**
     * DefaultWorkflowContext name builder.
     * @param workflowid workflow id
     * @param workplacename workplace name
     * @return DefaultWorkflowContext name
     */
    public static String nameBuilder(URI workflowid, String workplacename) {
        return workflowid.toString() + "@" + workplacename;
    }

    @Override
    public String name() {
        return nameBuilder(workflowId, workplaceName);
    }

    @Override
    public String distributor() {
        return workplaceName();
    }

    @Override
    public URI workflowId() {
        return this.workflowId;
    }

    @Override
    public String workplaceName() {
        return workplaceName;
    }

    @Override
    public WorkflowState state() {
        return state;
    }

    @Override
    public void setState(WorkflowState state) {
        this.state = state;
    }

    @Override
    public String current() {
        return this.current;
    }

    @Override
    public void setCurrent(Worklet worklet) {
        this.current = worklet.tag();
    }

    @Override
    public String cause() {
        return this.cause;
    }

    @Override
    public void setCause(String cause) {
        this.cause = cause;
    }

    @Override
    public void completed() {
        setTriggerNext(true);
    }

    @Override
    public void waitCompletion(Class<? extends Event> eventType, String eventHint,
                               WorkExecutor eventGenerator, long timeoutMs) {
        this.completionEventType = eventType;
        this.completionEventHint = eventHint;
        this.completionEventGenerator = eventGenerator;
        this.completionEventTimeoutMs = timeoutMs;
    }

    @Override
    public void waitFor(long timeoutMs) {
        this.completionEventTimeoutMs = timeoutMs;
    }

    @Override
    public Class<? extends Event> completionEventType() {
        return completionEventType;
    }

    @Override
    public String completionEventHint() {
        return completionEventHint;
    }

    @Override
    public WorkExecutor completionEventGenerator() {
        return completionEventGenerator;
    }

    @Override
    public long completionEventTimeout() {
        return completionEventTimeoutMs;
    }

    @Override
    public void setWorkflowExecutionService(WorkflowExecutionService workflowExecutionService) {
        this.workflowExecutionService = workflowExecutionService;
    }

    @Override
    public WorkflowExecutionService workflowService() {
        return workflowExecutionService;
    }

    @Override
    public void setWorkflowStore(WorkflowStore workflowStore) {
        this.workflowStore = workflowStore;
    }

    @Override
    public WorkflowStore workflowStore() {
        return workflowStore;
    }

    @Override
    public void setWorkplaceStore(WorkplaceStore workplaceStore) {
        this.workplaceStore = workplaceStore;
    }

    @Override
    public WorkplaceStore workplaceStore() {
        return workplaceStore;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("name", name())
                .add("triggernext", triggerNext())
                .add("data", data())
                .add("current", current)
                .add("state", state())
                .add("cause", cause())
                .toString();
    }
}
