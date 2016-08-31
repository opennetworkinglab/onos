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

package org.onosproject.yms.app.yob;

import org.onosproject.yms.app.ydt.YdtExtendedContext;
import org.onosproject.yms.ydt.YdtType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.onosproject.yms.app.yob.YobConstants.YDT_TYPE_IS_NOT_SUPPORT;
import static org.onosproject.yms.ydt.YdtType.MULTI_INSTANCE_LEAF_VALUE_NODE;
import static org.onosproject.yms.ydt.YdtType.MULTI_INSTANCE_NODE;
import static org.onosproject.yms.ydt.YdtType.SINGLE_INSTANCE_LEAF_VALUE_NODE;
import static org.onosproject.yms.ydt.YdtType.SINGLE_INSTANCE_NODE;

/**
 * Represents an YANG object builder factory to create different types
 * of YANG data tree node.
 */
final class YobHandlerFactory {

    private static final Logger log =
            LoggerFactory.getLogger(YobSingleInstanceLeafHandler.class);

    /**
     * Creates single instance node handler.
     */
    private static YobSingleInstanceHandler singleInstanceNode =
            new YobSingleInstanceHandler();

    /**
     * Creates multi instance node handler.
     */
    private static YobMultiInstanceHandler multiInstanceNode =
            new YobMultiInstanceHandler();

    /**
     * Creates single instance leaf node handler.
     */
    private static YobSingleInstanceLeafHandler singleInstanceLeaf =
            new YobSingleInstanceLeafHandler();

    /**
     * Creates multi instance leaf node handler.
     */
    private static YobMultiInstanceLeafHandler multiInstanceLeaf =
            new YobMultiInstanceLeafHandler();

    /**
     * Map of YANG object builder handler.
     */
    private static Map<YdtType, YobHandler> yobHandlerHashMap =
            new HashMap<>();

    /**
     * Create instance of YobHandlerFactory.
     */
    YobHandlerFactory() {
        yobHandlerHashMap.put(SINGLE_INSTANCE_NODE, singleInstanceNode);
        yobHandlerHashMap.put(MULTI_INSTANCE_NODE, multiInstanceNode);
        yobHandlerHashMap.put(SINGLE_INSTANCE_LEAF_VALUE_NODE,
                              singleInstanceLeaf);
        yobHandlerHashMap.put(MULTI_INSTANCE_LEAF_VALUE_NODE,
                              multiInstanceLeaf);
    }

    /**
     * Returns the corresponding YOB handler for current context.
     *
     * @param ydtExtendedContext ydtExtendedContext is used to get application
     *                           related information maintained in YDT
     * @return YANG object builder node
     */
    YobHandler getYobHandlerForContext(
            YdtExtendedContext ydtExtendedContext) {
        YobHandler yobHandler =
                yobHandlerHashMap.get(ydtExtendedContext.getYdtType());
        if (yobHandler == null) {
            log.error(YDT_TYPE_IS_NOT_SUPPORT);
            return null;
        }
        return yobHandler;
    }
}
