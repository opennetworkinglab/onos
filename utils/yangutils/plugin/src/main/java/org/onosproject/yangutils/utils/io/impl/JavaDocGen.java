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

package org.onosproject.yangutils.utils.io.impl;

import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.getCamelCase;
import static org.onosproject.yangutils.utils.UtilConstants.BUILDER;
import static org.onosproject.yangutils.utils.UtilConstants.BUILDER_CLASS_JAVA_DOC;
import static org.onosproject.yangutils.utils.UtilConstants.BUILDER_INTERFACE_JAVA_DOC;
import static org.onosproject.yangutils.utils.UtilConstants.BUILDER_OBJECT;
import static org.onosproject.yangutils.utils.UtilConstants.EMPTY_STRING;
import static org.onosproject.yangutils.utils.UtilConstants.ENUM_ATTRIBUTE_JAVADOC;
import static org.onosproject.yangutils.utils.UtilConstants.ENUM_CLASS_JAVADOC;
import static org.onosproject.yangutils.utils.UtilConstants.EVENT_JAVA_DOC;
import static org.onosproject.yangutils.utils.UtilConstants.EVENT_LISTENER_JAVA_DOC;
import static org.onosproject.yangutils.utils.UtilConstants.FOUR_SPACE_INDENTATION;
import static org.onosproject.yangutils.utils.UtilConstants.FROM_STRING_METHOD_NAME;
import static org.onosproject.yangutils.utils.UtilConstants.FROM_STRING_PARAM_NAME;
import static org.onosproject.yangutils.utils.UtilConstants.IMPL;
import static org.onosproject.yangutils.utils.UtilConstants.IMPL_CLASS_JAVA_DOC;
import static org.onosproject.yangutils.utils.UtilConstants.INPUT;
import static org.onosproject.yangutils.utils.UtilConstants.INTERFACE_JAVA_DOC;
import static org.onosproject.yangutils.utils.UtilConstants.JAVA_DOC_BUILD;
import static org.onosproject.yangutils.utils.UtilConstants.JAVA_DOC_BUILD_RETURN;
import static org.onosproject.yangutils.utils.UtilConstants.JAVA_DOC_CONSTRUCTOR;
import static org.onosproject.yangutils.utils.UtilConstants.JAVA_DOC_END_LINE;
import static org.onosproject.yangutils.utils.UtilConstants.JAVA_DOC_FIRST_LINE;
import static org.onosproject.yangutils.utils.UtilConstants.JAVA_DOC_GETTERS;
import static org.onosproject.yangutils.utils.UtilConstants.JAVA_DOC_MANAGER_SETTERS;
import static org.onosproject.yangutils.utils.UtilConstants.JAVA_DOC_OF;
import static org.onosproject.yangutils.utils.UtilConstants.JAVA_DOC_PARAM;
import static org.onosproject.yangutils.utils.UtilConstants.JAVA_DOC_RETURN;
import static org.onosproject.yangutils.utils.UtilConstants.JAVA_DOC_RPC;
import static org.onosproject.yangutils.utils.UtilConstants.JAVA_DOC_SETTERS;
import static org.onosproject.yangutils.utils.UtilConstants.JAVA_DOC_SETTERS_COMMON;
import static org.onosproject.yangutils.utils.UtilConstants.LIST;
import static org.onosproject.yangutils.utils.UtilConstants.NEW_LINE;
import static org.onosproject.yangutils.utils.UtilConstants.NEW_LINE_ASTERISK;
import static org.onosproject.yangutils.utils.UtilConstants.OBJECT;
import static org.onosproject.yangutils.utils.UtilConstants.OF;
import static org.onosproject.yangutils.utils.UtilConstants.PACKAGE_INFO_JAVADOC;
import static org.onosproject.yangutils.utils.UtilConstants.PACKAGE_INFO_JAVADOC_OF_CHILD;
import static org.onosproject.yangutils.utils.UtilConstants.PERIOD;
import static org.onosproject.yangutils.utils.UtilConstants.RPC_INPUT_STRING;
import static org.onosproject.yangutils.utils.UtilConstants.RPC_OUTPUT_STRING;
import static org.onosproject.yangutils.utils.UtilConstants.SPACE;
import static org.onosproject.yangutils.utils.UtilConstants.STRING_DATA_TYPE;
import static org.onosproject.yangutils.utils.UtilConstants.VALUE;
import static org.onosproject.yangutils.utils.UtilConstants.VOID;

