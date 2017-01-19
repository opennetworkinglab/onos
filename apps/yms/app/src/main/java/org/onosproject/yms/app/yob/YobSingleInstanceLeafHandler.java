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

import org.onosproject.yangutils.datamodel.YangLeaf;
import org.onosproject.yangutils.datamodel.YangSchemaNode;
import org.onosproject.yangutils.datamodel.YangType;
import org.onosproject.yangutils.datamodel.utils.builtindatatype.YangDataTypes;
import org.onosproject.yms.app.ydt.YdtExtendedContext;
import org.onosproject.yms.app.yob.exception.YobException;
import org.onosproject.yms.app.ysr.YangSchemaRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.onosproject.yms.app.ydt.AppType.YOB;
import static org.onosproject.yms.app.yob.YobConstants.E_FAIL_TO_INVOKE_METHOD;
import static org.onosproject.yms.app.yob.YobConstants.L_FAIL_TO_INVOKE_METHOD;

/**
 * Represents a single instance leaf node handler in YANG object builder.
 */
class YobSingleInstanceLeafHandler extends YobHandler {

    private static final Logger log =
            LoggerFactory.getLogger(YobSingleInstanceLeafHandler.class);

    @Override
    public void createBuilder(YdtExtendedContext curNode,
                              YdtExtendedContext rootNode,
                              YangSchemaRegistry registry) {
        // For single instance leaf no need to create an object.
    }

    @Override
    public void buildObject(YdtExtendedContext ydtNode,
                            YdtExtendedContext ydtRootNode,
                            YangSchemaRegistry schemaRegistry) {
        // For single instance leaf no need to build an object.
    }

    /**
     * Set the leaf's value in the YANG object.
     *
     * @param leafNode       leaf YDT node
     * @param schemaRegistry YANG schema registry
     * @throws YobException if failed to invoke the leaf's setter
     */
    @Override
    public void setInParent(YdtExtendedContext leafNode,
                            YangSchemaRegistry schemaRegistry) {
        Class<?> builderClass = null;

        try {
            YangSchemaNode schemaNode = leafNode.getYangSchemaNode();
            while (schemaNode.getReferredSchema() != null) {
                schemaNode = schemaNode.getReferredSchema();
            }

            String setterInParent = schemaNode.getJavaAttributeName();
            YdtExtendedContext parentNode =
                    (YdtExtendedContext) leafNode.getParent();
            YobWorkBench workBench = (YobWorkBench) parentNode.getAppInfo(YOB);
            Object builderObject = workBench
                    .getParentBuilder(leafNode, schemaRegistry);
            builderClass = builderObject.getClass();
            if (leafNode.getValue() != null || ((YangLeaf) schemaNode)
                    .getDataType().getDataType() == YangDataTypes.EMPTY) {
                Field leafName = builderClass.getDeclaredField(setterInParent);
                Method setterMethod = builderClass
                        .getDeclaredMethod(setterInParent, leafName.getType());
                YangType<?> yangType = ((YangLeaf) schemaNode).getDataType();
                YobUtils.setDataFromStringValue(yangType.getDataType(), leafNode
                                                        .getValue(),
                                                setterMethod, builderObject,
                                                leafNode);
            } else {
                YobUtils.setSelectLeaf(builderClass, leafNode,
                                       schemaRegistry, builderObject);
            }
        } catch (NoSuchMethodException | InvocationTargetException |
                IllegalAccessException | NoSuchFieldException e) {
            log.error(L_FAIL_TO_INVOKE_METHOD, builderClass.getName());
            throw new YobException(E_FAIL_TO_INVOKE_METHOD +
                                           builderClass.getName());
        }
    }
}
