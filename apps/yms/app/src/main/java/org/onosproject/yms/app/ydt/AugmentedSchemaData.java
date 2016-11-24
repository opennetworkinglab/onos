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

import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.datamodel.YangSchemaNode;

/**
 * Manages the application information required for schema nodes defined in
 * the module (sub-module).
 */
public class AugmentedSchemaData implements AugmentAppData {

    /*
     * Reference for schema node of augmenting application.
     */
    private YangSchemaNode augModSchema;

    @Override
    public YangSchemaNode getAugmentingSchemaNode() {
        return augModSchema;
    }

    @Override
    public void setAugmentingSchemaNode(YangSchemaNode schemaNode) {
        augModSchema = schemaNode;
    }

    @Override
    public YangSchemaNode getSchemaNode() {
        return augModSchema;
    }

    @Override
    public YangSchemaNode getRootSchemaNode() {
        return ((YangNode) getSchemaNode()).getParent();
    }
}
