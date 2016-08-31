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
import org.onosproject.yangutils.datamodel.javadatamodel
        .JavaQualifiedTypeInfoContainer;
import org.onosproject.yms.app.ydt.YdtExtendedContext;
import org.onosproject.yms.app.ysr.YangSchemaRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.onosproject.yms.app.ydt.AppType.YOB;
import static org.onosproject.yms.app.yob.YobConstants.FAIL_TO_INVOKE_METHOD;

/**
 * Represents a single instance leaf node handler in YANG object builder.
 */
class YobSingleInstanceLeafHandler extends YobHandler {

    private static final Logger log =
            LoggerFactory.getLogger(YobSingleInstanceLeafHandler.class);

    @Override
    public void createYangBuilderObject(YdtExtendedContext curYdtNode,
                                        YdtExtendedContext rootYdtNode,
                                        YangSchemaRegistry registry) {
        // For single instance leaf no need to create an object.
    }

    @Override
    public void buildObjectFromBuilder(YdtExtendedContext ydtNode,
                                       YdtExtendedContext ydtRootNode,
                                       YangSchemaRegistry schemaRegistry) {
        // For single instance leaf no need to build an object.
    }

    @Override
    public void setObjectInParent(YdtExtendedContext leafNode,
                                  YangSchemaRegistry schemaRegistry) {
        Class<?> parentBldrClass = null;
        YangSchemaNode yangSchemaNode = leafNode.getYangSchemaNode();
        YdtExtendedContext parentYdtNode =
                (YdtExtendedContext) leafNode.getParent();
        YobWorkBench parentYobWorkBench =
                (YobWorkBench) parentYdtNode.getAppInfo(YOB);
        String value = leafNode.getValue();

        try {
            String setterInParent = yangSchemaNode.getJavaAttributeName();
            Object parentBuilderObject = parentYobWorkBench
                    .getParentBuilder(leafNode, schemaRegistry);
            parentBldrClass = parentBuilderObject.getClass();
            Field leafName = parentBldrClass.getDeclaredField(setterInParent);
            Method parentSetterMethod = parentBldrClass
                    .getDeclaredMethod(setterInParent, leafName.getType());
            JavaQualifiedTypeInfoContainer javaQualifiedType =
                    (JavaQualifiedTypeInfoContainer) yangSchemaNode;
            YangType<?> yangType = ((YangLeaf) javaQualifiedType).getDataType();
            setDataFromStringValue(yangType, value, parentSetterMethod,
                                   parentBuilderObject, leafNode);
        } catch (NoSuchMethodException | InvocationTargetException |
                IllegalAccessException | NoSuchFieldException e) {
            log.error(FAIL_TO_INVOKE_METHOD + parentBldrClass.getName());
        }
    }
}
