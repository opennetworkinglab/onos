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

package org.onosproject.yms.app.ytb;

import org.onosproject.yms.app.ydt.YdtExtendedBuilder;
import org.onosproject.yms.app.ydt.YdtExtendedContext;
import org.onosproject.yms.app.ysr.YangSchemaRegistry;
import org.onosproject.yms.ydt.YmsOperationType;

import java.util.List;

/**
 * Abstraction of an entity which provides interfaces to build YANG data tree
 * from the object received from YNH, YAB or YCH.
 */
public interface YangTreeBuilder {

    /**
     * Returns the YDT builder after building the tree corresponding to the
     * response YANG object received from any of the protocol such as YAB or
     * YCH.
     *
     * @param moduleObj     application module object
     * @param rootName      logical root node name
     * @param rootNameSpace logical root node namespace
     * @param opType        root node operation type
     * @param registry      application schema registry
     * @return YDT builder from the tree
     */
    YdtExtendedBuilder getYdtBuilderForYo(List<Object> moduleObj,
                                          String rootName,
                                          String rootNameSpace,
                                          YmsOperationType opType,
                                          YangSchemaRegistry registry);

    /**
     * Returns the YDT context after building the tree received from the
     * protocol YNH.
     *
     * @param object   application notification object
     * @param rootName logical root node name
     * @param registry application schema registry
     * @return YDT context from the tree
     */
    YdtExtendedContext getYdtForNotification(Object object, String rootName,
                                             YangSchemaRegistry registry);

    /**
     * Returns the YDT context after building the RPC response tree. The input
     * for building the tree is RPC request builder, RPC output java object.
     * These are received from the YSB protocol.
     *
     * @param outputObj  application output object
     * @param reqBuilder RPC request builder from YDT
     * @return YDT builder where RPC response tree is created
     */
    YdtExtendedBuilder getYdtForRpcResponse(Object outputObj,
                                            YdtExtendedBuilder reqBuilder);
}
