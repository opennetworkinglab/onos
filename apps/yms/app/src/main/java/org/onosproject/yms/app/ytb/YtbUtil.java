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

package org.onosproject.yms.app.ytb;

import org.onosproject.yangutils.datamodel.YangAugment;
import org.onosproject.yangutils.datamodel.YangCase;
import org.onosproject.yangutils.datamodel.YangIdentity;
import org.onosproject.yangutils.datamodel.YangIdentityRef;
import org.onosproject.yangutils.datamodel.YangLeafRef;
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.datamodel.YangNotification;
import org.onosproject.yangutils.datamodel.YangOutput;
import org.onosproject.yangutils.datamodel.YangRpc;
import org.onosproject.yangutils.datamodel.YangSchemaNode;
import org.onosproject.yangutils.datamodel.YangType;
import org.onosproject.yangutils.datamodel.utils.builtindatatype.YangDataTypes;
import org.onosproject.yangutils.translator.tojava.javamodel.JavaLeafInfoContainer;
import org.onosproject.yms.app.utils.TraversalType;
import org.onosproject.yms.app.ysr.YangSchemaRegistry;
import org.onosproject.yms.ydt.YdtContextOperationType;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static org.onosproject.yangutils.datamodel.YangSchemaNodeType.YANG_AUGMENT_NODE;
import static org.onosproject.yangutils.datamodel.YangSchemaNodeType.YANG_MULTI_INSTANCE_NODE;
import static org.onosproject.yangutils.datamodel.utils.builtindatatype.YangDataTypes.BOOLEAN;
import static org.onosproject.yangutils.datamodel.utils.builtindatatype.YangDataTypes.EMPTY;
import static org.onosproject.yangutils.datamodel.utils.builtindatatype.YangDataTypes.INT16;
import static org.onosproject.yangutils.datamodel.utils.builtindatatype.YangDataTypes.INT32;
import static org.onosproject.yangutils.datamodel.utils.builtindatatype.YangDataTypes.INT64;
import static org.onosproject.yangutils.datamodel.utils.builtindatatype.YangDataTypes.INT8;
import static org.onosproject.yangutils.datamodel.utils.builtindatatype.YangDataTypes.LEAFREF;
import static org.onosproject.yangutils.datamodel.utils.builtindatatype.YangDataTypes.UINT16;
import static org.onosproject.yangutils.datamodel.utils.builtindatatype.YangDataTypes.UINT32;
import static org.onosproject.yangutils.datamodel.utils.builtindatatype.YangDataTypes.UINT8;
import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.getCapitalCase;
import static org.onosproject.yms.app.utils.TraversalType.PARENT;

/**
 * Representation of utility for YANG tree builder.
 */
public final class YtbUtil {

    /**
     * Static attribute for string value having null.
     */
    public static final String STR_NULL = "null";

    /**
     * Static attribute for a dot string.
     */
    public static final String PERIOD = ".";

    private static final int ONE = 1;
    private static final String YANG = "yang";
    private static final String OP_TYPE = "OpType";
    private static final String STR_NONE = "NONE";
    private static final String ENUM_LEAF_IDENTIFIER = "$LeafIdentifier";
    private static final Set<YangDataTypes> PRIMITIVE_TYPES =
            new HashSet<>(Arrays.asList(INT8, INT16, INT32, INT64, UINT8,
                                        UINT16, UINT32, BOOLEAN, EMPTY));
    private static final String TO_STRING = "toString";

    // No instantiation.
    private YtbUtil() {
    }

    /**
     * Returns the object of the node from the node info. Getting object for
     * augment and case differs from other node.
     *
     * @param nodeInfo node info of the holder
     * @param yangNode YANG node of the holder
     * @return object of the parent
     */
    public static Object getParentObjectOfNode(YtbNodeInfo nodeInfo,
                                               YangNode yangNode) {
        Object object;
        if (yangNode instanceof YangCase) {
            object = nodeInfo.getCaseObject();
        } else if (yangNode instanceof YangAugment) {
            object = nodeInfo.getAugmentObject();
        } else {
            object = nodeInfo.getYangObject();
        }
        return object;
    }

