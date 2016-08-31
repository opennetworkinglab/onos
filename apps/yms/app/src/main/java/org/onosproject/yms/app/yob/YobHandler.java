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

import org.onosproject.yangutils.datamodel.RpcNotificationContainer;
import org.onosproject.yangutils.datamodel.YangBinary;
import org.onosproject.yangutils.datamodel.YangSchemaNode;
import org.onosproject.yangutils.datamodel.YangType;
import org.onosproject.yms.app.ydt.YdtExtendedContext;
import org.onosproject.yms.app.ysr.YangSchemaRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;

import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.getCapitalCase;
import static org.onosproject.yms.app.ydt.AppType.YOB;
import static org.onosproject.yms.app.yob.YobConstants.DATA_TYPE_NOT_SUPPORT;
import static org.onosproject.yms.app.yob.YobConstants.FAIL_TO_LOAD_CLASS;
import static org.onosproject.yms.app.yob.YobConstants.FAIL_TO_LOAD_CONSTRUCTOR;
import static org.onosproject.yms.app.yob.YobConstants.FROM_STRING;
import static org.onosproject.yms.app.yob.YobConstants.OF;
import static org.onosproject.yms.app.yob.YobConstants.PERIOD;
import static org.onosproject.yms.app.yob.YobWorkBench.getQualifiedDefaultClassName;

/**
 * Represents a YANG object builder handler to process the ydt content and
 * build yang object.
 */
abstract class YobHandler {

    private static final Logger log = LoggerFactory.getLogger(YobHandler.class);

    /**
     * reference to YANG schema registry.
     */
    private YangSchemaRegistry registry;

    /**
     * Creates a YANG builder object.
     *
     * @param curYdtNode  ydtExtendedContext is used to get
     *                    application related information maintained
     *                    in YDT
     * @param rootYdtNode ydtRootNode is refers to module node
     * @param registry    registry
     */
    public void createYangBuilderObject(YdtExtendedContext curYdtNode,
                                        YdtExtendedContext rootYdtNode,
                                        YangSchemaRegistry registry) {
        String setterName = null;
        YangSchemaNode node = curYdtNode.getYangSchemaNode();

        String qualName = getQualifiedDefaultClassName(node);
        ClassLoader classLoader = getClassLoader(registry, qualName,
                                                 curYdtNode);

        if (curYdtNode != rootYdtNode) {
            setterName = node.getJavaAttributeName();
        }

        Object builderObject = new YobWorkBench(node, classLoader,
                                                qualName, setterName);

        curYdtNode.addAppInfo(YOB, builderObject);
    }