/**
 * Represents javadoc for the generated classes.
 */
public final class JavaDocGen {

    /**
     * Creates an instance of java doc gen.
     */
    private JavaDocGen() {
    }

    /**
     * JavaDocs types.
     */
    public enum JavaDocType {

        /**
         * For class.
         */
        IMPL_CLASS,

        /**
         * For builder class.
         */
        BUILDER_CLASS,

        /**
         * For interface.
         */
        INTERFACE,

        /**
         * For builder interface.
         */
        BUILDER_INTERFACE,

        /**
         * For package-info.
         */
        PACKAGE_INFO,

        /**
         * For getters.
         */
        GETTER_METHOD,

        /**
         * For rpc service.
         */
        RPC_INTERFACE,

        /**
         * For rpc manager.
         */
        RPC_MANAGER,

        /**
         * For event.
         */
        EVENT,

        /**
         * For event listener.
         */
        EVENT_LISTENER,

        /**
         * For setters.
         */
        SETTER_METHOD,

        /**
         * For type def's setters.
         */
        TYPE_DEF_SETTER_METHOD,

        /**
         * For of method.
         */
        OF_METHOD,

        /**
         * For default constructor.
         */
        DEFAULT_CONSTRUCTOR,

        /**
         * For constructor.
         */
        CONSTRUCTOR,

        /**
         * For from method.
         */
        FROM_METHOD,

        /**
         * For type constructor.
         */
        TYPE_CONSTRUCTOR,

        /**
         * For build.
         */
        BUILD_METHOD,

        /**
         * For enum.
         */
        ENUM_CLASS,

        /**
         * For enum's attributes.
         */
        ENUM_ATTRIBUTE,

        /**
         * For manager setters.
         */
        MANAGER_SETTER_METHOD,

        /**
         * For event subject.
         */
        EVENT_SUBJECT_CLASS
    }

    /**
     * Returns java docs.
     *
     * @param type java doc type
     * @param name name of the YangNode
     * @param isList is list attribute
     * @param pluginConfig plugin configurations
     * @return javadocs.
     */
    public static String getJavaDoc(JavaDocType type, String name, boolean isList, YangPluginConfig pluginConfig) {

        name = YangIoUtils.getSmallCase(getCamelCase(name, pluginConfig.getConflictResolver()));
        switch (type) {
            case IMPL_CLASS: {
                return generateForClass(name);
            }
            case BUILDER_CLASS: {
                return generateForBuilderClass(name);
            }
            case INTERFACE: {
                return generateForInterface(name);
            }
            case BUILDER_INTERFACE: {
                return generateForBuilderInterface(name);
            }
            case PACKAGE_INFO: {
                return generateForPackage(name, isList);
            }
            case GETTER_METHOD: {
                return generateForGetters(name, isList);
            }
            case TYPE_DEF_SETTER_METHOD: {
                return generateForTypeDefSetter(name);
            }
            case SETTER_METHOD: {
                return generateForSetters(name, isList);
            }
            case MANAGER_SETTER_METHOD: {
                return generateForManagerSetters(name, isList);
            }
            case OF_METHOD: {
                return generateForOf(name);
            }
            case DEFAULT_CONSTRUCTOR: {
                return generateForDefaultConstructors(name);
            }
            case BUILD_METHOD: {
                return generateForBuild(name);
            }
            case TYPE_CONSTRUCTOR: {
                return generateForTypeConstructor(name);
            }
            case FROM_METHOD: {
                return generateForFromString(name);
            }
            case ENUM_CLASS: {
                return generateForEnum(name);
            }
            case ENUM_ATTRIBUTE: {
                return generateForEnumAttr(name);
            }
            case RPC_INTERFACE: {
               return generateForRpcService(name);
            }
            case RPC_MANAGER: {
               return generateForClass(name);
            }
            case EVENT: {
                return generateForEvent(name);
            }
            case EVENT_LISTENER: {
                return generateForEventListener(name);
            }
            case EVENT_SUBJECT_CLASS: {
                return generateForClass(name);
            }
            default: {
                return generateForConstructors(name);
            }
        }
    }

