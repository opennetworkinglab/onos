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
 * Abstract class for worklet.
 */
public abstract class AbstractWorklet implements Worklet {

    @Override
    public String tag() {
        return this.getClass().getName();
    }

    @Override
    public boolean isCompleted(WorkflowContext context, Event event)throws WorkflowException {
        throw new WorkflowException("(" + tag() + ").isCompleted should not be called");
    }

    @Override
    public boolean isNext(WorkflowContext context) throws WorkflowException {
        throw new WorkflowException("(" + tag() + ").isNext should not be called");
    }

    @Override
    public void timeout(WorkflowContext context) throws WorkflowException {
        throw new WorkflowException("Timeout happened");
    }
}

