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
 * Common data model tree path.
 */
public interface DataModelPointer {

    /**
     * Workplace array pointer.
     */
    String WORKPLACES_PTR = "/workplaces";

    /**
     * Workplace name pointer.
     */
    String WORKPLACE_NAME_PTR = "/name";

    /**
     * Workplace data pointer.
     */
    String WORKPLACE_DATA_PTR = "/data";

    /**
     * Workplace workflow pointer.
     */
    String WORKPLACE_WORKFLOWS_PTR = "/workflows";

    /**
     * Workflow op pointer.
     */
    String WORKFLOW_OP_PTR = "/op";

    /**
     * Workflow id pointer.
     */
    String WORKFLOW_ID_PTR = "/id";

    /**
     * Workflow data pointer.
     */
    String WORKFLOW_DATA_PTR = "/data";

    /**
     * Gets path string.
     * @return path string
     */
    String getPath();
}