    /**
     * Generates javaDocs for enum's attributes.
     *
     * @param name attribute name
     * @return javaDocs
     */
    private static String generateForEnumAttr(String name) {
        return NEW_LINE + FOUR_SPACE_INDENTATION + JAVA_DOC_FIRST_LINE + FOUR_SPACE_INDENTATION + ENUM_ATTRIBUTE_JAVADOC
                + name + PERIOD + NEW_LINE + FOUR_SPACE_INDENTATION + JAVA_DOC_END_LINE;
    }

    /**
     * Generates javaDocs for rpc method.
     *
     * @param rpcName name of the rpc
     * @param inputName name of input
     * @param outputName name of output
     * @param pluginConfig plugin configurations
     * @return javaDocs of rpc method
     */
    public static String generateJavaDocForRpc(String rpcName, String inputName, String outputName,
            YangPluginConfig pluginConfig) {
        rpcName = getCamelCase(rpcName, pluginConfig.getConflictResolver());

        String javadoc =
                NEW_LINE + FOUR_SPACE_INDENTATION + JAVA_DOC_FIRST_LINE + FOUR_SPACE_INDENTATION + JAVA_DOC_RPC
                        + rpcName + PERIOD + NEW_LINE + FOUR_SPACE_INDENTATION + NEW_LINE_ASTERISK;
        if (!inputName.equals(EMPTY_STRING)) {
            javadoc = javadoc + getInputString(inputName, rpcName);
        }
        if (!outputName.equals(VOID)) {
            javadoc = javadoc + getOutputString(outputName, rpcName);
        }
        return javadoc + FOUR_SPACE_INDENTATION + JAVA_DOC_END_LINE;
    }

    /**
     * Returns output string of rpc.
     *
     * @param outputName name of output
     * @param rpcName name of rpc
     * @return javaDocs for output string of rpc
     */
    private static String getOutputString(String outputName, String rpcName) {
        return FOUR_SPACE_INDENTATION + JAVA_DOC_RETURN + outputName + SPACE + RPC_OUTPUT_STRING + rpcName + NEW_LINE;
    }

    /**
     * Returns input string of rpc.
     *
     * @param inputName name of input
     * @param rpcName name of rpc
     * @return javaDocs for input string of rpc
     */
    private static String getInputString(String inputName, String rpcName) {
        if (inputName.equals("")) {
            return null;
        } else {
            return FOUR_SPACE_INDENTATION + JAVA_DOC_PARAM + inputName + SPACE + RPC_INPUT_STRING + rpcName + NEW_LINE;
        }
    }

    /**
     * Generates javaDoc for the interface.
     *
     * @param interfaceName interface name
     * @return javaDocs
     */
    private static String generateForRpcService(String interfaceName) {
        return NEW_LINE + JAVA_DOC_FIRST_LINE + INTERFACE_JAVA_DOC + interfaceName + PERIOD + NEW_LINE
                + JAVA_DOC_END_LINE;
    }

    /**
     * Generates javaDoc for the event.
     *
     * @param eventClassName event class name
     * @return javaDocs
     */
    private static String generateForEvent(String eventClassName) {
        return NEW_LINE + JAVA_DOC_FIRST_LINE + EVENT_JAVA_DOC + eventClassName + PERIOD + NEW_LINE
                + JAVA_DOC_END_LINE;
    }

