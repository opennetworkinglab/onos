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

import org.onosproject.yangutils.datamodel.YangSchemaNode;
import org.onosproject.yms.app.ydt.YdtExtendedContext;
import org.onosproject.yms.app.ysr.YangSchemaRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.onosproject.yms.app.ydt.AppType.YOB;
import static org.onosproject.yms.app.yob.YobUtils.getQualifiedDefaultClass;

/**
 * Represents a YANG object builder handler to process the ydt content and
 * build yang object.
 */
abstract class YobHandler {

    private static final Logger log = LoggerFactory.getLogger(YobHandler.class);

    /**
     * Creates a YANG builder object.
     *
     * @param curNode  ydtExtendedContext is used to get
     *                 application related information maintained
     *                 in YDT
     * @param rootNode ydtRootNode is refers to module node
     * @param registry registry
     */
    public void createBuilder(YdtExtendedContext curNode,
                              YdtExtendedContext rootNode,
                              YangSchemaRegistry registry) {
        String setterName = null;
        YangSchemaNode node = curNode.getYangSchemaNode();
        while (node.getReferredSchema() != null) {
            node = node.getReferredSchema();
        }

        String qualName = getQualifiedDefaultClass(node);
        ClassLoader classLoader = YobUtils.getClassLoader(registry, qualName,
                                                          curNode, rootNode);

        if (curNode != rootNode) {
            setterName = node.getJavaAttributeName();
        }

        Object workBench = new YobWorkBench(curNode.getYangSchemaNode(), classLoader, qualName,
                                            setterName);

        curNode.addAppInfo(YOB, workBench);
    }

    /**
     * Sets the YANG built object in corresponding parent class method.
     *
     * @param ydtNode        ydtExtendedContext is used to get application
     *                       related information maintained in YDT
     * @param schemaRegistry YANG schema registry
     */
    public void setInParent(YdtExtendedContext ydtNode,
                            YangSchemaRegistry schemaRegistry) {
        YdtExtendedContext parentNode = (YdtExtendedContext) ydtNode.getParent();
        YobWorkBench parentWorkbench = (YobWorkBench) parentNode.getAppInfo(YOB);
        parentWorkbench.setObject(ydtNode, schemaRegistry);
    }

    /**
     * Builds the object.
     *
     * @param ydtNode        ydtExtendedContext is used to get
     *                       application related
     *                       information maintained in YDT
     * @param ydtRootNode    ydtRootNode
     * @param schemaRegistry YANG schema registry
     */
    public void buildObject(YdtExtendedContext ydtNode,
                            YdtExtendedContext ydtRootNode,
                            YangSchemaRegistry schemaRegistry) {
        YobWorkBench yobWorkBench = (YobWorkBench) ydtNode.getAppInfo(YOB);
        yobWorkBench.buildObject(ydtNode.getYdtContextOperationType(), schemaRegistry);
    }
}
