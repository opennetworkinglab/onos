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
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.datamodel.YangSchemaNode;
import org.onosproject.yangutils.datamodel.YangSchemaNodeContextInfo;
import org.onosproject.yangutils.datamodel.YangSchemaNodeIdentifier;
import org.onosproject.yangutils.datamodel.exceptions.DataModelException;
import org.onosproject.yms.app.ydt.YdtExtendedContext;
import org.onosproject.yms.app.yob.exception.YobExceptions;
import org.onosproject.yms.app.ysr.YangSchemaRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.HashMap;
import java.util.Map;

import static org.onosproject.yangutils.datamodel.YangSchemaNodeType.YANG_AUGMENT_NODE;
import static org.onosproject.yangutils.datamodel.YangSchemaNodeType.YANG_CHOICE_NODE;
import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.getCapitalCase;
import static org.onosproject.yms.app.ydt.AppType.YOB;
import static org.onosproject.yms.app.yob.YobConstants.ADD_TO;
import static org.onosproject.yms.app.yob.YobConstants.BUILD;
import static org.onosproject.yms.app.yob.YobConstants.DEFAULT;
import static org.onosproject.yms.app.yob.YobConstants.FAIL_TO_BUILD;
import static org.onosproject.yms.app.yob.YobConstants.FAIL_TO_GET_FIELD;
import static org.onosproject.yms.app.yob.YobConstants.FAIL_TO_GET_METHOD;
import static org.onosproject.yms.app.yob.YobConstants.FAIL_TO_INVOKE_METHOD;
import static org.onosproject.yms.app.yob.YobConstants.HAS_NO_CHILD;
import static org.onosproject.yms.app.yob.YobConstants.OPERATION_TYPE;
import static org.onosproject.yms.app.yob.YobConstants.OP_PARAM;
import static org.onosproject.yms.app.yob.YobConstants.OP_TYPE;
import static org.onosproject.yms.app.yob.YobConstants.PERIOD;
import static org.onosproject.yms.app.yob.YobConstants.SET_OP_TYPE_FAIL;
import static org.onosproject.yms.app.yob.YobConstants.VALUE_OF;
import static org.onosproject.yms.ydt.YdtType.MULTI_INSTANCE_NODE;

/**
 * Represents the YANG object builder's work bench corresponding to a YANG data
 * tree node.
 */
class YobWorkBench {

    private static final Logger log
            = LoggerFactory.getLogger(YobWorkBench.class);

    /**
     * Class loader to be used to load the class.
     */
    private ClassLoader classLoader;

    /**
     * Map of the non schema descendant objects.
     */
    private Map<YangSchemaNodeIdentifier, YobWorkBench> attributeMap
            = new HashMap<>();

    /**
     * Reference for data-model schema node.
     */
    private YangSchemaNode yangSchemaNode;

    /**
     * builder object or the built object corresponding to the current schema
     * node.
     */
    private YobBuilderOrBuiltObject builderOrBuiltObject;

    /**
     * Setter method to be used in parent builder.
     */
    private String setterInParent;

    /**
     * Returns the builder container with the mapping schema being initialized.
     *
     * @param yangSchemaNode     mapping schema node
     * @param classLoader        class loader
     * @param qualifiedClassName qualified class name
     * @param setterInParent     setter method in parent
     */
    YobWorkBench(YangSchemaNode yangSchemaNode, ClassLoader classLoader,
                 String qualifiedClassName, String setterInParent) {
        this.yangSchemaNode = yangSchemaNode;
        this.classLoader = classLoader;
        this.setterInParent = setterInParent;
        this.builderOrBuiltObject
                = new YobBuilderOrBuiltObject(qualifiedClassName, classLoader);
    }

    /**
     * Returns the builder object or the built object corresponding to the
     * current schema node.
     *
     * @return builder or built object
     */
    YobBuilderOrBuiltObject getBuilderOrBuiltObject() {
        return builderOrBuiltObject;
    }

