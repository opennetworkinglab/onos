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

import java.util.Objects;

import static org.onosproject.workflow.api.CheckCondition.check;

/**
 * Class for event task.
 */
public final class EventTask extends HandlerTask {

    /**
     * Event triggering event task.
     */
    private final Event event;

    /**
     * Event hint value for finding target event.
     */
    private final String eventHint;

    /**
     * Constructor of event task.
     * @param builder builder of event task
     */
    private EventTask(Builder builder) {
        super(builder);
        this.event = builder.event;
        this.eventHint = builder.eventHint;
    }

    /**
     * Gets event of event task.
     * @return event triggering event task
     */
    public Event event() {
        return event;
    }

    /**
     * Gets event type (class name of event) of event task.
     * @return event type
     */
    public String eventType() {
        return event.getClass().getName();
    }

    /**
     * Gets event hint of event task.
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
        return Objects.equals(this.event(), ((EventTask) obj).event())
                && Objects.equals(this.eventType(), ((EventTask) obj).eventType())
                && Objects.equals(this.eventHint(), ((EventTask) obj).eventHint());
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("context", context())
                .add("programCounter", programCounter())
                .add("event", event())
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
     * Builder of EventTask.
     */
    public static class Builder extends HandlerTask.Builder {

        /**
         * Event triggering event task.
         */
        private Event event;

        /**
         * Event hint value for finding target event.
         */
        private String eventHint;

        /**
         * Sets event.
         * @param event event triggering event task
         * @return Builder of EventTask
         */
        public Builder event(Event event) {
            this.event = event;
            return this;
        }

        /**
         * Sets event hint.
         * @param eventHint event hint value for finding target event
         * @return Builder of EventTask
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
        public Builder programCounter(ProgramCounter programCounter) {
            super.programCounter(programCounter);
            return this;
        }

        /**
         * Builds EventTask.
         * @return instance of EventTask
         * @throws WorkflowException workflow exception
         */
        public EventTask build() throws WorkflowException {
            check(event != null, "event is invalid");
            check(eventHint != null, "eventHint is invalid");
            return new EventTask(this);
        }
    }
}