    /**
     * Generates javaDoc for the event listener.
     *
     * @param eventListenerInterfaceName event class name
     * @return javaDocs
     */
    private static String generateForEventListener(String eventListenerInterfaceName) {
        return NEW_LINE + JAVA_DOC_FIRST_LINE + EVENT_LISTENER_JAVA_DOC + eventListenerInterfaceName
                + PERIOD + NEW_LINE + JAVA_DOC_END_LINE;
    }

    /**
     * Generates javaDocs for getter method.
     *
     * @param attribute attribute
     * @param isList is list attribute
     * @return javaDocs
     */
    private static String generateForGetters(String attribute, boolean isList) {

        String getter = NEW_LINE + FOUR_SPACE_INDENTATION + JAVA_DOC_FIRST_LINE + FOUR_SPACE_INDENTATION
                + JAVA_DOC_GETTERS + attribute + PERIOD + NEW_LINE + FOUR_SPACE_INDENTATION + NEW_LINE_ASTERISK
                + FOUR_SPACE_INDENTATION + JAVA_DOC_RETURN;
        if (isList) {
            String listAttribute = LIST.toLowerCase() + SPACE + OF + SPACE;
            getter = getter + listAttribute;
        } else {
            getter = getter + VALUE + SPACE + OF + SPACE;
        }

        getter = getter + attribute + NEW_LINE + FOUR_SPACE_INDENTATION + JAVA_DOC_END_LINE;
        return getter;
    }

    /**
     * Generates javaDocs for setter method.
     *
     * @param attribute attribute
     * @param isList is list attribute
     * @return javaDocs
     */
    private static String generateForSetters(String attribute, boolean isList) {

        String setter = NEW_LINE + FOUR_SPACE_INDENTATION + JAVA_DOC_FIRST_LINE + FOUR_SPACE_INDENTATION
                + JAVA_DOC_SETTERS + attribute + PERIOD + NEW_LINE + FOUR_SPACE_INDENTATION + NEW_LINE_ASTERISK
                + FOUR_SPACE_INDENTATION + JAVA_DOC_PARAM + attribute + SPACE;
        if (isList) {
            String listAttribute = LIST.toLowerCase() + SPACE + OF + SPACE;
            setter = setter + listAttribute;
        } else {
            setter = setter + VALUE + SPACE + OF + SPACE;
        }
        setter = setter + attribute + NEW_LINE + FOUR_SPACE_INDENTATION + JAVA_DOC_RETURN + BUILDER_OBJECT
                + attribute
                + NEW_LINE + FOUR_SPACE_INDENTATION + JAVA_DOC_END_LINE;
        return setter;
    }

    /**
     * Generates javaDocs for setter method.
     *
     * @param attribute attribute
     * @param isList is list attribute
     * @return javaDocs
     */
    private static String generateForManagerSetters(String attribute, boolean isList) {

        String setter = NEW_LINE + FOUR_SPACE_INDENTATION + JAVA_DOC_FIRST_LINE + FOUR_SPACE_INDENTATION
                + JAVA_DOC_MANAGER_SETTERS + attribute + PERIOD + NEW_LINE + FOUR_SPACE_INDENTATION + NEW_LINE_ASTERISK
                + FOUR_SPACE_INDENTATION + JAVA_DOC_PARAM + attribute + SPACE;
        if (isList) {
            String listAttribute = LIST.toLowerCase() + SPACE + OF + SPACE;
            setter = setter + listAttribute;
        } else {
            setter = setter + VALUE + SPACE + OF + SPACE;
        }
        setter = setter + attribute
                + NEW_LINE + FOUR_SPACE_INDENTATION + JAVA_DOC_END_LINE;
        return setter;
    }

