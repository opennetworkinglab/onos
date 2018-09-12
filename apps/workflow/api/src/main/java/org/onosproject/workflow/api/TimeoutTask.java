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


public final class TimeoutTask extends HandlerTask {

    private TimeoutTask(Builder builder) {
        super(builder);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("context", context())
                .add("workletType", workletType())
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
     * Builder of TimeoutTask.
     */
    public static class Builder extends HandlerTask.Builder {
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
         * Builds TimeoutTask.
         * @return instance of TimeoutTask
         */
        public TimeoutTask build() {
            return new TimeoutTask(this);
        }
    }
}
