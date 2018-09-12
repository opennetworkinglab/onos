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
import java.net.URI;


/**
 * interface for workflow description.
 */
public interface WorkflowDescription {

    /**
     * Workflow workplace field name.
     */
    String WF_WORKPLACE = "workplace";

    /**
     * Workflow ID field name.
     */
    String WF_ID = "id";

    /**
     * Workflow data field name.
     */
    String WF_DATA = "data";

    /**
     * Gets workplace name.
     * @return workplace name
     */
    String workplaceName();

    /**
     * Gets workflow ID.
     * @return workflow ID
     */
    URI id();

    /**
     * Gets workflow context name.
     * @return workflow context name
     */
    String workflowContextName();

    /**
     * Gets workflow data model.
     * @return workflow data model
     */
    JsonNode data();

    /**
     * Gets json of workflow description.
     * @return json of workflow description
     */
    JsonNode toJson();
}
