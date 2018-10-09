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

/**
 * Abstract class for handler task.
 */
public abstract class HandlerTask {

    /**
     * Workflow context of handler task.
     */
    private final WorkflowContext context;

    /**
     * Program counter of handler task.
     */
    private final ProgramCounter programCounter;

    /**
     * Constructor for handler task.
     * @param builder handler task builder
     */
    protected HandlerTask(Builder builder) {
        this.context = builder.context;
        this.programCounter = builder.programCounter;
    }

    /**
     * Returns workflow context of this handler task.
     * @return workflow context
     */
    public WorkflowContext context() {
        return context;
    }

    /**
     * Returns program counter of this handler task.
     * @return program counter
     */
    public ProgramCounter programCounter() {
        return programCounter;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("context", context())
                .add("programCounter", programCounter())
                .toString();
    }

    /**
     * Builder of HandlerTask.
     */
    public static class Builder {
        protected WorkflowContext context;
        protected ProgramCounter programCounter;

        /**
         * Sets workflow context of handler task.
         * @param context workflow context
         * @return builder of handler task
         */
        public Builder context(WorkflowContext context) {
            this.context = context;
            return this;
        }

        /**
         * Sets program counter of handler task.
         * @param programCounter program counter of handler type
         * @return builder of handler task
         */
        public Builder programCounter(ProgramCounter programCounter) {
            this.programCounter = programCounter;
            return this;
        }
    }
}