    /**
     * Returns the parent builder object in which the child object can be set.
     *
     * @param childNode child YDT node
     * @param registry  schema registry
     * @return parent builder object
     * @throws YobExceptions schema node does not have child
     */
    Object getParentBuilder(YdtExtendedContext childNode,
                            YangSchemaRegistry registry) {

        // Descendant schema node for whom the builder is required.
        YangSchemaNodeIdentifier targetNode = childNode
                .getYangSchemaNode().getYangSchemaNodeIdentifier();

        //Current builder container
        YobWorkBench curWorkBench = this;

        //Current Schema node context
        YangSchemaNodeContextInfo schemaContext;
        do {

            try {
                //Find the new schema context node.
                schemaContext = curWorkBench.yangSchemaNode.getChildSchema(
                        targetNode);

            } catch (DataModelException e) {
                throw new YobExceptions(yangSchemaNode.getName() +
                                                HAS_NO_CHILD +
                                                targetNode.getName());
            }

            //If the descendant schema node is in switched context
            if (schemaContext.getContextSwitchedNode() != null) {

                //check if the descendant builder container is already available
                YobWorkBench childWorkBench
                        = curWorkBench.attributeMap.get(targetNode);

                if (childWorkBench == null) {
                    YobWorkBench newWorkBench = getNewChildWorkBench(
                            schemaContext, targetNode, curWorkBench, registry);

                    //TODO: When choice and case support is added, confirm with
                    // UT, the workbench is for case and not for choice

                    curWorkBench.attributeMap.put(targetNode, newWorkBench);
                    curWorkBench = newWorkBench;
                } else {
                    curWorkBench = childWorkBench;
                }
            }

        } while (schemaContext.getContextSwitchedNode() != null);

        return curWorkBench.builderOrBuiltObject.getBuilderObject();
    }

    /**
     * Creates a new builder container object corresponding to a context
     * switch schema node.
     *
     * @param childContext schema context of immediate child
     * @param targetNode   final node whose parent builder is
     *                     required
     * @param curWorkBench current context builder container
     * @param registry     schema registry
     * @return new builder container object corresponding to a context
     * switch schema node
     */
    private YobWorkBench getNewChildWorkBench(
            YangSchemaNodeContextInfo childContext,
            YangSchemaNodeIdentifier targetNode, YobWorkBench curWorkBench,
            YangSchemaRegistry registry) {

        YangSchemaNode ctxSwitchedNode = childContext.getContextSwitchedNode();

         /*This is the first child trying to set its object in the
         current context. */
        String setterInParent = ctxSwitchedNode.getJavaAttributeName();

        /* If current switched context is choice, then case class needs to be
         used. */
        if (ctxSwitchedNode.getYangSchemaNodeType() == YANG_CHOICE_NODE) {
            try {
                childContext = ctxSwitchedNode.getChildSchema(targetNode);
            } catch (DataModelException e) {
                throw new YobExceptions(yangSchemaNode.getName() +
                                                HAS_NO_CHILD +
                                                targetNode.getName());
            }
        }

        ClassLoader newClassesLoader = getTargetClassLoader(
                curWorkBench.classLoader, childContext, registry);

        return new YobWorkBench(ctxSwitchedNode, newClassesLoader,
                                getQualifiedDefaultClassName(
                                        childContext.getSchemaNode()),
                                setterInParent);
    }

    /**
     * Returns the qualified default / op param class.
     *
     * @param schemaNode schema node of the required class
     * @return qualified default / op param class name
     */
    static String getQualifiedDefaultClassName(YangSchemaNode schemaNode) {
        String packageName = schemaNode.getJavaPackage();
        String className = getCapitalCase(
                schemaNode.getJavaClassNameOrBuiltInType());

        if (schemaNode instanceof RpcNotificationContainer) {
            return packageName + PERIOD + className + OP_PARAM;
        }

        return packageName + PERIOD + DEFAULT + className;
    }

    /**
     * Returns the class loader to be used for the switched context schema node.
     *
     * @param currentClassLoader current context class loader
     * @param switchedContext    switched context
     * @param registry           schema registry
     * @return class loader to be used for the switched context schema node
     */
    private ClassLoader getTargetClassLoader(
            ClassLoader currentClassLoader,
            YangSchemaNodeContextInfo switchedContext,
            YangSchemaRegistry registry) {
        YangSchemaNode augmentSchemaNode = switchedContext.getSchemaNode();
        if (augmentSchemaNode.getYangSchemaNodeType() ==
                YANG_AUGMENT_NODE) {
            YangSchemaNode parentSchemaNode =
                    ((YangNode) augmentSchemaNode).getParent();

            Class<?> regClass = registry.getRegisteredClass(
                    parentSchemaNode, getCapitalCase(
                            parentSchemaNode.getJavaClassNameOrBuiltInType()));
            return regClass.getClassLoader();
        }

        return currentClassLoader;
    }

