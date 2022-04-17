/*
 * Copyright 2022-present Open Networking Foundation
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

import java.util.List;
import java.util.Map;

public interface WorkflowLogStore {

    /**
     * Adds log messages to store.
     *
     * @param contextName context name
     * @param logMsg log message
     * @param className class name
     * @param level logging level
     */
    void addLog(String contextName, String logMsg, String className, String level);

    /**
     * Adds exception message and call stack to store.
     *
     * @param contextName context name
     * @param logMsg log message
     * @param className class name
     * @param level logging level
     * @param e Throwable object
     */
    void addException(String contextName, String logMsg, String className, String level, Throwable e);

    /**
     * Gets log messages from store.
     *
     * @param contextName context name
     * @return log messages of given contextName
     */
    List<String> getLog(String contextName);


    /**
     * Get store as Java map.
     *
     * @return Store as Java hash map.
     */
    Map<String, List<String>> asJavaMap();
}
