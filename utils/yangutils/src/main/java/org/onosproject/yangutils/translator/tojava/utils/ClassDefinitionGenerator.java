/*
 * Copyright 2016 Open Networking Laboratory
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
import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.GENERATE_TYPEDEF_CLASS;
import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.IMPL_CLASS_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.INTERFACE_MASK;
import static org.onosproject.yangutils.utils.UtilConstants.BUILDER;
import static org.onosproject.yangutils.utils.UtilConstants.CLASS;
import static org.onosproject.yangutils.utils.UtilConstants.FINAL;
import static org.onosproject.yangutils.utils.UtilConstants.IMPL;
import static org.onosproject.yangutils.utils.UtilConstants.IMPLEMENTS;
import static org.onosproject.yangutils.utils.UtilConstants.INTERFACE;
import static org.onosproject.yangutils.utils.UtilConstants.NEW_LINE;
import static org.onosproject.yangutils.utils.UtilConstants.OPEN_CURLY_BRACKET;
import static org.onosproject.yangutils.utils.UtilConstants.PERIOD;
import static org.onosproject.yangutils.utils.UtilConstants.PUBLIC;
import static org.onosproject.yangutils.utils.UtilConstants.SPACE;

/**
 * Generates class definition for generated files.
 */
public final class ClassDefinitionGenerator {

    /**
     * Default constructor.
     */
    private ClassDefinitionGenerator() {
    }

    /**
     * Based on the file type and the YANG name of the file, generate the class
     * / interface definition start.
     *
     * @param genFileTypes generated file type
     * @param yangName class name
     * @return class definition
     */
    public static String generateClassDefinition(int genFileTypes, String yangName) {

        /**
         * based on the file type and the YANG name of the file, generate the
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

            return getTypeDefClassDefinition(yangName);
        }
        return null;
    }

    /**
     * Returns interface file class definition.
     *
     * @param yangName file name
     * @return definition
     */
    private static String getInterfaceDefinition(String yangName) {

        return PUBLIC + SPACE + INTERFACE + SPACE + yangName + SPACE + OPEN_CURLY_BRACKET + NEW_LINE;
    }

    /**
     * Returns builder interface file class definition.
     *
     * @param yangName java class name, corresponding to which the builder class
     *            is being generated
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
     * Returns typeDef file class definition.
     *
     * @param yangName file name
     * @return definition
     */
    private static String getTypeDefClassDefinition(String yangName) {

        return PUBLIC + SPACE + FINAL + SPACE + CLASS + SPACE + yangName + SPACE + OPEN_CURLY_BRACKET + NEW_LINE;
    }
}