    /**
     * Returns the value of an attribute, in a class object. The attribute
     * name is taken from the YANG node java name.
     *
     * @param nodeObj   object of the node
     * @param fieldName name of the attribute
     * @return object of the attribute
     * @throws NoSuchMethodException method not found exception
     */
    public static Object getAttributeOfObject(Object nodeObj, String fieldName)
            throws NoSuchMethodException {
        Class<?> nodeClass = nodeObj.getClass();
        Method getterMethod;
        try {
            getterMethod = nodeClass.getDeclaredMethod(fieldName);
            return getterMethod.invoke(nodeObj);
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new YtbException(e);
        }
    }

    /**
     * Returns the object of the declared method in parent class by invoking
     * through the child class object.
     *
     * @param childClass child class which inherits the parent class
     * @param methodName name of the declared method
     * @return value of the method
     */
    public static Object getAttributeFromInheritance(Object childClass,
                                                     String methodName) {
        Class<?> parentClass = childClass.getClass().getSuperclass();
        Method getterMethod;
        try {
            getterMethod = parentClass.getDeclaredMethod(methodName);
            return getterMethod.invoke(childClass);
        } catch (InvocationTargetException | NoSuchMethodException |
                IllegalAccessException e) {
            throw new YtbException(e);
        }
    }

    /**
     * Returns interface class from an implementation class object.
     *
     * @param obj implementation class object
     * @return interface class
     */
    public static Class<?> getInterfaceClassFromImplClass(Object obj) {
        Class<?>[] interfaces = obj.getClass().getInterfaces();
        if (interfaces.length > ONE) {
            // TODO: Need to handle when impl class has more than one interface.
            throw new YtbException("Implementation class having more than one" +
                                           " interface is not handled");
        }
        return interfaces[0];
    }

    /**
     * Returns the operation type value for a class object. If the operation
     * type is not set, then none type is returned.
     *
     * @param nodeObj  node object
     * @param typeName data type name
     * @return operation type of the class
     */
    public static YdtContextOperationType getNodeOpType(Object nodeObj,
                                                        String typeName) {
        Object opTypeObj;
        try {
            opTypeObj = getAttributeOfObject(nodeObj, typeName);
        } catch (NoSuchMethodException e) {
            return YdtContextOperationType.valueOf(STR_NONE);
        }
        String opTypeValue = String.valueOf(opTypeObj);
        if (opTypeValue.equals(STR_NULL)) {
            return null;
        }
        return YdtContextOperationType.valueOf(opTypeValue);
    }

    /**
     * Returns true, if data type of leaf is primitive data type; false
     * otherwise.
     *
     * @param yangType leaf type
     * @return true if data type is primitive; false otherwise
     */
    public static boolean isTypePrimitive(YangType yangType) {
        if (yangType.getDataType() == LEAFREF) {
            YangLeafRef leafRef =
                    (YangLeafRef) yangType.getDataTypeExtendedInfo();
            return isPrimitiveDataType(leafRef.getEffectiveDataType()
                                               .getDataType());
        }
        return isPrimitiveDataType(yangType.getDataType());
    }

    /**
     * Returns the registered class from the YSR of the module node where
     * augment is present.
     *
     * @param curNode  current augment node
     * @param registry schema registry
     * @return class loader of module
     */
    public static Class<?> getClassLoaderForAugment(
            YangNode curNode, YangSchemaRegistry registry) {
        YangNode moduleNode = curNode.getParent();
        String moduleName = moduleNode.getJavaClassNameOrBuiltInType();
        String modulePackage = moduleNode.getJavaPackage();
        return registry.getRegisteredClass(moduleNode
        );
    }

