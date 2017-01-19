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


import org.onosproject.yangutils.datamodel.YangLeafList;
import org.onosproject.yangutils.datamodel.YangSchemaNode;
import org.onosproject.yangutils.datamodel.YangType;
import org.onosproject.yangutils.datamodel.javadatamodel.JavaQualifiedTypeInfoContainer;
import org.onosproject.yms.app.ydt.YdtExtendedContext;
import org.onosproject.yms.app.yob.exception.YobException;
import org.onosproject.yms.app.ysr.YangSchemaRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.Set;

import static org.onosproject.yangutils.datamodel.utils.builtindatatype.YangDataTypes.IDENTITYREF;
import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.getCapitalCase;
import static org.onosproject.yms.app.ydt.AppType.YOB;
import static org.onosproject.yms.app.yob.YobConstants.ADD_TO;
import static org.onosproject.yms.app.yob.YobConstants.E_FAIL_TO_INVOKE_METHOD;
import static org.onosproject.yms.app.yob.YobConstants.L_FAIL_TO_INVOKE_METHOD;

/**
 * Represents a multi instance leaf node handler in YANG object builder.
 */
class YobMultiInstanceLeafHandler
        extends YobHandler {

    private static final Logger log =
            LoggerFactory.getLogger(YobMultiInstanceLeafHandler.class);

    @Override
    public void createBuilder(YdtExtendedContext curNode,
                              YdtExtendedContext rootNode,
                              YangSchemaRegistry registry) {
        // For multi instance leaf no need to create an object.
    }

    @Override
    public void buildObject(YdtExtendedContext ydtNode,
                            YdtExtendedContext ydtRootNode,
                            YangSchemaRegistry schemaRegistry) {
        // For multi instance leaf no need to build object.
    }

    /**
     * Set the leaf list values in the YANG object.
     *
     * @param leafListNode   leaf list YDT node
     * @param schemaRegistry YANG schema registry
     * @throws YobException if failed to invoke the leaf list's setter
     */
    @Override
    public void setInParent(YdtExtendedContext leafListNode,
                            YangSchemaRegistry schemaRegistry) {
        Class<?> parentBuilderClass = null;
        YangSchemaNode yangSchemaNode = leafListNode.getYangSchemaNode();
        while (yangSchemaNode.getReferredSchema() != null) {
            yangSchemaNode = yangSchemaNode.getReferredSchema();
        }

        YdtExtendedContext parentYdtNode =
                (YdtExtendedContext) leafListNode.getParent();
        YobWorkBench parentYobWorkBench =
                (YobWorkBench) parentYdtNode.getAppInfo(YOB);
        Set<String> valueSet = leafListNode.getValueSet();

        for (String value : valueSet) {
            try {
                String setterInParent = yangSchemaNode.getJavaAttributeName();
                Object builderObject = parentYobWorkBench
                        .getParentBuilder(leafListNode, schemaRegistry);
                parentBuilderClass = builderObject.getClass();
                Field leafName = parentBuilderClass
                        .getDeclaredField(setterInParent);
                ParameterizedType genericListType =
                        (ParameterizedType) leafName.getGenericType();
                Class<?> genericListClass;
                if (((YangLeafList) leafListNode.getYangSchemaNode())
                        .getDataType().getDataType() == IDENTITYREF) {
                    ParameterizedType type = (ParameterizedType)
                            genericListType.getActualTypeArguments()[0];
                    genericListClass = type.getClass().getClass();
                } else {
                    genericListClass = (Class<?>) genericListType.getActualTypeArguments()[0];
                }

                Method setterMethod = parentBuilderClass.getDeclaredMethod(
                        ADD_TO + getCapitalCase(setterInParent), genericListClass);

                JavaQualifiedTypeInfoContainer javaQualifiedType =
                        (JavaQualifiedTypeInfoContainer) yangSchemaNode;
                YangType<?> yangType =
                        ((YangLeafList) javaQualifiedType).getDataType();
                YobUtils.setDataFromStringValue(yangType.getDataType(), value,
                                                setterMethod,
                                                builderObject, leafListNode);
            } catch (NoSuchMethodException | InvocationTargetException
                    | IllegalAccessException | NoSuchFieldException e) {
                log.error(L_FAIL_TO_INVOKE_METHOD,
                          parentBuilderClass.getName());
                throw new YobException(E_FAIL_TO_INVOKE_METHOD +
                                               parentBuilderClass.getName());
            }
        }
    }
}
