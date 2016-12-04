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
import org.onosproject.yms.app.yob.exception.YobException;
import org.onosproject.yms.app.ysr.YangSchemaRegistry;
import org.onosproject.yms.ydt.YdtContext;

import static org.onosproject.yms.app.yob.YobConstants.E_MISSING_DATA_IN_NODE;
import static org.onosproject.yms.app.yob.YobHandlerFactory.instance;

/**
 * Represents implementation of YANG object builder listener.
 */
class YobListener implements YdtExtendedListener {

    /**
     * Reference to the ydt root node.
     */
    private YdtExtendedContext rootNode;

    /**
     * Reference to YANG schema registry.
     */
    private YangSchemaRegistry schemaRegistry;

    /**
     * Reference to YOB handler.
     */
    private YobHandlerFactory handlerFactory;

    /**
     * Creates an instance of YANG object builder listener.
     *
     * @param rootNode       refers to YDT context
     * @param schemaRegistry refers to YANG schema registry
     */
    YobListener(YdtExtendedContext rootNode,
                YangSchemaRegistry schemaRegistry) {
        this.rootNode = rootNode;
        this.schemaRegistry = schemaRegistry;
        this.handlerFactory = instance();
    }

    @Override
    public void enterYdtNode(YdtExtendedContext node) {

        YobHandler nodeHandler =
                handlerFactory.getYobHandlerForContext(node);

        nodeHandler.createBuilder(node, rootNode, schemaRegistry);

    }

    @Override
    public void exitYdtNode(YdtExtendedContext node) {
        YobHandler nodeHandler =
                handlerFactory.getYobHandlerForContext(node);

        nodeHandler.buildObject(node, rootNode, schemaRegistry);

        // The current ydt context node and root node are same then built
        // object needs to be returned.
        if (!node.equals(rootNode)) {
            nodeHandler.setInParent(node, schemaRegistry);
        }

    }

    /**
     * Does not support walking of non extended context YDT.
     *
     * @param ydtContext YANG data tree context
     * @throws YobException if YDT walker is not using extended context walker
     */
    @Override
    public void enterYdtNode(YdtContext ydtContext) {
        throw new YobException(E_MISSING_DATA_IN_NODE);
    }

    /**
     * Does not support walking of non extended context YDT.
     *
     * @param ydtContext YANG data tree context
     * @throws YobException if YDT walker is not using extended context walker
     */
    @Override
    public void exitYdtNode(YdtContext ydtContext) {
        throw new YobException(E_MISSING_DATA_IN_NODE);
    }
}
