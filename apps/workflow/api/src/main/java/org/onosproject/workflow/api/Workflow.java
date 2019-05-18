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

import java.net.URI;
import java.util.List;
import java.util.Set;

/**
 * An interface representing workflow.
 */
public interface Workflow {

    /**
     * Id of workflow.
     *
     * @return id
     */
    URI id();

    /**
     * Returns init worklet.
     *
     * @param context workflow context
     * @return init worklet
     * @throws WorkflowException workflow exception
     */
    Worklet init(WorkflowContext context) throws WorkflowException;

    /**
     * Returns next program counter.
     *
     * @param context workflow context
     * @return next program counter
     * @throws WorkflowException workflow exception
     */
    ProgramCounter next(WorkflowContext context) throws WorkflowException;

    /**
     * Gets increased program coounter.
     *
     * @param pc program counter
     * @return increased program counter
     * @throws WorkflowException workflow exception
     */
    ProgramCounter increased(ProgramCounter pc) throws WorkflowException;

    /**
     * Returns instance of worklet.
     *
     * @param pc program counter
     * @return instance of worklet
     * @throws WorkflowException workflow exception
     */
    Worklet getWorkletInstance(ProgramCounter pc) throws WorkflowException;

    /**
     * Builds workflow context.
     *
     * @param workplace workplace of system workflow
     * @param data      data model of system workflow context
     * @return workflow context
     * @throws WorkflowException workflow exception
     */
    WorkflowContext buildContext(Workplace workplace, DataModelTree data) throws WorkflowException;

    /**
     * Builds system workflow context.
     *
     * @param workplace workplace of system workflow
     * @param data      data model of system workflow context
     * @return system workflow context
     * @throws WorkflowException workflow exception
     */
    WorkflowContext buildSystemContext(Workplace workplace, DataModelTree data) throws WorkflowException;

    /**
     * Returns workflow attributes.
     *
     * @return attributes
     */
    Set<WorkflowAttribute> attributes();

    /**
     * Build the list of ProgramCounters, and returns.
     *
     * @return program counter list
     */
    List<ProgramCounter> getProgram();

    /**
     * Returns worklet description.
     * @param pc program counter
     * @return worklet description list
     */
    WorkletDescription getWorkletDesc(ProgramCounter pc);

}
