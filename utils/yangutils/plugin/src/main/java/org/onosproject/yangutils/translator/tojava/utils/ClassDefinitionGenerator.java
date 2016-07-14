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

package org.onosproject.yangutils.translator.tojava.utils;

import org.onosproject.yangutils.datamodel.YangAugment;
import org.onosproject.yangutils.datamodel.YangIdentity;
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.datamodel.YangNotification;
import org.onosproject.yangutils.translator.exception.TranslatorException;
import org.onosproject.yangutils.translator.tojava.JavaFileInfo;
import org.onosproject.yangutils.translator.tojava.JavaFileInfoContainer;
import org.onosproject.yangutils.translator.tojava.JavaQualifiedTypeInfo;
import org.onosproject.yangutils.translator.tojava.TempJavaCodeFragmentFilesContainer;

import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.BUILDER_CLASS_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.BUILDER_INTERFACE_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.GENERATE_ENUM_CLASS;
import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.GENERATE_EVENT_CLASS;
import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.GENERATE_EVENT_LISTENER_INTERFACE;
import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.GENERATE_EVENT_SUBJECT_CLASS;
import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.GENERATE_IDENTITY_CLASS;
import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.GENERATE_SERVICE_AND_MANAGER;
import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.GENERATE_TYPEDEF_CLASS;
import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.GENERATE_UNION_CLASS;
import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.IMPL_CLASS_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.INTERFACE_MASK;
import static org.onosproject.yangutils.utils.UtilConstants.ABSTRACT;
import static org.onosproject.yangutils.utils.UtilConstants.BUILDER;
import static org.onosproject.yangutils.utils.UtilConstants.CLASS;
import static org.onosproject.yangutils.utils.UtilConstants.COMMA;
import static org.onosproject.yangutils.utils.UtilConstants.DEFAULT;
import static org.onosproject.yangutils.utils.UtilConstants.DIAMOND_CLOSE_BRACKET;
import static org.onosproject.yangutils.utils.UtilConstants.DIAMOND_OPEN_BRACKET;
import static org.onosproject.yangutils.utils.UtilConstants.EIGHT_SPACE_INDENTATION;
import static org.onosproject.yangutils.utils.UtilConstants.ENUM;
import static org.onosproject.yangutils.utils.UtilConstants.EVENT_LISTENER_STRING;
import static org.onosproject.yangutils.utils.UtilConstants.EVENT_STRING;
import static org.onosproject.yangutils.utils.UtilConstants.EXTEND;
import static org.onosproject.yangutils.utils.UtilConstants.FINAL;
import static org.onosproject.yangutils.utils.UtilConstants.IMPLEMENTS;
import static org.onosproject.yangutils.utils.UtilConstants.INTERFACE;
import static org.onosproject.yangutils.utils.UtilConstants.LISTENER_REG;
import static org.onosproject.yangutils.utils.UtilConstants.LISTENER_SERVICE;
import static org.onosproject.yangutils.utils.UtilConstants.MANAGER;
import static org.onosproject.yangutils.utils.UtilConstants.NEW_LINE;
import static org.onosproject.yangutils.utils.UtilConstants.OPEN_CURLY_BRACKET;
import static org.onosproject.yangutils.utils.UtilConstants.PERIOD;
import static org.onosproject.yangutils.utils.UtilConstants.PUBLIC;
import static org.onosproject.yangutils.utils.UtilConstants.REGEX_FOR_ANY_STRING_ENDING_WITH_SERVICE;
import static org.onosproject.yangutils.utils.UtilConstants.SERVICE;
import static org.onosproject.yangutils.utils.UtilConstants.SPACE;
import static org.onosproject.yangutils.utils.UtilConstants.SUBJECT;
import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.getCapitalCase;
import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.trimAtLast;

/**
 * Represents generator for class definition of generated files.
 */
public final class ClassDefinitionGenerator {

    /**
     * Creates an instance of class definition generator.
     */
    private ClassDefinitionGenerator() {
    }

    /**
     * Based on the file type and the YANG name of the file, generate the class
     * / interface definition start.
     *
     * @param genFileTypes generated file type
     * @param yangName     class name
     * @return class definition
     */
    public static String generateClassDefinition(int genFileTypes, String yangName) {

        /**
         * Based on the file type and the YANG name of the file, generate the
         * class / interface definition start.
         */
        switch (genFileTypes) {
            case GENERATE_TYPEDEF_CLASS:
            case GENERATE_UNION_CLASS:
                return getTypeClassDefinition(yangName);
            case GENERATE_ENUM_CLASS:
                return getEnumClassDefinition(yangName);
            default:
                return null;
        }
    }