    /**
     * Returns the string true, if the leaf data is actually set; false
     * otherwise.
     *
     * @param holder     leaf holder
     * @param nodeObj    object if the node
     * @param javaName   java name of the leaf
     * @param methodName getter method name
     * @return string value of the boolean method
     * @throws NoSuchMethodException if the method is not present
     */
    public static String isValueOrSelectLeafSet(YangSchemaNode holder, Object nodeObj,
                                                String javaName, String methodName)
            throws NoSuchMethodException {

        Class<?> nodeClass = nodeObj.getClass();

        // Appends the enum inner package to the interface class package.
        String enumPackage = holder.getJavaPackage() + PERIOD +
                getCapitalCase(holder.getJavaClassNameOrBuiltInType()) +
                ENUM_LEAF_IDENTIFIER;

        ClassLoader classLoader = nodeClass.getClassLoader();
        Class leafEnum;
        try {
            leafEnum = classLoader.loadClass(enumPackage);
            Method getterMethod = nodeClass.getMethod(methodName, leafEnum);
            // Gets the value of the enum.
            Enum<?> value = Enum.valueOf(leafEnum, javaName.toUpperCase());
            // Invokes the method with the value of enum as param.
            return String.valueOf(getterMethod.invoke(nodeObj, value));
        } catch (IllegalAccessException | InvocationTargetException |
                ClassNotFoundException e) {
            throw new YtbException(e);
        }
    }

    /**
     * Returns the string value from the respective data types of the
     * leaf/leaf-list.
     *
     * @param holder    leaf/leaf-list holder
     * @param holderObj leaf/leaf-list holder object
     * @param name      leaf/leaf-list name
     * @param fieldObj  object of the leaf/leaf-list field
     * @param dataType  type of the leaf/leaf-list
     * @return string value from the type
     */
    public static String getStringFromType(YangSchemaNode holder, Object holderObj,
                                           String name, Object fieldObj, YangType dataType) {

        if (fieldObj == null) {
            throw new YtbException("Value of " + holder.getName() + " is null");
        }

        YangDataTypes type = dataType.getDataType();
        switch (type) {
            case INT8:
            case INT16:
            case INT32:
            case INT64:
            case UINT8:
            case UINT16:
            case UINT32:
            case UINT64:
            case EMPTY:
            case STRING:
            case DECIMAL64:
            case INSTANCE_IDENTIFIER:
            case DERIVED:
            case UNION:
            case ENUMERATION:
            case BOOLEAN:
                return String.valueOf(fieldObj).trim();

            case BITS:
                return getBitsValue(holder, holderObj, name, fieldObj).trim();

            case BINARY:
                return Base64.getEncoder().encodeToString((byte[]) fieldObj);

            case IDENTITYREF:
                YangIdentityRef ir =
                        (YangIdentityRef) dataType.getDataTypeExtendedInfo();
                if (ir.isInGrouping()) {
                    return String.valueOf(fieldObj).trim();
                }
                return getIdentityRefValue(fieldObj, ir, holderObj);

            case LEAFREF:
                YangLeafRef leafRef =
                        (YangLeafRef) dataType.getDataTypeExtendedInfo();
                return getStringFromType(holder, holderObj, name, fieldObj,
                                         leafRef.getEffectiveDataType());

            default:
                throw new YtbException("Unsupported data type. Cannot be " +
                                               "processed.");
        }
    }

    /**
     * Returns the string values for the data type bits.
     *
     * @param holder    leaf/leaf-list holder
     * @param holderObj leaf/leaf-list holder object
     * @param name      leaf/leaf-list name
     * @param fieldObj  object of the leaf/leaf-list field
     * @return string value for bits type
     */
    private static String getBitsValue(YangSchemaNode holder, Object holderObj,
                                       String name, Object fieldObj) {

        Class<?> holderClass = holderObj.getClass();
        String interfaceName = holder.getJavaClassNameOrBuiltInType();
        String className = interfaceName.toLowerCase() + PERIOD +
                getCapitalCase(name);
        String pkgName = holder.getJavaPackage() + PERIOD + className;
        ClassLoader classLoader = holderClass.getClassLoader();

        Class<?> bitClass;
        try {
            bitClass = classLoader.loadClass(pkgName);
            Method getterMethod = bitClass.getDeclaredMethod(
                    TO_STRING, fieldObj.getClass());
            return String.valueOf(getterMethod.invoke(null, fieldObj));
        } catch (ClassNotFoundException | NoSuchMethodException |
                InvocationTargetException | IllegalAccessException e) {
            throw new YtbException(e);
        }
    }