    /**
     * Generates javaDocs for of method.
     *
     * @param attribute attribute
     * @return javaDocs
     */
    private static String generateForOf(String attribute) {
        return NEW_LINE + FOUR_SPACE_INDENTATION + JAVA_DOC_FIRST_LINE + FOUR_SPACE_INDENTATION + JAVA_DOC_OF
                + attribute + PERIOD + NEW_LINE + FOUR_SPACE_INDENTATION + NEW_LINE_ASTERISK + FOUR_SPACE_INDENTATION
                + JAVA_DOC_PARAM + VALUE + SPACE + VALUE + SPACE + OF + SPACE + attribute + NEW_LINE
                + FOUR_SPACE_INDENTATION + JAVA_DOC_RETURN + OBJECT + SPACE + OF + SPACE + attribute + NEW_LINE
                + FOUR_SPACE_INDENTATION + JAVA_DOC_END_LINE;
    }

    /**
     * Generates javaDocs for from method.
     *
     * @param attribute attribute
     * @return javaDocs
     */
    private static String generateForFromString(String attribute) {

        return NEW_LINE + FOUR_SPACE_INDENTATION + JAVA_DOC_FIRST_LINE + FOUR_SPACE_INDENTATION + JAVA_DOC_OF
                + attribute + SPACE + FROM_STRING_METHOD_NAME + SPACE + INPUT + SPACE + STRING_DATA_TYPE + PERIOD
                + NEW_LINE + FOUR_SPACE_INDENTATION + NEW_LINE_ASTERISK + FOUR_SPACE_INDENTATION + JAVA_DOC_PARAM
                + FROM_STRING_PARAM_NAME + SPACE + INPUT + SPACE + STRING_DATA_TYPE + NEW_LINE
                + FOUR_SPACE_INDENTATION + JAVA_DOC_RETURN + OBJECT + SPACE + OF + SPACE + attribute + NEW_LINE
                + FOUR_SPACE_INDENTATION + JAVA_DOC_END_LINE;
    }

    /**
     * Generates javaDocs for typedef setter method.
     *
     * @param attribute attribute
     * @return javaDocs
     */
    private static String generateForTypeDefSetter(String attribute) {
        return NEW_LINE + FOUR_SPACE_INDENTATION + JAVA_DOC_FIRST_LINE + FOUR_SPACE_INDENTATION
                + JAVA_DOC_SETTERS_COMMON + attribute + PERIOD + NEW_LINE + FOUR_SPACE_INDENTATION + NEW_LINE_ASTERISK
                + FOUR_SPACE_INDENTATION + JAVA_DOC_PARAM + VALUE + SPACE + VALUE + SPACE + OF + SPACE + attribute
                + NEW_LINE + FOUR_SPACE_INDENTATION + JAVA_DOC_END_LINE;
    }

    /**
     * Generates javaDocs for the impl class.
     *
     * @param className class name
     * @return javaDocs
     */
    private static String generateForClass(String className) {
        return NEW_LINE + JAVA_DOC_FIRST_LINE + IMPL_CLASS_JAVA_DOC + className + PERIOD + NEW_LINE + JAVA_DOC_END_LINE;
    }

    /**
     * Generates javaDocs for enum.
     *
     * @param className enum class name
     * @return javaDocs
     */
    private static String generateForEnum(String className) {
        return NEW_LINE + NEW_LINE + JAVA_DOC_FIRST_LINE + ENUM_CLASS_JAVADOC + className + PERIOD + NEW_LINE
                + JAVA_DOC_END_LINE;
    }

    /**
     * Generates javaDocs for the builder class.
     *
     * @param className class name
     * @return javaDocs
     */
    private static String generateForBuilderClass(String className) {
        return NEW_LINE + JAVA_DOC_FIRST_LINE + BUILDER_CLASS_JAVA_DOC + className + PERIOD + NEW_LINE
                + JAVA_DOC_END_LINE;
    }

    /**
     * Generates javaDoc for the interface.
     *
     * @param interfaceName interface name
     * @return javaDocs
     */
    private static String generateForInterface(String interfaceName) {
        return NEW_LINE + JAVA_DOC_FIRST_LINE + INTERFACE_JAVA_DOC + interfaceName + PERIOD + NEW_LINE
                + JAVA_DOC_END_LINE;
    }

