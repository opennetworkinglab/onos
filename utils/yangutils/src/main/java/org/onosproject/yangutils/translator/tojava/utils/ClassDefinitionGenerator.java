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

import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.BUILDER_CLASS_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.BUILDER_INTERFACE_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.GENERATE_ENUM_CLASS;
import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.GENERATE_RPC_INTERFACE;
import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.GENERATE_TYPEDEF_CLASS;
import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.GENERATE_UNION_CLASS;
import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.IMPL_CLASS_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.INTERFACE_MASK;
import static org.onosproject.yangutils.translator.tojava.utils.JavaFileGenerator.getExtendsList;
import static org.onosproject.yangutils.translator.tojava.utils.JavaFileGenerator.isExtendsList;
import static org.onosproject.yangutils.utils.UtilConstants.BUILDER;
import static org.onosproject.yangutils.utils.UtilConstants.CLASS;
import static org.onosproject.yangutils.utils.UtilConstants.COMMA;
import static org.onosproject.yangutils.utils.UtilConstants.ENUM;
import static org.onosproject.yangutils.utils.UtilConstants.EXTEND;
import static org.onosproject.yangutils.utils.UtilConstants.FINAL;
import static org.onosproject.yangutils.utils.UtilConstants.IMPL;
import static org.onosproject.yangutils.utils.UtilConstants.IMPLEMENTS;
import static org.onosproject.yangutils.utils.UtilConstants.INTERFACE;
import static org.onosproject.yangutils.utils.UtilConstants.NEW_LINE;
import static org.onosproject.yangutils.utils.UtilConstants.OPEN_CURLY_BRACKET;
import static org.onosproject.yangutils.utils.UtilConstants.PERIOD;
import static org.onosproject.yangutils.utils.UtilConstants.PUBLIC;
import static org.onosproject.yangutils.utils.UtilConstants.SPACE;
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
        if ((genFileTypes & INTERFACE_MASK) != 0) {
            return getInterfaceDefinition(yangName);
        } else if ((genFileTypes & BUILDER_CLASS_MASK) != 0) {
            return getBuilderClassDefinition(yangName);
        } else if ((genFileTypes & IMPL_CLASS_MASK) != 0) {
            return getImplClassDefinition(yangName);
        } else if ((genFileTypes & BUILDER_INTERFACE_MASK) != 0) {
            return getBuilderInterfaceDefinition(yangName);
        } else if ((genFileTypes & GENERATE_TYPEDEF_CLASS) != 0) {
            return getTypeClassDefinition(yangName);
        } else if ((genFileTypes & GENERATE_UNION_CLASS) != 0) {
            return getTypeClassDefinition(yangName);
        } else if ((genFileTypes & GENERATE_ENUM_CLASS) != 0) {
            return getEnumClassDefinition(yangName);
        } else if ((genFileTypes & GENERATE_RPC_INTERFACE) != 0) {
            return getRpcInterfaceDefinition(yangName);
        }
        return null;
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
    private static String getInterfaceDefinition(String yangName) {
        if (!isExtendsList()) {
            return PUBLIC + SPACE + INTERFACE + SPACE + yangName + SPACE + OPEN_CURLY_BRACKET + NEW_LINE;
        }
        String def = PUBLIC + SPACE + INTERFACE + SPACE + yangName + SPACE + EXTEND + SPACE;
        for (String extend : getExtendsList()) {
            def = def + extend + COMMA + SPACE;
        }
        def = trimAtLast(def, COMMA);

        return def + SPACE + OPEN_CURLY_BRACKET + NEW_LINE;
    }

    /**
     * Returns builder interface file class definition.
     *
     * @param yangName java class name, corresponding to which the builder class
     *                 is being generated
     * @return definition
     */
    private static String getBuilderInterfaceDefinition(String yangName) {
        return INTERFACE + SPACE + yangName + BUILDER + SPACE + OPEN_CURLY_BRACKET + NEW_LINE + NEW_LINE;
    }

    /**
     * Returns builder file class definition.
     *
     * @param yangName file name
     * @return definition
     */
    private static String getBuilderClassDefinition(String yangName) {
        return PUBLIC + SPACE + CLASS + SPACE + yangName + BUILDER + SPACE + IMPLEMENTS + SPACE + yangName + PERIOD
                + yangName + BUILDER + SPACE + OPEN_CURLY_BRACKET + NEW_LINE;
    }

    /**
     * Returns impl file class definition.
     *
     * @param yangName file name
     * @return definition
     */
    private static String getImplClassDefinition(String yangName) {
        return PUBLIC + SPACE + FINAL + SPACE + CLASS + SPACE + yangName + IMPL + SPACE + IMPLEMENTS + SPACE + yangName
                + SPACE + OPEN_CURLY_BRACKET + NEW_LINE;
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
     * Returns rpc file interface definition.
     *
     * @param yangName file name
     * @return definition
     */
    private static String getRpcInterfaceDefinition(String yangName) {
        return INTERFACE + SPACE + yangName + SPACE + OPEN_CURLY_BRACKET + NEW_LINE;
    }
}
