/*
 * Copyright 2019-present Open Networking Foundation
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
 * A class representing meta data for workflow event.
 */
public class WorkflowEventMetaData {

    private boolean isTriggerSet = false;
    private ProgramCounter programCounter;

    /**
     * Constructor of workflow event meta data.
     * @param isTriggerSet trigger event set for the the workflow
     * @param programCounter program counter representing worklet type for registered event
     */
    public WorkflowEventMetaData(boolean isTriggerSet, ProgramCounter programCounter) {
        this.isTriggerSet = isTriggerSet;
        this.programCounter = programCounter;
    }

    /**
     * Copy constructor of workflow event meta data.
     * @param workflowEventMetaData object of WorkflowEventMetaData
     */
    public WorkflowEventMetaData(WorkflowEventMetaData workflowEventMetaData) {
        this.isTriggerSet = workflowEventMetaData.getTriggerFlag();
        this.programCounter = workflowEventMetaData.getProgramCounter().clone();
    }

    /**
     * Returns program counter value related to worflow event.
     * @return programCounter
     */
    public ProgramCounter getProgramCounter() {
        return programCounter;
    }

    /**
     * Returns trigger flag for the workflow.
     * @return triggerFlag
     */
    public boolean getTriggerFlag() {
        return isTriggerSet;
    }


    /**
     * Sets true or false for triggerFlag of the workflow.
     * @param triggerFlag flag to indicate trigger event set for the workflow
     */
    public void setTriggerFlag(boolean triggerFlag) {
        this.isTriggerSet = triggerFlag;
    }

    /**
     * Sets program counter representing worklet type for registered event of the workflow.
     * @param programCounter program counter representing worklet type for registered event
     */
    public void setProgramCounterString(ProgramCounter programCounter) {
        this.programCounter = programCounter;
    }


    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("trigger-flag", getTriggerFlag())
                .add("program-counter", getProgramCounter())
                .toString();
    }

}
