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

import org.onosproject.yangutils.datamodel.YangSchemaNode;
import org.onosproject.yms.ydt.YdtContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages the application information required for schema nodes defined in
 * the module (sub-module).
 */
public class ModuleSchemaData implements ModuleAppData {

    /*
     * Reference for application's root ydtContext.
     */
    private YdtExtendedContext moduleContext;

    /*
     * Reference for list of nodes with operation type delete.
     */
    private final List<YdtContext> deleteNodes = new ArrayList<>();

    @Override
    public List<YdtContext> getDeleteNodes() {
        // This suppose to be mutable for YAB
        return deleteNodes;
    }

    @Override
    public void addDeleteNodes(YdtContext deletedNode) {
        deleteNodes.add(deletedNode);
    }

    @Override
    public YdtContext getModuleContext() {
        return moduleContext;
    }

    @Override
    public void setModuleContext(YdtExtendedContext moduleContext) {
        this.moduleContext = moduleContext;
    }

    @Override
    public YangSchemaNode getSchemaNode() {
        return moduleContext.getYangSchemaNode();
    }

    @Override
    public YangSchemaNode getRootSchemaNode() {
        return moduleContext.getYangSchemaNode();
    }
}
