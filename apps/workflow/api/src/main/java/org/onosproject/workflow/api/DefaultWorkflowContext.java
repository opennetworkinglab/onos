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
import org.onlab.osgi.DefaultServiceDirectory;
import org.onlab.osgi.ServiceNotFoundException;
import org.onosproject.event.Event;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;

import static org.onosproject.workflow.api.CheckCondition.check;

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
     * Current program counter of the workflow.
     */
    private ProgramCounter current;

    /**
     * Cause of workflow exception.
     */
    private String cause;

    /**
     * Completion event type.
     */
    private transient Class<? extends Event> completionEventType;

    /**
     * Completion event hint Set.
     */
    private transient Set<String> completionEventHintSet;

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
     * @param builder default workflow context builder
     */
    protected DefaultWorkflowContext(Builder builder) {
        super(builder.data);
        this.workflowId = builder.workflowId;
        this.workplaceName = builder.workplaceName;
        this.state = WorkflowState.IDLE;
        this.current = ProgramCounter.INIT_PC;
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
    public ProgramCounter current() {
        return this.current;
    }

    @Override
    public void setCurrent(ProgramCounter pc) {
        this.current = pc;
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
        this.completionEventHintSet = new HashSet<>();
        this.completionEventHintSet.add(eventHint);
        this.completionEventGenerator = eventGenerator;
        this.completionEventTimeoutMs = timeoutMs;
    }

    @Override
    public void waitAnyCompletion(Class<? extends Event> eventType, Set<String> eventHint,
                                  WorkExecutor eventGenerator, long timeoutMs) {
        this.completionEventType = eventType;
        this.completionEventHintSet = new HashSet<>();
        this.completionEventHintSet.addAll(eventHint);
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
    public Set<String> completionEventHints() {
        return completionEventHintSet;
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

    public <T> T getService(Class<T> serviceClass) throws WorkflowException {
        T service;
        try {
            service = DefaultServiceDirectory.getService(serviceClass);
        } catch (ServiceNotFoundException e) {
            throw new WorkflowException(e);
        }
        return service;
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

    /**
     * Gets builder instance.
     * @return builder instance
     */
    public static final Builder builder() {
        return new Builder();
    }

    /**
     * Builder for default workflow context.
     */
    public static class Builder {

        /**
         * ID of workflow.
         */
        private URI workflowId;

        /**
         * Workplace name of the workflow.
         */
        private String workplaceName;

        /**
         * Data model tree.
         */
        private DataModelTree data;

        /**
         * Sets workflow id.
         * @param workflowId workflow id
         * @return builder
         */
        public Builder workflowId(URI workflowId) {
            this.workflowId = workflowId;
            return this;
        }

        /**
         * Sets workplace name.
         * @param workplaceName workplace name
         * @return builder
         */
        public Builder workplaceName(String workplaceName) {
            this.workplaceName = workplaceName;
            return this;
        }

        /**
         * Sets data model tree.
         * @param data data model tree
         * @return builder
         */
        public Builder data(DataModelTree data) {
            this.data = data;
            return this;
        }

        /**
         * Builds default workflow context.
         * @return instance of default workflow context
         * @throws WorkflowException workflow exception
         */
        public DefaultWorkflowContext build() throws WorkflowException {
            check(data != null, "Invalid data model tree");
            check(workflowId != null, "Invalid workflowId");
            check(workplaceName != null, "Invalid workplaceName");
            return new DefaultWorkflowContext(this);
        }
    }
}