    /**
     * Based on the file type and the YANG name of the file, generate the class
     * / interface definition start.
     *
     * @param genFileTypes generated file type
     * @param yangName     class name
     * @param curNode      current YANG node
     * @return class definition
     */
    public static String generateClassDefinition(int genFileTypes, String yangName, YangNode curNode) {

        /**
         * Based on the file type and the YANG name of the file, generate the
         * class / interface definition start.
         */
        switch (genFileTypes) {
            case INTERFACE_MASK:
                return getInterfaceDefinition(yangName, curNode);
            case BUILDER_CLASS_MASK:
                return getBuilderClassDefinition(yangName, curNode);
            case IMPL_CLASS_MASK:
                return getImplClassDefinition(yangName, curNode);
            case BUILDER_INTERFACE_MASK:
                return getBuilderInterfaceDefinition(yangName, curNode);
            case GENERATE_SERVICE_AND_MANAGER:
                return getRpcInterfaceDefinition(yangName, curNode);
            case GENERATE_EVENT_CLASS:
                String eventName = yangName + SUBJECT;
                return getEventDefinition(yangName, eventName);
            case GENERATE_EVENT_LISTENER_INTERFACE:
                return getEventListenerDefinition(yangName);
            case GENERATE_EVENT_SUBJECT_CLASS:
                return getClassDefinition(yangName);
            case GENERATE_IDENTITY_CLASS:
                return getIdentityClassDefinition(yangName, curNode);
            default:
                return null;
        }
    }

    /**
     * Returns enum file class definition.
     *
     * @param yangName class name
     * @return enum file class definition
     */
    private static String getEnumClassDefinition(String yangName) {
        return PUBLIC + SPACE + ENUM + SPACE + yangName + SPACE + OPEN_CURLY_BRACKET + NEW_LINE;
    }

    /**
     * Returns interface file class definition.
     *
     * @param yangName file name
     * @return definition
     */
    private static String getInterfaceDefinition(String yangName, YangNode curNode) {

        String clsDef = getClassDefinitionForWhenExtended(curNode, yangName, INTERFACE_MASK);
        if (clsDef != null) {
            return clsDef;
        }
        return PUBLIC + SPACE + INTERFACE + SPACE + yangName + SPACE + OPEN_CURLY_BRACKET + NEW_LINE;
    }

    /**
     * Returns builder interface file class definition.
     *
     * @param yangName java class name, corresponding to which the builder class
     *                 is being generated
     * @return definition
     */
    private static String getBuilderInterfaceDefinition(String yangName, YangNode curNode) {
        String clsDef = "";
        if (curNode instanceof YangAugment) {
            clsDef = getClassDefinitionForWhenExtended(curNode, yangName, BUILDER_INTERFACE_MASK);
            if (clsDef != null) {
                return clsDef;
            }
        }
        return INTERFACE + SPACE + yangName + BUILDER + SPACE + OPEN_CURLY_BRACKET + NEW_LINE + NEW_LINE;
    }

    /**
     * Returns builder file class definition.
     *
     * @param yangName file name
     * @return definition
     */
    private static String getBuilderClassDefinition(String yangName, YangNode curNode) {
        String clsDef = "";
        if (curNode instanceof YangAugment) {
            clsDef = getClassDefinitionForWhenExtended(curNode, yangName, BUILDER_CLASS_MASK);
            if (clsDef != null) {
                return clsDef;
            }
        }
        return PUBLIC + SPACE + CLASS + SPACE + yangName + BUILDER + SPACE + IMPLEMENTS + SPACE + yangName + PERIOD
                + yangName + BUILDER + SPACE + OPEN_CURLY_BRACKET + NEW_LINE;
    }

    /**
     * Returns impl file class definition.
     *
     * @param yangName file name
     * @return definition
     */
    private static String getImplClassDefinition(String yangName, YangNode curNode) {
        String clsDef = "";
        if (curNode instanceof YangAugment) {
            clsDef = getClassDefinitionForWhenExtended(curNode, yangName, IMPL_CLASS_MASK);
            if (clsDef != null) {
                return clsDef;
            }
        }
        return PUBLIC + SPACE + CLASS + SPACE + getCapitalCase(DEFAULT) + yangName + SPACE + IMPLEMENTS + SPACE
                + yangName + SPACE + OPEN_CURLY_BRACKET + NEW_LINE;
    }

    /**
     * Returns impl file class definition.
     *
     * @param yangName file name
     * @return definition
     */
    private static String getClassDefinition(String yangName) {
        return PUBLIC + SPACE + CLASS + SPACE + yangName + SPACE + OPEN_CURLY_BRACKET + NEW_LINE;
    }

