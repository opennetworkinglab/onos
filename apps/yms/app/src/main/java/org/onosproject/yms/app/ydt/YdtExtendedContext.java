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
import org.onosproject.yms.ydt.YdtContextOperationType;

/**
 * Abstraction of an entity which represents application related information
 * maintained in YDT.
 */
public interface YdtExtendedContext extends YdtContext {

    /**
     * Returns the application stored information. Application type is used to
     * identify application.
     *
     * @param appType application type
     * @return application information
     */
    Object getAppInfo(AppType appType);

    /**
     * Sets application stored information. Application type is used to
     * identify application.
     *
     * @param appType application type
     * @param object  application information object
     */
    void addAppInfo(AppType appType, Object object);

    /**
     * Returns schema node from data model for curNode.
     *
     * @return yang schema node
     */
    YangSchemaNode getYangSchemaNode();

    /**
     * Returns YDT current extended context operation type.
     *
     * @return operation type
     */
    YdtContextOperationType getYdtContextOperationType();
}
