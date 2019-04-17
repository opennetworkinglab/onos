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
 * Class for WorkFlow Trigger event task.
 */
public final class WfTriggerEventTask extends HandlerTask {

    /**
     * Event triggering event task.
     */
    private final Event event;

    /**
     * Constructor of event task.
     * @param builder builder of event task
     */
    private WfTriggerEventTask(Builder builder) {
        super(builder);
        this.event = builder.event;
    }


    /**
     * Gets event of event task.
     * @return event triggering event task
     */
    public Event event() {
        return event;
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
        return Objects.equals(this.event(),
                              ((WfTriggerEventTask) obj).event());
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("context", context())
                .add("event", event())
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
     * Builder of WfTriggerEventTask.
     */
    public static class Builder extends HandlerTask.Builder {

        /**
         * Event triggering event task.
         */
        private Event event;


        /**
         * Sets event.
         * @param event event triggering event task
         * @return Builder of WfTriggerEventTask
         */
        public Builder event(Event event) {
            this.event = event;
            return this;
        }

        @Override
        public Builder context(WorkflowContext context) {
            super.context(context);
            return this;
        }

        /**
         * Builds WfTriggerEventTask.
         * @return instance of WfTriggerEventTask
         * @throws WorkflowException workflow exception
         */
        public WfTriggerEventTask build() throws WorkflowException {
            check(context != null, "context is invalid");
            check(event != null, "event is invalid");
            return new WfTriggerEventTask(this);
        }
    }
}