    /**
     * Returns the string value of the type identity-ref.
     *
     * @param fieldObj  object of the leaf/leaf-list field
     * @param ir        YANG identity ref
     * @param holderObj leaf/leaf-list holder object
     * @return string value for identity ref type
     */
    private static String getIdentityRefValue(Object fieldObj, YangIdentityRef ir,
                                              Object holderObj) {

        YangIdentity id = ir.getReferredIdentity();
        String idName = id.getJavaClassNameOrBuiltInType();
        String idPkg = id.getJavaPackage() + PERIOD + getCapitalCase(idName);
        String methodName = idName + getCapitalCase(TO_STRING);

        Class<?> holderClass = holderObj.getClass();
        ClassLoader classLoader = holderClass.getClassLoader();
        Class<?> idClass;
        try {
            idClass = classLoader.loadClass(idPkg);
            Method method = idClass.getDeclaredMethod(methodName, null);
            return String.valueOf(method.invoke(fieldObj, null)).trim();
        } catch (ClassNotFoundException | NoSuchMethodException |
                InvocationTargetException | IllegalAccessException e) {
            throw new YtbException(e);
        }
    }

    /**
     * Returns true, if the data type is primitive; false otherwise.
     *
     * @param dataType data type
     * @return true if the data type is primitive; false otherwise
     */
    private static boolean isPrimitiveDataType(YangDataTypes dataType) {
        return PRIMITIVE_TYPES.contains(dataType);
    }

    /**
     * Returns true, if processing of the node is not required; false otherwise.
     * For the nodes such as notification, RPC, augment there is a different
     * flow, so these nodes are skipped in normal conditions.
     *
     * @param yangNode node to be checked
     * @return true if node processing is not required; false otherwise.
     */
    public static boolean isNonProcessableNode(YangNode yangNode) {
        return yangNode != null && (yangNode instanceof YangNotification) ||
                (yangNode instanceof YangRpc) || (yangNode instanceof YangAugment);
    }

    /**
     * Returns true, if multi instance node; false otherwise.
     *
     * @param yangNode YANG node
     * @return true, if multi instance node; false otherwise.
     */
    public static boolean isMultiInstanceNode(YangNode yangNode) {
        return yangNode.getYangSchemaNodeType() == YANG_MULTI_INSTANCE_NODE;
    }

    /**
     * Returns true, if augment node; false otherwise.
     *
     * @param yangNode YANG node
     * @return true, if augment node; false otherwise.
     */
    public static boolean isAugmentNode(YangNode yangNode) {
        return yangNode.getYangSchemaNodeType() == YANG_AUGMENT_NODE;
    }

    /**
     * Returns string for throwing error when empty object is given as input
     * to YTB.
     *
     * @param objName name of the object
     * @return error message
     */
    public static String emptyObjErrMsg(String objName) {
        return "The " + objName + " given for tree creation cannot be null";
    }

    /**
     * Returns the java name for the nodes, leaf/leaf-list.
     *
     * @param node YANG node
     * @return node java name
     */
    public static String getJavaName(Object node) {
        return ((JavaLeafInfoContainer) node).getJavaName(null);
    }

    /**
     * Returns true, if the list is not null and non-empty; false otherwise.
     *
     * @param c collection object
     * @return true, if the list is not null and non-empty; false otherwise
     */
    public static boolean nonEmpty(Collection<?> c) {
        return c != null && !c.isEmpty();
    }

    /**
     * Returns true, if the string is not null and non-empty; false otherwise.
     *
     * @param str string value
     * @return true, if the string is not null and non-empty; false otherwise.
     */
    public static boolean nonEmpty(String str) {
        return str != null && !str.isEmpty();
    }

    /**
     * Returns true when the node processing of RPC and notification is
     * completed; false otherwise. For RPC and notification, processing of
     * other nodes are invalid, so once node gets completed, it must be stopped.
     *
     * @param curNode      current node
     * @param curTraversal current traversal of the node
     * @return true, if the node processing is completed; false otherwise.
     */
    public static boolean isNodeProcessCompleted(
            YangNode curNode, TraversalType curTraversal) {
        return (curTraversal == PARENT &&
                curNode instanceof YangNotification) ||
                curNode instanceof YangOutput;
    }

    /**
     * Returns the name of the operation type variable from the yang name.
     *
     * @param curNode YANG node
     * @return name of operation type
     */
    public static String getOpTypeName(YangNode curNode) {
        return YANG + getCapitalCase(curNode.getJavaClassNameOrBuiltInType()) +
                OP_TYPE;
    }
}
