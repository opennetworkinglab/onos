/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.yms.app.ydt;

import org.onosproject.yms.ydt.YdtContext;

import java.util.List;

/**
 * Represents a module/sub-module node in application tree.
 */
interface ModuleAppData extends AppData {

    /**
     * Returns the list of nodes with operation type delete.
     *
     * @return list of nodes with operation type delete
     */
    List<YdtContext> getDeleteNodes();

    /**
     * Adds the ydt node with operation type delete in module delete node list.
     *
     * @param node ydt node with operation type delete/remove
     */
    void addDeleteNodes(YdtContext node);

    /**
     * Returns application's root ydtContext.
     *
     * @return YdtContext of application root node
     */
    YdtContext getModuleContext();

    /**
     * Sets the application's ydtContext.
     *
     * @param moduleNode application's ydtContext
     */
    void setModuleContext(YdtExtendedContext moduleNode);
}