    /**
     * Returns implementation file identity class definition.
     *
     * @param yangName file name
     * @return identity class definition
     */
    private static String getIdentityClassDefinition(String yangName, YangNode curNode) {
        if (!(curNode instanceof YangIdentity)) {
            throw new TranslatorException("Expected java identity instance node");
        }
        YangIdentity identity = (YangIdentity) curNode;
        if (identity.getBaseNode() != null) {
            YangIdentity baseIdentity = identity.getBaseNode().getReferredIdentity();
            if (!(baseIdentity instanceof YangIdentity)) {
                throw new TranslatorException("Expected java identity instance node");
            }

            JavaFileInfo fileInfo = ((JavaFileInfoContainer) baseIdentity).getJavaFileInfo();
            return PUBLIC + SPACE + ABSTRACT + SPACE + CLASS + SPACE + yangName + SPACE + EXTEND + SPACE
                    + getCapitalCase(fileInfo.getJavaName()) + SPACE +
                    OPEN_CURLY_BRACKET + NEW_LINE;
        }

        return PUBLIC + SPACE + ABSTRACT + SPACE + CLASS + SPACE + yangName + SPACE + OPEN_CURLY_BRACKET + NEW_LINE;
    }

    /**
     * Returns type file class definition.
     *
     * @param yangName file name
     * @return definition
     */
    private static String getTypeClassDefinition(String yangName) {
        return PUBLIC + SPACE + FINAL + SPACE + CLASS + SPACE + yangName + SPACE + OPEN_CURLY_BRACKET + NEW_LINE;
    }

    /**
     * Returns RPC file interface definition.
     *
     * @param yangName file name
     * @param curNode  current YANG node
     * @return definition
     */
    private static String getRpcInterfaceDefinition(String yangName, YangNode curNode) {
        JavaExtendsListHolder holder = ((TempJavaCodeFragmentFilesContainer) curNode)
                .getTempJavaCodeFragmentFiles().getServiceTempFiles().getJavaExtendsListHolder();
        if (holder.getExtendsList() != null && !holder.getExtendsList().isEmpty()) {
            curNode = curNode.getChild();
            while (curNode != null) {
                if (curNode instanceof YangNotification) {
                    return getRpcInterfaceDefinitionWhenItExtends(yangName);
                }
                curNode = curNode.getNextSibling();
            }
        }
        if (yangName.matches(REGEX_FOR_ANY_STRING_ENDING_WITH_SERVICE)) {
            return PUBLIC + SPACE + INTERFACE + SPACE + yangName + SPACE + OPEN_CURLY_BRACKET + NEW_LINE;
        }
        return PUBLIC + SPACE + CLASS + SPACE + yangName + SPACE + IMPLEMENTS + SPACE
                + yangName.substring(0, yangName.length() - 7) + SERVICE + SPACE + OPEN_CURLY_BRACKET + NEW_LINE;
    }

    /* Provides class definition when RPC interface needs to extends any event.*/
    private static String getRpcInterfaceDefinitionWhenItExtends(String yangName) {

        if (yangName.matches(REGEX_FOR_ANY_STRING_ENDING_WITH_SERVICE)) {
            String[] strArray = yangName.split(SERVICE);
            return PUBLIC + SPACE + INTERFACE + SPACE + yangName + NEW_LINE + EIGHT_SPACE_INDENTATION
                    + EXTEND + SPACE + LISTENER_SERVICE + DIAMOND_OPEN_BRACKET + strArray[0] + EVENT_STRING + COMMA
                    + SPACE + strArray[0] + EVENT_LISTENER_STRING + DIAMOND_CLOSE_BRACKET + SPACE
                    + OPEN_CURLY_BRACKET + NEW_LINE;
        }
        yangName = yangName.substring(0, yangName.length() - 7);
        return PUBLIC + SPACE + CLASS + SPACE + yangName + MANAGER + NEW_LINE + EIGHT_SPACE_INDENTATION
                + EXTEND + SPACE + LISTENER_REG + DIAMOND_OPEN_BRACKET + yangName + EVENT_STRING + COMMA + SPACE
                + yangName + EVENT_LISTENER_STRING + DIAMOND_CLOSE_BRACKET + NEW_LINE
                + EIGHT_SPACE_INDENTATION + IMPLEMENTS + SPACE + yangName + SERVICE + SPACE + OPEN_CURLY_BRACKET
                + NEW_LINE;
    }

    /**
     * Returns event class definition.
     *
     * @param javaName file name
     * @return definition
     */
    private static String getEventDefinition(String javaName, String eventName) {
        String classDef = PUBLIC + SPACE + CLASS + SPACE + javaName + SPACE + "extends AbstractEvent<"
                + javaName + ".Type, " + eventName + ">" + SPACE + OPEN_CURLY_BRACKET + NEW_LINE;

        return classDef;
    }

