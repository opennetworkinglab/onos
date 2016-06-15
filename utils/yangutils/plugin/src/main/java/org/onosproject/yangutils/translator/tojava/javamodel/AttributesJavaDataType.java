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

package org.onosproject.yangutils.translator.tojava.javamodel;

import java.util.Stack;
import org.onosproject.yangutils.datamodel.YangDataTypes;
import org.onosproject.yangutils.datamodel.YangDerivedInfo;
import org.onosproject.yangutils.datamodel.YangEnumeration;
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.datamodel.YangType;
import org.onosproject.yangutils.datamodel.YangTypeDef;
import org.onosproject.yangutils.datamodel.YangUnion;
import org.onosproject.yangutils.translator.exception.TranslatorException;
import org.onosproject.yangutils.translator.tojava.JavaCodeGeneratorInfo;
import org.onosproject.yangutils.translator.tojava.JavaFileInfo;
import org.onosproject.yangutils.translator.tojava.JavaFileInfoContainer;
import org.onosproject.yangutils.utils.io.impl.YangToJavaNamingConflictUtil;

import static org.onosproject.yangutils.translator.tojava.javamodel.YangJavaModelUtils.getCurNodePackage;
import static org.onosproject.yangutils.translator.tojava.utils.JavaIdentifierSyntax.getRootPackage;
import static org.onosproject.yangutils.utils.UtilConstants.BIG_INTEGER;
import static org.onosproject.yangutils.utils.UtilConstants.BOOLEAN_DATA_TYPE;
import static org.onosproject.yangutils.utils.UtilConstants.BOOLEAN_WRAPPER;
import static org.onosproject.yangutils.utils.UtilConstants.BYTE;
import static org.onosproject.yangutils.utils.UtilConstants.BYTE_WRAPPER;
import static org.onosproject.yangutils.utils.UtilConstants.INT;
import static org.onosproject.yangutils.utils.UtilConstants.INTEGER_WRAPPER;
import static org.onosproject.yangutils.utils.UtilConstants.JAVA_LANG;
import static org.onosproject.yangutils.utils.UtilConstants.JAVA_MATH;
import static org.onosproject.yangutils.utils.UtilConstants.LONG;
import static org.onosproject.yangutils.utils.UtilConstants.LONG_WRAPPER;
import static org.onosproject.yangutils.utils.UtilConstants.PERIOD;
import static org.onosproject.yangutils.utils.UtilConstants.SHORT;
import static org.onosproject.yangutils.utils.UtilConstants.SHORT_WRAPPER;
import static org.onosproject.yangutils.utils.UtilConstants.STRING_DATA_TYPE;
import static org.onosproject.yangutils.utils.UtilConstants.YANG_BINARY_CLASS;
import static org.onosproject.yangutils.utils.UtilConstants.YANG_BITS_CLASS;
import static org.onosproject.yangutils.utils.UtilConstants.YANG_DECIMAL64_CLASS;
import static org.onosproject.yangutils.utils.UtilConstants.YANG_TYPES_PKG;
import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.getCamelCase;
import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.getCapitalCase;
import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.getPackageDirPathFromJavaJPackage;

/**
 * Represents java data types info corresponding to YANG type.
 */
public final class AttributesJavaDataType {

    /**
     * Creates an instance of attribute java data type.
     */
    private AttributesJavaDataType() {
    }

    /**
     * Returns java type.
     *
     * @param yangType YANG type
     * @return java type
     */
    public static String getJavaDataType(YangType<?> yangType) {

        YangDataTypes type = yangType.getDataType();

        switch (type) {
            case INT8:
                return BYTE;
            case INT16:
                return SHORT;
            case INT32:
                return INT;
            case INT64:
                return LONG;
            case UINT8:
                return SHORT;
            case UINT16:
                return INT;
            case UINT32:
                return LONG;
            case UINT64:
                return BIG_INTEGER;
            case BINARY:
                return YANG_BINARY_CLASS;
            case DECIMAL64:
                return YANG_DECIMAL64_CLASS;
            case STRING:
                return STRING_DATA_TYPE;
            case BOOLEAN:
                return BOOLEAN_DATA_TYPE;
            default:
                throw new TranslatorException("given data type is not supported.");
        }
    }

