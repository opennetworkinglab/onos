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


import com.fasterxml.jackson.databind.JsonNode;

/**
 * Interface for workflow service.
 */
public interface WorkflowService {


    /**
     * Registers workflow.
     * @param workflow registering workflow
     * @throws WorkflowException workflow exception
     */
    void register(Workflow workflow) throws WorkflowException;

    /**
     * Creates workplace.
     * @param wpDesc workplace description
     * @throws WorkflowException workflow exception
     */
    void createWorkplace(WorkplaceDescription wpDesc) throws WorkflowException;

    /**
     * Removes workplace.
     * @param wpDesc workplace description
     * @throws WorkflowException workflow exception
     */
    void removeWorkplace(WorkplaceDescription wpDesc) throws WorkflowException;

    /**
     * Clears all workplaces.
     * @throws WorkflowException workflow exception
     */
    void clearWorkplace() throws WorkflowException;

    /**
     * Invokes workflow.
     * @param wfDesc workflow description
     * @throws WorkflowException workflow exception
     */
    void invokeWorkflow(WorkflowDescription wfDesc) throws WorkflowException;

    /**
     * Invokes workflow.
     * @param worklowDescJson workflow description json
     * @throws WorkflowException workflow exception
     */
    void invokeWorkflow(JsonNode worklowDescJson) throws WorkflowException;

    /**
     * Terminates workflow.
     * @param wfDesc workflow description
     * @throws WorkflowException workflow exception
     */
    void terminateWorkflow(WorkflowDescription wfDesc) throws WorkflowException;
}