    /**
     * Generates javaDoc for the builder interface.
     *
     * @param builderforName builder for name
     * @return javaDocs
     */
    private static String generateForBuilderInterface(String builderforName) {
        return JAVA_DOC_FIRST_LINE + BUILDER_INTERFACE_JAVA_DOC + builderforName + PERIOD + NEW_LINE
                + JAVA_DOC_END_LINE;
    }

    /**
     * Generates javaDocs for package-info.
     *
     * @param packageName package name
     * @param isChildNode is it child node
     * @return javaDocs
     */
    private static String generateForPackage(String packageName, boolean isChildNode) {
        String javaDoc = JAVA_DOC_FIRST_LINE + PACKAGE_INFO_JAVADOC + packageName;
        if (isChildNode) {
            javaDoc = javaDoc + PACKAGE_INFO_JAVADOC_OF_CHILD;
        }
        return javaDoc + PERIOD + NEW_LINE + JAVA_DOC_END_LINE;
    }

    /**
     * Generates javaDocs for default constructor.
     *
     * @param className class name
     * @return javaDocs
     */
    private static String generateForDefaultConstructors(String className) {
        return FOUR_SPACE_INDENTATION + JAVA_DOC_FIRST_LINE + FOUR_SPACE_INDENTATION + JAVA_DOC_CONSTRUCTOR + className
                + PERIOD + NEW_LINE + FOUR_SPACE_INDENTATION + JAVA_DOC_END_LINE;
    }

    /**
     * Generates javaDocs for constructor with parameters.
     *
     * @param className class name
     * @return javaDocs
     */
    private static String generateForConstructors(String className) {
        return NEW_LINE + FOUR_SPACE_INDENTATION + JAVA_DOC_FIRST_LINE + FOUR_SPACE_INDENTATION + JAVA_DOC_CONSTRUCTOR
                + className + IMPL + PERIOD + NEW_LINE + FOUR_SPACE_INDENTATION + NEW_LINE_ASTERISK
                + FOUR_SPACE_INDENTATION + JAVA_DOC_PARAM + BUILDER.toLowerCase() + OBJECT + SPACE + BUILDER_OBJECT
                + className + NEW_LINE + FOUR_SPACE_INDENTATION + JAVA_DOC_END_LINE;
    }

    /**
     * Generates javaDocs for build.
     *
     * @param buildName builder name
     * @return javaDocs
     */
    private static String generateForBuild(String buildName) {
        return NEW_LINE + FOUR_SPACE_INDENTATION + JAVA_DOC_FIRST_LINE + FOUR_SPACE_INDENTATION + JAVA_DOC_BUILD
                + buildName + PERIOD + NEW_LINE + FOUR_SPACE_INDENTATION + NEW_LINE_ASTERISK + FOUR_SPACE_INDENTATION
                + JAVA_DOC_RETURN + JAVA_DOC_BUILD_RETURN + buildName + PERIOD + NEW_LINE + FOUR_SPACE_INDENTATION
                + JAVA_DOC_END_LINE;
    }

    /**
     * Generates javaDocs for type constructor.
     *
     * @param attribute attribute string
     * @return javaDocs for type constructor
     */
    private static String generateForTypeConstructor(String attribute) {
        return NEW_LINE + FOUR_SPACE_INDENTATION + JAVA_DOC_FIRST_LINE + FOUR_SPACE_INDENTATION + JAVA_DOC_CONSTRUCTOR
                + attribute + PERIOD + NEW_LINE + FOUR_SPACE_INDENTATION + NEW_LINE_ASTERISK + FOUR_SPACE_INDENTATION
                + JAVA_DOC_PARAM + VALUE + SPACE + VALUE + SPACE + OF + SPACE + attribute + NEW_LINE
                + FOUR_SPACE_INDENTATION + JAVA_DOC_END_LINE;
    }
}
