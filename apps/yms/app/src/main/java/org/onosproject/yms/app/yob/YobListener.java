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
import org.onosproject.yms.app.ydt.YdtExtendedListener;
import org.onosproject.yms.app.yob.exception.YobExceptions;
import org.onosproject.yms.app.ysr.YangSchemaRegistry;
import org.onosproject.yms.ydt.YdtContext;

import static org.onosproject.yms.app.yob.YobConstants.NO_HANDLE_FOR_YDT;

/**
 * Represents implementation of YANG object builder listener.
 */
class YobListener implements YdtExtendedListener {

    /**
     * Reference to the ydt root node.
     */
    private YdtExtendedContext ydtRootNode;

    /**
     * Reference to YANG schema registry.
     */
    private YangSchemaRegistry schemaRegistry;

    /**
     * Reference to YOB handler.
     */
    private YobHandlerFactory yobHandlerFactory;

    /**
     * Creates an instance of YANG object builder listener.
     *
     * @param ydtRootExtendedContext refers to YDT context
     * @param schemaRegistry         refers to YANG schema registry
     */
    YobListener(YdtExtendedContext ydtRootExtendedContext,
                YangSchemaRegistry schemaRegistry) {
        this.ydtRootNode = ydtRootExtendedContext;
        this.schemaRegistry = schemaRegistry;
        this.yobHandlerFactory = new YobHandlerFactory();
    }

    @Override
    public void enterYdtNode(YdtExtendedContext ydtExtendedContext) {

        YobHandler nodeHandler =
                yobHandlerFactory.getYobHandlerForContext(ydtExtendedContext);

        if (nodeHandler == null) {
            throw new YobExceptions(NO_HANDLE_FOR_YDT);
        }
        nodeHandler.createYangBuilderObject(ydtExtendedContext,
                                            ydtRootNode, schemaRegistry);

    }

    @Override
    public void exitYdtNode(YdtExtendedContext ydtExtendedContext) {
        YobHandler nodeHandler =
                yobHandlerFactory.getYobHandlerForContext(ydtExtendedContext);
        if (nodeHandler != null) {
            nodeHandler.buildObjectFromBuilder(ydtExtendedContext,
                                               ydtRootNode, schemaRegistry);
            // The current ydt context node and root node are same then return.
            if (!ydtExtendedContext.equals(ydtRootNode)) {
                nodeHandler.setObjectInParent(ydtExtendedContext,
                                              schemaRegistry);
            }
        }
    }

    @Override
    public void enterYdtNode(YdtContext ydtContext) {
    }

    @Override
    public void exitYdtNode(YdtContext ydtContext) {
    }
}
