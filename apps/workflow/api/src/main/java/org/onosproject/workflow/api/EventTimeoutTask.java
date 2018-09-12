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

import java.util.Objects;

/**
 * Class for event timeout task.
 */
public final class EventTimeoutTask extends HandlerTask {

    /**
     * Event type (Class name of event).
     */
    private final String eventType;

    /**
     * Event hint value for finding target event.
     */
    private final String eventHint;

    /**
     * Constructor of EventTimeoutTask.
     * @param builder builder of EventTimeoutTask
     */
    private EventTimeoutTask(Builder builder) {
        super(builder);
        this.eventType = builder.eventType;
        this.eventHint = builder.eventHint;
    }

    /**
     * Gets event type (Class name of event).
     * @return event type
     */
    public String eventType() {
        return eventType;
    }

    /**
     * Gets event hint value for finding target event.
     * @return event hint string
     */
    public String eventHint() {
        return eventHint;
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
                && Objects.equals(this.eventHint(), ((EventTask) obj).eventHint());
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("context", context())
                .add("workletType", workletType())
                .add("eventType", eventType())
                .add("eventHint", eventHint())
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
         * Event hint value for finding target event.
         */
        private String eventHint;

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
         * @param eventHint event hint string
         * @return builder of EventTimeoutTask
         */
        public Builder eventHint(String eventHint) {
            this.eventHint = eventHint;
            return this;
        }

        @Override
        public Builder context(WorkflowContext context) {
            super.context(context);
            return this;
        }

        @Override
        public Builder workletType(String workletType) {
            super.workletType(workletType);
            return this;
        }

        /**
         * Builds EventTimeoutTask.
         * @return instance of EventTimeoutTask
         */
        public EventTimeoutTask build() {
            return new EventTimeoutTask(this);
        }
    }
}
