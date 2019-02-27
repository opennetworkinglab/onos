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


import java.util.Map;

/**
 * Workflow DataModel exception class.
 */
public class WorkflowDataModelException extends WorkflowException {

    private String workflowName;
    private Map<String, Map<String, String>> errorListMap;


    /**
     * Constructor for Workflow DataModel Exception.
     *
     * @param msg exception message
     */
    public WorkflowDataModelException(String msg) {

        super(msg);
    }

    /**
     * Constructor for Workflow DataModel Exception.
     *
     * @param msg          exception message
     * @param workflowName workflow name
     * @param errorListMap throwable to deliver
     */
    public WorkflowDataModelException(String msg, String workflowName, Map<String, Map<String, String>> errorListMap) {
        super(msg);
        this.workflowName = workflowName;
        this.errorListMap = errorListMap;
    }

    @Override
    public String toString() {
        return "WorkflowDataModelException{" +
                "workflowName='" + workflowName + '\'' +
                ", errorListMap=" + errorListMap.toString() +
                '}';
    }

}
