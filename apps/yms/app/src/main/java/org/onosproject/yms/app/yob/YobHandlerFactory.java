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
import org.onosproject.yms.app.yob.exception.YobException;
import org.onosproject.yms.ydt.YdtType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.onosproject.yms.app.yob.YobConstants.E_YDT_TYPE_IS_NOT_SUPPORT;
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
            LoggerFactory.getLogger(YobHandlerFactory.class);

    /**
     * Map of YANG object builder handler.
     */
    private static final Map<YdtType, YobHandler> HANDLER_MAP = new HashMap<>();

    /**
     * Create instance of YobHandlerFactory.
     */
    private YobHandlerFactory() {
        HANDLER_MAP.put(SINGLE_INSTANCE_NODE, new YobSingleInstanceHandler());
        HANDLER_MAP.put(MULTI_INSTANCE_NODE, new YobMultiInstanceHandler());
        HANDLER_MAP.put(SINGLE_INSTANCE_LEAF_VALUE_NODE,
                        new YobSingleInstanceLeafHandler());
        HANDLER_MAP.put(MULTI_INSTANCE_LEAF_VALUE_NODE,
                        new YobMultiInstanceLeafHandler());
    }

    /**
     * Returns the corresponding YOB handler for current context.
     *
     * @param currentNode current YDT node for which object needs to be created
     * @return handler to create the object
     * @throws YobException if the YDT node type is not supported in YOB
     */
    YobHandler getYobHandlerForContext(YdtExtendedContext currentNode) {
        YobHandler yobHandler = HANDLER_MAP.get(currentNode.getYdtType());
        if (yobHandler == null) {
            log.error(E_YDT_TYPE_IS_NOT_SUPPORT);
            throw new YobException(E_YDT_TYPE_IS_NOT_SUPPORT);
        }
        return yobHandler;
    }

    /**
     * Returns the YANG object builder factory instance.
     *
     * @return YANG object builder factory instance
     */
    public static YobHandlerFactory instance() {
        return LazyHolder.INSTANCE;
    }

    /*
     * Bill Pugh Singleton pattern. INSTANCE won't be instantiated until the
     * LazyHolder class is loaded via a call to the instance() method below.
     */
    private static class LazyHolder {
        private static final YobHandlerFactory INSTANCE =
                new YobHandlerFactory();
    }
}
