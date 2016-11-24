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
import org.onosproject.yangutils.datamodel.YangSchemaNodeContextInfo;
import org.onosproject.yangutils.datamodel.YangSchemaNodeIdentifier;
import org.onosproject.yangutils.datamodel.exceptions.DataModelException;
import org.onosproject.yms.app.ydt.YdtExtendedContext;
import org.onosproject.yms.app.yob.exception.YobException;
import org.onosproject.yms.app.ysr.YangSchemaRegistry;
import org.onosproject.yms.ydt.YdtContextOperationType;
import org.onosproject.yms.ydt.YdtType;
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
import static org.onosproject.yms.app.ydt.AppType.YOB;
import static org.onosproject.yms.app.yob.YobConstants.ADD_AUGMENT_METHOD;
import static org.onosproject.yms.app.yob.YobConstants.ADD_TO;
import static org.onosproject.yms.app.yob.YobConstants.BUILD;
import static org.onosproject.yms.app.yob.YobConstants.E_FAIL_TO_BUILD;
import static org.onosproject.yms.app.yob.YobConstants.E_FAIL_TO_GET_FIELD;
import static org.onosproject.yms.app.yob.YobConstants.E_FAIL_TO_GET_METHOD;
import static org.onosproject.yms.app.yob.YobConstants.E_FAIL_TO_INVOKE_METHOD;
import static org.onosproject.yms.app.yob.YobConstants.E_FAIL_TO_LOAD_CLASS;
import static org.onosproject.yms.app.yob.YobConstants.E_HAS_NO_CHILD;
import static org.onosproject.yms.app.yob.YobConstants.E_SET_OP_TYPE_FAIL;
import static org.onosproject.yms.app.yob.YobConstants.L_FAIL_TO_BUILD;
import static org.onosproject.yms.app.yob.YobConstants.L_FAIL_TO_GET_FIELD;
import static org.onosproject.yms.app.yob.YobConstants.L_FAIL_TO_GET_METHOD;
import static org.onosproject.yms.app.yob.YobConstants.L_FAIL_TO_INVOKE_METHOD;
import static org.onosproject.yms.app.yob.YobConstants.ONOS_YANG_OP_TYPE;
import static org.onosproject.yms.app.yob.YobConstants.OP_TYPE;
import static org.onosproject.yms.app.yob.YobConstants.VALUE_OF;
import static org.onosproject.yms.app.yob.YobConstants.YANG;
import static org.onosproject.yms.app.yob.YobUtils.getCapitalCase;
import static org.onosproject.yms.app.yob.YobUtils.getModuleInterface;
import static org.onosproject.yms.app.yob.YobUtils.getQualifiedDefaultClass;
import static org.onosproject.yms.ydt.YdtType.MULTI_INSTANCE_NODE;
import static org.onosproject.yms.ydt.YdtType.SINGLE_INSTANCE_NODE;

/**
 * Represents the YANG object builder's work bench corresponding to a YANG data
 * tree node.
 */
class YobWorkBench {

    private static final Logger log =
            LoggerFactory.getLogger(YobWorkBench.class);

    /**
     * Class loader to be used to load the class.
     */
    private ClassLoader classLoader;

