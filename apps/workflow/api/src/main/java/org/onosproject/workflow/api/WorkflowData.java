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


/**
 * An abstract class representing workflow data.
 */
public abstract class WorkflowData {

    private DataModelTree data;
    private boolean triggerNext = true; // default is true

    /**
     * Constructor of workflow data.
     * @param data data model tree
     */
    public WorkflowData(DataModelTree data) {
        this.data = data;
    }

    /**
     * Returns name.
     * @return name
     */
    public abstract String name();

    /**
     * Returns work-partition distributor.
     * @return work-partition distributor
     */
    public abstract String distributor();

    /**
     * Returns context model tree.
     * @return context model tree
     */
    public DataModelTree data() {
        return data;
    }

    /**
     * Returns whether to trigger next worklet selection.
     * @return whether to trigger next worklet selection
     */
    public boolean triggerNext() {
        return triggerNext;
    }

    /**
     * Sets whether to handle update event.
     * @param triggerNext whether to handle update event
     */
    public void setTriggerNext(boolean triggerNext) {
        this.triggerNext = triggerNext;
    }

}
