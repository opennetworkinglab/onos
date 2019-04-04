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

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static org.onosproject.workflow.api.CheckCondition.check;

/**
 * Class for event timeout task.
 */
public final class EventTimeoutTask extends HandlerTask {

    /**
     * Event type (Class name of event).
     */
    private final String eventType;

    /**
     * Set of Event hint value for finding target event.
     */
    private final Set<String> eventHintSet = new HashSet<>();

    /**
     * Constructor of EventTimeoutTask.
     * @param builder builder of EventTimeoutTask
     */
    private EventTimeoutTask(Builder builder) {
        super(builder);
        this.eventType = builder.eventType;
        this.eventHintSet.addAll(builder.eventHintSet);
    }

    /**
     * Gets event type (Class name of event).
     * @return event type
     */
    public String eventType() {
        return eventType;
    }

    /**
     * Gets set of event hint value for finding target event.
     * @return event hint set
     */
    public Set<String> eventHintSet() {
        return eventHintSet;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.toString());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof EventTask)) {
            return false;
        }
        return Objects.equals(this.eventType(), ((EventTask) obj).eventType())
                && Objects.equals(this.eventHintSet(), ((EventTask) obj).eventHint());
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("context", context())
                .add("programCounter", programCounter())
                .add("eventType", eventType())
                .add("eventHint", eventHintSet())
                .toString();
    }

    /**
     * Gets a instance of builder.
     * @return instance of builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder of EventTimeoutTask.
     */
    public static class Builder extends HandlerTask.Builder {
        /**
         * Event type (Class name of event).
         */
        private String eventType;

        /**
         * Set of Event hint value for finding target event.
         */
        private Set<String> eventHintSet;

        /**
         * Sets Event type (Class name of event).
         * @param eventType event type
         * @return builder of EventTimeoutTask
         */
        public Builder eventType(String eventType) {
            this.eventType = eventType;
            return this;
        }

        /**
         * Sets event hint string for finding target event.
         * @param eventHintSet Set of event hint string
         * @return builder of EventTimeoutTask
         */
        public Builder eventHintSet(Set<String> eventHintSet) {
            this.eventHintSet = eventHintSet;
            return this;
        }

        @Override
        public Builder context(WorkflowContext context) {
            super.context(context);
            return this;
        }

        @Override
        public Builder programCounter(ProgramCounter programCounter) {
            super.programCounter(programCounter);
            return this;
        }

        /**
         * Builds EventTimeoutTask.
         * @return instance of EventTimeoutTask
         * @throws WorkflowException workflow exception
         */
        public EventTimeoutTask build() throws WorkflowException {
            check(eventType != null, "eventType is invalid");
            check(eventHintSet != null, "eventHintSet is invalid");
            return new EventTimeoutTask(this);
        }
    }
}
