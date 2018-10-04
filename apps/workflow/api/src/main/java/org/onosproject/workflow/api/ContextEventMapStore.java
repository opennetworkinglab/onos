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


import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.store.service.DocumentPath;
import org.onosproject.store.service.Versioned;

import java.util.Map;

/**
 *  WorkflowContext Event Map Store.
 */
public interface ContextEventMapStore {

    /**
     * Registers workflow context event mapping.
     * @param eventType the class name of event
     * @param eventHint event hint string value of the event
     * @param contextName workflow context name
     * @param workletType the class name of worklet
     * @throws WorkflowException workflow exception
     */
    void registerEventMap(String eventType, String eventHint,
                          String contextName, String workletType) throws WorkflowException;

    /**
     * Unregisters workflow context event mapping.
     * @param eventType the class name of event
     * @param eventHint event hint string value of the event
     * @param contextName workflow context name
     * @throws WorkflowException workflow exception
     */
    void unregisterEventMap(String eventType, String eventHint,
                            String contextName) throws WorkflowException;

    /**
     * Returns workflow context event mapping.
     * @param eventType the class name of event
     * @param eventHint event hint string value of the event
     * @return workflow context event mapping
     * @throws WorkflowException workflow exception
     */
    Map<String, String> getEventMap(String eventType, String eventHint) throws WorkflowException;

    /**
     * Returns child nodes on document tree path.
     * @param path document tree path
     * @return children under document tree path
     * @throws WorkflowException workflow exception
     */
    Map<String, Versioned<String>> getChildren(String path) throws WorkflowException;

    /**
     * Returns document path.
     * @param path document path string
     * @return document tree
     * @throws WorkflowException workflow exception
     */
    DocumentPath getDocumentPath(String path) throws WorkflowException;

    /**
     * Transforms document tree to json tree.
     * @return json tree
     * @throws WorkflowException workflow exception
     */
    ObjectNode asJsonTree() throws WorkflowException;
}

