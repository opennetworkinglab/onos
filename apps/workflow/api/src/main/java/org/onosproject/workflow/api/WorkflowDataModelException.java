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


import com.fasterxml.jackson.databind.JsonNode;

import java.net.URI;
import java.util.List;

/**
 * Workflow DataModel exception class.
 */
public class WorkflowDataModelException extends WorkflowException {

    private URI workflowId;
    private JsonNode parameterJson;
    private List<String> errorMsgs;

    /**
     * Default Constructor for Workflow DataModel Exception.
     *
     * @param msg exception message
     */
    public WorkflowDataModelException(String msg) {

        super(msg);
    }

    /**
     * Constructor for Workflow DataModel Exception.
     *
     * @param workflowId id of workflow
     * @param parameterJson paramter json data model
     * @param errorMsgs error message for json data model
     */
    public WorkflowDataModelException(URI workflowId, JsonNode parameterJson, List<String> errorMsgs) {
        super("Invalid workflow data model: " +
                " workflow: " + workflowId.toString() +
                ", parameter json: " + parameterJson.toString() +
                ", errors: " + errorMsgs);
        this.workflowId = workflowId;
        this.parameterJson = parameterJson;
        this.errorMsgs = errorMsgs;
    }
}