    /**
     * Returns java import class.
     *
     * @param yangType     YANG type
     * @param isListAttr   if the attribute need to be a list
     * @param pluginConfig plugin configurations
     * @return java import class
     */
    public static String getJavaImportClass(YangType<?> yangType, boolean isListAttr,
                                            YangToJavaNamingConflictUtil pluginConfig) {

        YangDataTypes type = yangType.getDataType();

        if (isListAttr) {
            switch (type) {
                case INT8:
                    return BYTE_WRAPPER;
                case INT16:
                    return SHORT_WRAPPER;
                case INT32:
                    return INTEGER_WRAPPER;
                case INT64:
                    return LONG_WRAPPER;
                case UINT8:
                    return SHORT_WRAPPER;
                case UINT16:
                    return INTEGER_WRAPPER;
                case UINT32:
                    return LONG_WRAPPER;
                case UINT64:
                    return BIG_INTEGER;
                case DECIMAL64:
                    return YANG_DECIMAL64_CLASS;
                case STRING:
                    return STRING_DATA_TYPE;
                case BOOLEAN:
                    return BOOLEAN_WRAPPER;
                case ENUMERATION:
                    return getCapitalCase(
                            getCamelCase(((YangJavaEnumeration) yangType.getDataTypeExtendedInfo()).getName(),
                                    pluginConfig));
                case BITS:
                    return YANG_BITS_CLASS;
                case BINARY:
                    return YANG_BINARY_CLASS;
                case LEAFREF:
                    //TODO:LEAFREF
                    break;
                case IDENTITYREF:
                    //TODO:IDENTITYREF
                    break;
                case EMPTY:
                    return BOOLEAN_WRAPPER;
                case UNION:
                    return getCapitalCase(getCamelCase(((YangJavaUnion) yangType.getDataTypeExtendedInfo()).getName(),
                            pluginConfig));
                case INSTANCE_IDENTIFIER:
                    //TODO:INSTANCE_IDENTIFIER
                    break;
                case DERIVED:
                    return getCapitalCase(
                            getCamelCase(yangType.getDataTypeName(), pluginConfig));
                default:
                    throw new TranslatorException("given data type is not supported.");
            }
        } else {
            switch (type) {
                case UINT64:
                    return BIG_INTEGER;
                case DECIMAL64:
                    return YANG_DECIMAL64_CLASS;
                case STRING:
                    return STRING_DATA_TYPE;
                case ENUMERATION:
                    return getCapitalCase(
                            getCamelCase(((YangJavaEnumeration) yangType.getDataTypeExtendedInfo()).getName(),
                                    pluginConfig));
                case BITS:
                    return YANG_BITS_CLASS;
                case BINARY:
                    return YANG_BINARY_CLASS;
                case LEAFREF:
                    //TODO:LEAFREF
                    break;
                case IDENTITYREF:
                    //TODO:IDENTITYREF
                    break;
                case EMPTY:
                    return BOOLEAN_DATA_TYPE;
                case UNION:
                    return getCapitalCase(getCamelCase(((YangJavaUnion) yangType.getDataTypeExtendedInfo()).getName(),
                            pluginConfig));
                case INSTANCE_IDENTIFIER:
                    //TODO:INSTANCE_IDENTIFIER
                    break;
                case DERIVED:
                    return getCapitalCase(
                            getCamelCase(yangType.getDataTypeName(), pluginConfig));
                default:
                    return null;
            }
        }
        return null;
    }

