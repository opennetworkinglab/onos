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

import org.onosproject.yangutils.datamodel.YangNamespace;

class NameSpace implements YangNamespace {

    /*
     * Reference for namespace.
     */
    private final String nameSpace;

    /**
     * Creates an instance of namespace which is used to initialize the
     * nameSpace for requested YDT node.
     *
     * @param nameSpace namespace of the requested node
     */
    public NameSpace(String nameSpace) {
        this.nameSpace = nameSpace;
    }

    @Override
    public String getModuleNamespace() {
        return nameSpace;
    }

    @Override
    public String getModuleName() {
        return nameSpace;
    }
}