    /**
     * Set the operation type attribute and build the object from the builder
     * object, by invoking the build method.
     *
     * @param ydtNode     data tree node
     * @param ydtRootNode root node
     */
    void buildObject(YdtExtendedContext ydtNode,
                     YdtExtendedContext ydtRootNode) {
        Object builderObject = builderOrBuiltObject.getBuilderObject();
        Class<?> defaultBuilderClass = builderOrBuiltObject.yangBuilderClass;
        Class<?> interfaceClass = builderOrBuiltObject.yangDefaultClass;
        Object operationType;

        // Setting the value into YANG node operation type from ydtContext
        // operation type.
        try {
            Class<?>[] innerClasses = interfaceClass.getClasses();
            for (Class<?> innerEnumClass : innerClasses) {
                if (innerEnumClass.getSimpleName().equals(OP_TYPE)) {
                    Method valueOfMethod = innerEnumClass
                            .getDeclaredMethod(VALUE_OF, String.class);
                    if (ydtNode.getYdtContextOperationType() != null) {
                        operationType = valueOfMethod.invoke(null, ydtNode
                                .getYdtContextOperationType().toString());
                        Field operationTypeField = defaultBuilderClass
                                .getDeclaredField(OPERATION_TYPE);
                        operationTypeField.setAccessible(true);
                        operationTypeField.set(builderObject, operationType);
                        break;
                    }
                }
            }
        } catch (NoSuchFieldException | NoSuchMethodException |
                InvocationTargetException | IllegalAccessException e) {
            log.error(SET_OP_TYPE_FAIL);
            throw new YobExceptions(SET_OP_TYPE_FAIL);
        }


        // Invoking the build method to get built object from build method.
        try {
            Method method = defaultBuilderClass.getDeclaredMethod(BUILD);
            if (method == null) {
                log.error(FAIL_TO_GET_METHOD + defaultBuilderClass.getName());
                throw new YobExceptions(FAIL_TO_GET_METHOD +
                                                defaultBuilderClass.getName());
            }
            Object builtObject = method.invoke(builderObject);
            // The built object will be maintained in ydt context and same will
            // be used while setting into parent method.
            builderOrBuiltObject.setBuiltObject(builtObject);

        } catch (NoSuchMethodException | InvocationTargetException |
                IllegalAccessException e) {
            log.error(FAIL_TO_BUILD + defaultBuilderClass.getName());
            throw new YobExceptions(FAIL_TO_BUILD +
                                            defaultBuilderClass.getName());
        }

        // The current ydt context node and root node are same then return.
        if (!ydtNode.equals(ydtRootNode)) {
            invokeSetObjectInParent(ydtNode);
        }
    }

    /**
     * Sets the YANG built object in corresponding parent class method.
     *
     * @param ydtNode ydtExtendedContext is used to get application
     *                related information maintained in YDT
     */
    private void invokeSetObjectInParent(YdtExtendedContext ydtNode) {
        Class<?> classType = null;
        Method method;

        Object objectToSetInParent = builderOrBuiltObject.getBuiltObject();

        YdtExtendedContext parentNode = (YdtExtendedContext) ydtNode
                .getParent();
        if (parentNode != null) {
            YobWorkBench parentYobWorkBench = (YobWorkBench)
                    parentNode.getAppInfo(YOB);
            Object parentBuilderObject = parentYobWorkBench
                    .builderOrBuiltObject.getBuilderObject();

            Class<?> parentBuilderClass = parentBuilderObject.getClass();
            String parentBuilderClassName = parentBuilderClass.getName();

            try {
                Field fieldName = parentBuilderClass
                        .getDeclaredField(setterInParent);
                if (fieldName != null) {
                    classType = fieldName.getType();
                }

                if (ydtNode.getYdtType() == MULTI_INSTANCE_NODE) {
                    if (fieldName != null) {
                        ParameterizedType genericListType =
                                (ParameterizedType) fieldName.getGenericType();
                        classType = (Class<?>) genericListType
                                .getActualTypeArguments()[0];
                    }
                    method = parentBuilderClass.getDeclaredMethod(
                            ADD_TO + getCapitalCase(setterInParent), classType);
                } else {
                    method = parentBuilderClass.getDeclaredMethod(
                            setterInParent, classType);
                }

                if (method != null) {
                    method.invoke(parentBuilderObject, objectToSetInParent);
                }
            } catch (NoSuchFieldException e) {
                log.error(FAIL_TO_GET_FIELD + parentBuilderClassName);
            } catch (NoSuchMethodException e) {
                log.error(FAIL_TO_GET_METHOD + parentBuilderClassName);
            } catch (InvocationTargetException | IllegalAccessException e) {
                log.error(FAIL_TO_INVOKE_METHOD + parentBuilderClassName);
            }
        }
        ydtNode.addAppInfo(YOB, this);
    }
}