    /**
     * Returns java import package.
     *
     * @param yangType         YANG type
     * @param isListAttr       if the attribute is of list type
     * @param conflictResolver object of YANG to java naming conflict util
     * @return java import package
     */
    public static String getJavaImportPackage(YangType<?> yangType, boolean isListAttr,
                                              YangToJavaNamingConflictUtil conflictResolver) {

        YangDataTypes type = yangType.getDataType();

        if (isListAttr) {
            switch (type) {
                case INT8:
                case INT16:
                case INT32:
                case INT64:
                case UINT8:
                case UINT16:
                case UINT32:
                case STRING:
                case BOOLEAN:
                case EMPTY:
                    return JAVA_LANG;
                case UINT64:
                    return JAVA_MATH;
                case ENUMERATION:
                    return getEnumsPackage(yangType, conflictResolver);
                case DECIMAL64:
                case BITS:
                case BINARY:
                    return YANG_TYPES_PKG;
                case LEAFREF:
                    //TODO:LEAFREF
                    break;
                case IDENTITYREF:
                    //TODO:IDENTITYREF
                    break;
                case UNION:
                    return getUnionPackage(yangType, conflictResolver);
                case INSTANCE_IDENTIFIER:
                    //TODO:INSTANCE_IDENTIFIER
                    break;
                case DERIVED:
                    return getTypDefsPackage(yangType, conflictResolver);
                default:
                    throw new TranslatorException("given data type is not supported.");
            }
        } else {
            switch (type) {
                case UINT64:
                    return JAVA_MATH;
                case STRING:
                    return JAVA_LANG;
                case ENUMERATION:
                    return getEnumsPackage(yangType, conflictResolver);
                case DECIMAL64:
                case BITS:
                case BINARY:
                    return YANG_TYPES_PKG;
                case LEAFREF:
                    //TODO:LEAFREF
                    break;
                case IDENTITYREF:
                    //TODO:IDENTITYREF
                    break;
                case EMPTY:
                    return JAVA_LANG;
                case UNION:
                    return getUnionPackage(yangType, conflictResolver);
                case INSTANCE_IDENTIFIER:
                    //TODO:INSTANCE_IDENTIFIER
                    break;
                case DERIVED:
                    return getTypDefsPackage(yangType, conflictResolver);
                default:
                    return null;
            }
        }
        return null;
    }

    /**
     * Returns java package for typedef node.
     *
     * @param type             YANG type
     * @param conflictResolver object of YANG to java naming conflict util
     * @return java package for typedef node
     */
    private static String getTypDefsPackage(YangType<?> type, YangToJavaNamingConflictUtil conflictResolver) {
        Object var = type.getDataTypeExtendedInfo();
        if (!(var instanceof YangDerivedInfo)) {
            throw new TranslatorException("type should have been derived.");
        }

        if (!(((YangDerivedInfo<?>) var).getReferredTypeDef() instanceof YangTypeDef)) {
            throw new TranslatorException("derived info is not an instance of typedef.");
        }

        YangJavaTypeDef typedef = (YangJavaTypeDef) ((YangDerivedInfo<?>) var).getReferredTypeDef();
        if (typedef.getJavaFileInfo().getPackage() == null) {
            return getPackageFromParent(typedef.getParent(), conflictResolver);
        }
        return typedef.getJavaFileInfo().getPackage();
    }

    /**
     * Returns java package for union node.
     *
     * @param type             YANG type
     * @param conflictResolver object of YANG to java naming conflict util
     * @return java package for union node
     */
    private static String getUnionPackage(YangType<?> type, YangToJavaNamingConflictUtil conflictResolver) {

        if (!(type.getDataTypeExtendedInfo() instanceof YangUnion)) {
            throw new TranslatorException("type should have been union.");
        }

        YangJavaUnion union = (YangJavaUnion) type.getDataTypeExtendedInfo();
        if (union.getJavaFileInfo().getPackage() == null) {
            return getPackageFromParent(union.getParent(), conflictResolver);
        }
        return union.getJavaFileInfo().getPackage();
    }

    /**
     * Returns YANG enumeration's java package.
     *
     * @param type             YANG type
     * @param conflictResolver object of YANG to java naming conflict util
     * @return YANG enumeration's java package
     */
    private static String getEnumsPackage(YangType<?> type, YangToJavaNamingConflictUtil conflictResolver) {

        if (!(type.getDataTypeExtendedInfo() instanceof YangEnumeration)) {
            throw new TranslatorException("type should have been enumeration.");
        }
        YangJavaEnumeration enumeration = (YangJavaEnumeration) type.getDataTypeExtendedInfo();
        if (enumeration.getJavaFileInfo().getPackage() == null) {
            return getPackageFromParent(enumeration.getParent(), conflictResolver);
        }
        return enumeration.getJavaFileInfo().getPackage();
    }