    /**
     * Sets the YANG built object in corresponding parent class method.
     *
     * @param ydtNode        ydtExtendedContext is used to get application
     *                       related information maintained in YDT
     * @param schemaRegistry YANG schema registry
     */
    public void setObjectInParent(YdtExtendedContext ydtNode,
                                  YangSchemaRegistry schemaRegistry) {
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
    public void buildObjectFromBuilder(YdtExtendedContext ydtNode,
                                       YdtExtendedContext ydtRootNode,
                                       YangSchemaRegistry schemaRegistry) {
        YobWorkBench yobWorkBench = (YobWorkBench) ydtNode.getAppInfo(YOB);
        yobWorkBench.buildObject(ydtNode, ydtRootNode);
    }

    /**
     * This method is used to set data from string value in parent method.
     *
     * @param type                refers to YANG type
     * @param leafValue           leafValue argument is used to set the value
     *                            in method
     * @param parentSetterMethod  Invokes the underlying method represented
     *                            by this parentSetterMethod
     * @param parentBuilderObject the parentBuilderObject is to invoke the
     *                            underlying method
     * @param ydtExtendedContext  ydtExtendedContext is used to get
     *                            application related
     *                            information maintained in YDT
     * @throws InvocationTargetException throws InvocationTargetException
     * @throws IllegalAccessException    throws IllegalAccessException
     * @throws NoSuchMethodException     throws NoSuchMethodException
     */
    void setDataFromStringValue(YangType<?> type, String leafValue,
                                Method parentSetterMethod,
                                Object parentBuilderObject,
                                YdtExtendedContext ydtExtendedContext)
            throws InvocationTargetException, IllegalAccessException,
            NoSuchMethodException {
        switch (type.getDataType()) {
            case INT8: {
                parentSetterMethod.invoke(parentBuilderObject,
                                          Byte.parseByte(leafValue));
                break;
            }
            case UINT8:
            case INT16: {
                parentSetterMethod.invoke(parentBuilderObject,
                                          Short.parseShort(leafValue));
                break;
            }
            case UINT16:
            case INT32: {
                parentSetterMethod.invoke(parentBuilderObject,
                                          Integer.parseInt(leafValue));
                break;
            }
            case UINT32:
            case INT64: {
                parentSetterMethod.invoke(parentBuilderObject,
                                          Long.parseLong(leafValue));
                break;
            }
            case UINT64: {
                parentSetterMethod.invoke(parentBuilderObject,
                                          new BigInteger(leafValue));
                break;
            }
            case EMPTY:
            case BOOLEAN: {
                parentSetterMethod.invoke(parentBuilderObject,
                                          Boolean.parseBoolean(leafValue));
                break;
            }
            case STRING: {
                parentSetterMethod.invoke(parentBuilderObject, leafValue);
                break;
            }
            case BINARY: {
                parentSetterMethod.invoke(parentBuilderObject,
                                          new YangBinary(leafValue));
                break;
            }
            case BITS: {
                //TODO
                break;
            }
            case DECIMAL64: {
                parentSetterMethod.invoke(parentBuilderObject,
                                          new BigDecimal(leafValue));
                break;
            }
            case DERIVED: {
                parseDerivedTypeInfo(ydtExtendedContext, parentSetterMethod,
                                     parentBuilderObject, leafValue, false);
                break;
            }
            case UNION: {
                // TODO
                break;
            }
            case LEAFREF: {
                // TODO
                break;
            }
            case ENUMERATION: {
                parseDerivedTypeInfo(ydtExtendedContext, parentSetterMethod,
                                     parentBuilderObject, leafValue, true);
                break;
            }
            default: {
                log.error(DATA_TYPE_NOT_SUPPORT);
            }
        }
    }

    /**
     * To set data into parent setter method from string value for derived type.
     *
     * @param leafValue           leafValue argument is used to set the value
     *                            in method
     * @param parentSetterMethod  Invokes the underlying method represented
     *                            by this parentSetterMethod
     * @param parentBuilderObject the parentBuilderObject is to invoke the
     *                            underlying method
     * @param ydtExtendedContext  ydtExtendedContext is used to get
     *                            application related
     *                            information maintained in YDT
     * @param isEnum              isEnum parameter is used to check whether
     *                            type is enum or derived
     *                            information maintained in YDT
     * @throws InvocationTargetException throws InvocationTargetException
     * @throws IllegalAccessException    throws IllegalAccessException
     * @throws NoSuchMethodException     throws NoSuchMethodException
     */
    private void parseDerivedTypeInfo(YdtExtendedContext ydtExtendedContext,
                                      Method parentSetterMethod,
                                      Object parentBuilderObject,
                                      String leafValue, boolean isEnum)
            throws InvocationTargetException, IllegalAccessException,
            NoSuchMethodException {
        Class<?> childSetClass = null;
        Constructor<?> childConstructor = null;
        Object childValue = null;
        Object childObject = null;
        Method childMethod = null;

        YangSchemaNode yangJavaModule = ydtExtendedContext.getYangSchemaNode();
        String packageName = yangJavaModule.getJavaPackage();
        String className = getCapitalCase(
                yangJavaModule.getJavaClassNameOrBuiltInType());
        String qualifiedClassName = packageName + PERIOD + className;
        ClassLoader classLoader = getClassLoader(registry,
                                                 qualifiedClassName,
                                                 ydtExtendedContext);
        try {
            childSetClass = classLoader.loadClass(qualifiedClassName);
        } catch (ClassNotFoundException e) {
            log.error(FAIL_TO_LOAD_CLASS + packageName + PERIOD + className);
        }
        if (!isEnum) {

            if (childSetClass != null) {
                childConstructor = childSetClass.getDeclaredConstructor();
            }

            if (childConstructor != null) {
                childConstructor.setAccessible(true);
            }
            try {
                if (childConstructor != null) {
                    childObject = childConstructor.newInstance();
                }
            } catch (InstantiationException e) {
                log.error(FAIL_TO_LOAD_CONSTRUCTOR + className);
            }
            if (childSetClass != null) {
                childMethod = childSetClass
                        .getDeclaredMethod(FROM_STRING, String.class);
            }
        } else {
            if (childSetClass != null) {
                childMethod = childSetClass.getDeclaredMethod(OF, String.class);
            }
            //leafValue = JavaIdentifierSyntax.getEnumJavaAttribute(leafValue);
            //leafValue = leafValue.toUpperCase();
        }
        if (childMethod != null) {
            childValue = childMethod.invoke(childObject, leafValue);
        }

        parentSetterMethod.invoke(parentBuilderObject, childValue);
    }

    /**
     * Updates class loader for all the classes.
     *
     * @param registry           YANG schema registry
     * @param context            YDT context
     * @param qualifiedClassName qualified class name
     * @return current class loader
     */
    private ClassLoader getClassLoader(YangSchemaRegistry registry,
                                       String qualifiedClassName,
                                       YdtExtendedContext context) {

        YangSchemaNode yangSchemaNode = context.getYangSchemaNode();
        if (yangSchemaNode instanceof RpcNotificationContainer) {
            Class<?> regClass = registry.getRegisteredClass(yangSchemaNode,
                                                            qualifiedClassName);
            return regClass.getClassLoader();
        } else {

            YdtExtendedContext parent =
                    (YdtExtendedContext) context.getParent();
            YobWorkBench parentBuilderContainer =
                    (YobWorkBench) parent.getAppInfo(YOB);
            Object parentObj =
                    parentBuilderContainer.getParentBuilder(context, registry);
            return parentObj.getClass().getClassLoader();
        }
    }

    /**
     * Returns the YANG schema registry.
     *
     * @return registry YANG schema registry
     */
    public YangSchemaRegistry getRegistry() {
        return registry;
    }

    /**
     * Sets the YANG schema registry.
     *
     * @param registry YANG schema registry
     */
    public void setRegistry(YangSchemaRegistry registry) {
        this.registry = registry;
    }
}