    /**
     * Returns event listener interface definition.
     *
     * @param javaName file name
     * @return definition
     */
    private static String getEventListenerDefinition(String javaName) {
        String intfDef = PUBLIC + SPACE + INTERFACE + SPACE + javaName + SPACE + "extends EventListener<"
                + javaName;
        if (intfDef.length() < 8) {
            throw new RuntimeException("Event listener interface name is error");
        }
        intfDef = intfDef.substring(0, intfDef.length() - 8);
        intfDef = intfDef + "Event>" + SPACE + OPEN_CURLY_BRACKET + NEW_LINE;

        return intfDef;
    }

    /**
     * Returns class definition when class is extending another class.
     *
     * @param curNode      current node
     * @param yangName     name
     * @param genFileTypes gen file type
     * @return class definition
     */
    private static String getClassDefinitionForWhenExtended(YangNode curNode, String yangName, int genFileTypes) {
        JavaExtendsListHolder holder = ((TempJavaCodeFragmentFilesContainer) curNode)
                .getTempJavaCodeFragmentFiles().getBeanTempFiles().getJavaExtendsListHolder();

        if (holder.getExtendsList() != null && !holder.getExtendsList().isEmpty()) {
            String def = PUBLIC + SPACE;
            switch (genFileTypes) {
                case INTERFACE_MASK:
                    def = def + INTERFACE + SPACE + yangName + SPACE + EXTEND + SPACE;
                    for (JavaQualifiedTypeInfo info : holder.getExtendsList()) {
                        if (curNode instanceof YangAugment) {
                            if (!info.getClassInfo().contains(BUILDER) && !info.getClassInfo().contains(
                                    getCapitalCase(DEFAULT))) {
                                def = getDefinitionString(def, info, holder);
                            }
                        } else {
                            def = getDefinitionString(def, info, holder);
                        }
                    }
                    def = trimAtLast(def, COMMA);

                    return def + SPACE + OPEN_CURLY_BRACKET + NEW_LINE;
                case BUILDER_INTERFACE_MASK:
                    String builderDef = INTERFACE + SPACE + yangName + BUILDER + SPACE + EXTEND + SPACE;
                    for (JavaQualifiedTypeInfo info : holder.getExtendsList()) {
                        if (info.getClassInfo().contains(BUILDER) && !info.getClassInfo().contains(
                                getCapitalCase(DEFAULT))) {
                            builderDef = getDefinitionString(builderDef, info, holder);
                        }
                    }

                    builderDef = trimAtLast(builderDef, COMMA);

                    return builderDef + SPACE + OPEN_CURLY_BRACKET + NEW_LINE;
                case BUILDER_CLASS_MASK:
                    def = def + CLASS + SPACE + yangName + BUILDER + SPACE + EXTEND + SPACE;
                    for (JavaQualifiedTypeInfo info : holder.getExtendsList()) {
                        if (info.getClassInfo().contains(BUILDER)
                                && info.getClassInfo().contains(getCapitalCase(DEFAULT))) {
                            def = getDefinitionString(def, info, holder);
                        }
                    }

                    def = trimAtLast(def, COMMA);

                    return def + SPACE + IMPLEMENTS + SPACE + yangName + PERIOD
                            + yangName + BUILDER + SPACE + OPEN_CURLY_BRACKET + NEW_LINE;

                case IMPL_CLASS_MASK:
                    def = def + SPACE + CLASS + SPACE + getCapitalCase(DEFAULT) + yangName + SPACE + EXTEND + SPACE;
                    for (JavaQualifiedTypeInfo info : holder.getExtendsList()) {
                        if (!info.getClassInfo().contains(BUILDER)
                                && info.getClassInfo().contains(getCapitalCase(DEFAULT))) {
                            def = getDefinitionString(def, info, holder);
                        }
                    }

                    def = trimAtLast(def, COMMA);

                    return def + SPACE + IMPLEMENTS + SPACE
                            + yangName + SPACE + OPEN_CURLY_BRACKET + NEW_LINE;

                default:
                    return null;
            }
        }
        return null;
    }

    /**
     * Returns updated class definition.
     *
     * @param def    current definition
     * @param info   java qualified info
     * @param holder extend list holder
     * @return updated class definition
     */
    private static String getDefinitionString(String def, JavaQualifiedTypeInfo info, JavaExtendsListHolder holder) {
        if (!holder.getExtendedClassStore().get(info)) {
            def = def + info.getClassInfo() + COMMA + SPACE;
        } else {
            def = def + info.getPkgInfo() + PERIOD + info.getClassInfo() + COMMA + SPACE;
        }
        return def;
    }

}