    /**
     * Map of the non schema descendant objects.
     */
    private Map<YangSchemaNodeIdentifier, YobWorkBench> attributeMap =
            new HashMap<>();

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
        this.builderOrBuiltObject =
                new YobBuilderOrBuiltObject(qualifiedClassName, classLoader);
    }

    /**
     * Set the attribute in a builder object.
     *
     * @param builder   builder object in which the attribute needs to be set
     * @param setter    setter method in parent
     * @param nodeType  type of node to set
     * @param attribute attribute to set in the builder
     */
    private static void setObjectInBuilder(Object builder, String setter,
                                           YdtType nodeType, Object attribute) {
        Class<?> builderClass = builder.getClass();
        String builderClassName = builderClass.getName();
        try {
            Class<?> type = null;
            Field fieldName = builderClass.getDeclaredField(setter);
            if (fieldName != null) {
                type = fieldName.getType();
            }

            Method method;
            if (nodeType == MULTI_INSTANCE_NODE) {
                if (fieldName != null) {
                    ParameterizedType genericTypes =
                            (ParameterizedType) fieldName.getGenericType();
                    type = (Class<?>) genericTypes.getActualTypeArguments()[0];
                }
                method = builderClass.getDeclaredMethod(
                        ADD_TO + getCapitalCase(setter), type);
            } else {
                method = builderClass.getDeclaredMethod(setter, type);
            }

            method.invoke(builder, attribute);
        } catch (NoSuchFieldException e) {
            log.error(L_FAIL_TO_GET_FIELD, builderClassName);
            throw new YobException(E_FAIL_TO_GET_FIELD + builderClassName);
        } catch (NoSuchMethodException e) {
            log.error(L_FAIL_TO_GET_METHOD, builderClassName);
            throw new YobException(E_FAIL_TO_GET_METHOD + builderClassName);
        } catch (InvocationTargetException | IllegalAccessException e) {
            log.error(L_FAIL_TO_INVOKE_METHOD, builderClassName);
            throw new YobException(E_FAIL_TO_INVOKE_METHOD + builderClassName);
        }
    }

    private static void addInAugmentation(Object builder, String className,
                                          Object instance) {
        Class<?>[] interfaces = instance.getClass().getInterfaces();
        if (interfaces == null) {
            throw new YobException(E_FAIL_TO_LOAD_CLASS + className);
        }

        int i;
        for (i = 0; i < interfaces.length; i++) {
            if (interfaces[i].getName().equals(className)) {
                break;
            }
        }
        if (i == interfaces.length) {
            throw new YobException(E_FAIL_TO_LOAD_CLASS + className);
        }

        Class<?> builderClass = builder.getClass();
        String builderClassName = builderClass.getName();
        try {

            Method method = builderClass.getDeclaredMethod(ADD_AUGMENT_METHOD,
                                                           Object.class,
                                                           Class.class);
            method.invoke(builder, instance, interfaces[i]);
        } catch (NoSuchMethodException e) {
            log.error(L_FAIL_TO_GET_METHOD, builderClassName);
            throw new YobException(E_FAIL_TO_GET_METHOD + builderClassName);
        } catch (InvocationTargetException | IllegalAccessException e) {
            log.error(L_FAIL_TO_INVOKE_METHOD, builderClassName);
            throw new YobException(E_FAIL_TO_INVOKE_METHOD + builderClassName);
        }

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
    private static YobWorkBench getNewChildWorkBench(
            YangSchemaNodeContextInfo childContext,
            YangSchemaNodeIdentifier targetNode, YobWorkBench curWorkBench,
            YangSchemaRegistry registry) {

        YangSchemaNode ctxSwitchedNode = childContext.getContextSwitchedNode();
        String name;

         /* This is the first child trying to set its object in the
         current context. */
        String setterInParent = ctxSwitchedNode.getJavaAttributeName();

        /* If current switched context is choice, then case class needs to be
         used. */
        if (ctxSwitchedNode.getYangSchemaNodeType() == YANG_CHOICE_NODE) {
            try {
                childContext = ctxSwitchedNode.getChildSchema(targetNode);
                ctxSwitchedNode = childContext.getContextSwitchedNode();
                name = getQualifiedDefaultClass(
                        childContext.getContextSwitchedNode());

            } catch (DataModelException e) {
                throw new YobException(ctxSwitchedNode.getName() +
                                               E_HAS_NO_CHILD +
                                               targetNode.getName());
            }
        } else if (ctxSwitchedNode.getYangSchemaNodeType() ==
                YANG_AUGMENT_NODE) {
            name = getQualifiedDefaultClass(ctxSwitchedNode);
            setterInParent = YobUtils.getQualifiedinterface(ctxSwitchedNode);
        } else {
            name = getQualifiedDefaultClass(childContext.getSchemaNode());
        }

        ClassLoader newClassesLoader = YobUtils.getTargetClassLoader(
                curWorkBench.classLoader, childContext, registry);

        return new YobWorkBench(ctxSwitchedNode, newClassesLoader, name,
                                setterInParent);
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
     * @param node     child YDT node
     * @param registry schema registry
     * @return parent builder object
     */
    Object getParentBuilder(YdtExtendedContext node,
                            YangSchemaRegistry registry) {

        // Descendant schema node for whom the builder is required.
        YangSchemaNodeIdentifier targetNode =
                node.getYangSchemaNode().getYangSchemaNodeIdentifier();

        //Current builder container
        YobWorkBench curWorkBench = this;

        YangSchemaNode nonSchemaHolder;
        do {

            //Current Schema node context
            YangSchemaNodeContextInfo schemaContext;
            try {
                //Find the new schema context node.
                schemaContext = curWorkBench.yangSchemaNode
                        .getChildSchema(targetNode);

            } catch (DataModelException e) {
                throw new YobException(yangSchemaNode.getName() +
                                               E_HAS_NO_CHILD +
                                               targetNode.getName());
            }

            nonSchemaHolder = schemaContext.getContextSwitchedNode();

            //If the descendant schema node is in switched context
            if (nonSchemaHolder != null) {

                YangSchemaNodeIdentifier nonSchemaIdentifier =
                        nonSchemaHolder.getYangSchemaNodeIdentifier();

                //check if the descendant builder container is already available
                YobWorkBench childWorkBench =
                        curWorkBench.attributeMap.get(nonSchemaIdentifier);

                if (childWorkBench == null) {
                    YobWorkBench newWorkBench = getNewChildWorkBench(
                            schemaContext, targetNode, curWorkBench, registry);

                    curWorkBench.attributeMap.put(nonSchemaIdentifier,
                                                  newWorkBench);
                    curWorkBench = newWorkBench;
                } else {
                    curWorkBench = childWorkBench;
                }
            }

        } while (nonSchemaHolder != null);

        return curWorkBench.builderOrBuiltObject.getBuilderObject();
    }

    /**
     * Set the operation type attribute and build the object from the builder
     * object, by invoking the build method.
     *
     * @param operationType  data tree node
     * @param schemaRegistry YANG schema registry
     */
    void buildObject(YdtContextOperationType operationType,
                     YangSchemaRegistry schemaRegistry) {

        buildNonSchemaAttributes(operationType, schemaRegistry);

        Object builderObject = builderOrBuiltObject.getBuilderObject();
        Class<?> defaultBuilderClass = builderOrBuiltObject.yangBuilderClass;

        //set the operation type
        setOperationType(operationType, schemaRegistry);

        // Invoking the build method to get built object from build method.
        try {
            Method method = defaultBuilderClass.getDeclaredMethod(BUILD);
            if (method == null) {
                log.error(L_FAIL_TO_GET_METHOD, defaultBuilderClass.getName());
                throw new YobException(E_FAIL_TO_GET_METHOD +
                                               defaultBuilderClass.getName());
            }
            Object builtObject = method.invoke(builderObject);
            // The built object will be maintained in ydt context and same will
            // be used while setting into parent method.
            builderOrBuiltObject.setBuiltObject(builtObject);

        } catch (NoSuchMethodException | InvocationTargetException |
                IllegalAccessException e) {
            log.error(L_FAIL_TO_BUILD, defaultBuilderClass.getName());
            throw new YobException(E_FAIL_TO_BUILD +
                                           defaultBuilderClass.getName());
        }
    }

    /**
     * Set the operation type in the built object from the YDT node.
     * <p>
     * It needs to be invoked only for the workbench corresponding to the
     * schema YDT nodes, non schema node without the YDT node should not
     * invoke this, as it is not applicable to it.
     *
     * @param ydtoperation   schema data tree node
     * @param schemaRegistry YANG schema registry
     */
    private void setOperationType(YdtContextOperationType ydtoperation,
                                  YangSchemaRegistry schemaRegistry) {

        if (ydtoperation == null) {
            return;
        }

        Object builderObject = builderOrBuiltObject.getBuilderObject();
        Class<?> defaultBuilderClass = builderOrBuiltObject.yangBuilderClass;
        Class<?>[] intfClass = builderOrBuiltObject.yangDefaultClass
                .getInterfaces();
        String setterName = YANG + intfClass[0].getSimpleName() + OP_TYPE;

        // Setting the value into YANG node operation type from ydtContext
        // operation type.
        try {
            Class<?> interfaceClass;
            interfaceClass = getModuleInterface(yangSchemaNode,
                                                schemaRegistry);
            Object operationType;
            Class<?>[] innerClasses = interfaceClass.getClasses();
            for (Class<?> innerEnumClass : innerClasses) {
                if (innerEnumClass.getSimpleName().equals(ONOS_YANG_OP_TYPE)) {
                    Method valueOfMethod = innerEnumClass
                            .getDeclaredMethod(VALUE_OF, String.class);
                    operationType = valueOfMethod.invoke(null, ydtoperation.
                            toString());
                    Field operationTypeField = defaultBuilderClass
                            .getDeclaredField(setterName);
                    operationTypeField.setAccessible(true);
                    operationTypeField.set(builderObject, operationType);
                    break;
                }
            }
        } catch (NoSuchMethodException |
                InvocationTargetException | IllegalAccessException |
                IllegalArgumentException e) {
            log.error(E_SET_OP_TYPE_FAIL);
            throw new YobException(E_SET_OP_TYPE_FAIL);
        } catch (NoSuchFieldException e) {
            log.error(E_SET_OP_TYPE_FAIL);
        }
    }

    /**
     * build the non schema objects and maintain it in the contained schema
     * node.
     *
     * @param operationType  contained schema node
     * @param schemaRegistry YANG schema registry
     */
    private void buildNonSchemaAttributes(YdtContextOperationType operationType,
                                          YangSchemaRegistry schemaRegistry) {
        for (Map.Entry<YangSchemaNodeIdentifier, YobWorkBench> entry :
                attributeMap.entrySet()) {
            YobWorkBench childWorkBench = entry.getValue();
            childWorkBench.buildObject(operationType, schemaRegistry);

            if (childWorkBench.yangSchemaNode.getYangSchemaNodeType() ==
                    YANG_AUGMENT_NODE) {
                addInAugmentation(builderOrBuiltObject.getBuilderObject(),
                                  childWorkBench.setterInParent,
                                  childWorkBench.getBuilderOrBuiltObject()
                                          .getBuiltObject());
                continue;
            }

            setObjectInBuilder(
                    builderOrBuiltObject.getBuilderObject(),
                    childWorkBench.setterInParent,
                    SINGLE_INSTANCE_NODE,
                    childWorkBench.getBuilderOrBuiltObject().getBuiltObject());
        }
    }

    /**
     * Sets the YANG built object in corresponding parent class method.
     *
     * @param childnode      ydtExtendedContext is used to get application
     *                       related information maintained in YDT
     * @param schemaRegistry YANG schema registry
     */
    public void setObject(YdtExtendedContext childnode,
                          YangSchemaRegistry schemaRegistry) {
        Object builder = getParentBuilder(childnode, schemaRegistry);
        YobWorkBench childWorkBench = (YobWorkBench) childnode.getAppInfo(YOB);

        setObjectInBuilder(builder, childWorkBench.setterInParent,
                           childnode.getYdtType(), childWorkBench
                                   .builderOrBuiltObject.getBuiltObject());
    }
}