    /**
     * Returns package from parent node.
     *
     * @param parent           parent YANG node
     * @param conflictResolver object of YANG to java naming conflict util
     * @return java package from parent node
     */
    private static String getPackageFromParent(YangNode parent,
                                               YangToJavaNamingConflictUtil conflictResolver) {
        if (!(parent instanceof JavaFileInfoContainer)) {
            throw new TranslatorException("invalid child node is being processed.");
        }
        JavaFileInfo parentInfo = ((JavaFileInfoContainer) parent).getJavaFileInfo();
        if (parentInfo.getPackage() == null) {
            updateJavaFileInfo(parent, conflictResolver);
        }
        return parentInfo.getPackage() + PERIOD + parentInfo.getJavaName().toLowerCase();
    }

    /**
     * Update the referred data model nodes java file info, this will be called,
     * when the linked node is yet to translate. Then resolve until the parent hierarchy.
     *
     * @param yangNode         node whose java info needs to be updated
     * @param conflictResolver yang plugin config
     */
    public static void updateJavaFileInfo(YangNode yangNode,
                                          YangToJavaNamingConflictUtil conflictResolver) {
        Stack<YangNode> nodesToUpdatePackage = new Stack<YangNode>();

        /*
         * Add the nodes to be updated for package info in a stack.
         */
        while (yangNode != null
                && ((JavaFileInfoContainer) yangNode)
                .getJavaFileInfo().getPackage() == null) {
            nodesToUpdatePackage.push(yangNode);
            yangNode = yangNode.getParent();
        }

        /*
         * If the package is not updated till root node, then root package needs to
         * be updated.
         */
        if (yangNode == null) {
            yangNode = nodesToUpdatePackage.pop();
            String pkg;
            if (yangNode instanceof YangJavaModule) {
                YangJavaModule module = (YangJavaModule) yangNode;
                pkg = getRootPackage(module.getVersion(), module.getNameSpace().getUri(), module
                        .getRevision().getRevDate(), conflictResolver);
            } else if (yangNode instanceof YangJavaSubModule) {
                YangJavaSubModule submodule = (YangJavaSubModule) yangNode;
                pkg = getRootPackage(submodule.getVersion(),
                        submodule.getNameSpaceFromModule(submodule.getBelongsTo()),
                        submodule.getRevision().getRevDate(), conflictResolver);
            } else {
                throw new TranslatorException("Invalid root node of data model tree");
            }

            ((JavaCodeGeneratorInfo) yangNode).getJavaFileInfo()
                    .setJavaName(getCamelCase(yangNode.getName(), conflictResolver));
            ((JavaCodeGeneratorInfo) yangNode).getJavaFileInfo()
                    .setPackage(pkg);
            ((JavaCodeGeneratorInfo) yangNode).getJavaFileInfo()
                    .setPackageFilePath(getPackageDirPathFromJavaJPackage(
                            ((JavaCodeGeneratorInfo) yangNode).getJavaFileInfo()
                                    .getPackage()));
        }

        /**
         * Parent of the node in stack is updated with java info,
         * all the nodes can be popped and updated
         */
        while (nodesToUpdatePackage.size() != 0) {
            yangNode = nodesToUpdatePackage.pop();
            ((JavaCodeGeneratorInfo) yangNode).getJavaFileInfo()
                    .setJavaName(getCamelCase(yangNode.getName(), conflictResolver));
            ((JavaCodeGeneratorInfo) yangNode).getJavaFileInfo()
                    .setPackage(getCurNodePackage(yangNode));
            ((JavaCodeGeneratorInfo) yangNode).getJavaFileInfo()
                    .setPackageFilePath(getPackageDirPathFromJavaJPackage(
                            ((JavaCodeGeneratorInfo) yangNode).getJavaFileInfo()
                                    .getPackage()));
        }
    }
}
