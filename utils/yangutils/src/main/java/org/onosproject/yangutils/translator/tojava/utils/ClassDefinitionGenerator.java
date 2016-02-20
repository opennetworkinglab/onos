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

import org.onosproject.yangutils.translator.GeneratedFileType;
import org.onosproject.yangutils.utils.UtilConstants;

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
     * Generate class definition for specific classes.
     *
     * @param genFileTypes generated file type
     * @param yangName class name
     * @return class definition
     */
    public static String generateClassDefinition(GeneratedFileType genFileTypes, String yangName) {

        /**
         * based on the file type and the YANG name of the file, generate
         * the class / interface definition start.
         */
        if (genFileTypes.equals(GeneratedFileType.INTERFACE)) {

            return getInterfaceDefinition(yangName);
        } else if (genFileTypes.equals(GeneratedFileType.BUILDER_CLASS)) {

            return getBuilderClassDefinition(yangName);
        } else if (genFileTypes.equals(GeneratedFileType.IMPL)) {

            return getImplClassDefinition(yangName);
        } else if (genFileTypes.equals(GeneratedFileType.BUILDER_INTERFACE)) {

            return getBuilderInterfaceDefinition();
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

        return UtilConstants.PUBLIC + UtilConstants.SPACE + UtilConstants.INTERFACE + UtilConstants.SPACE + yangName
                + UtilConstants.SPACE + UtilConstants.OPEN_CURLY_BRACKET + UtilConstants.NEW_LINE;
    }

    /**
     * Returns builder interface file class definition.
     *
     * @return definition
     */
    private static String getBuilderInterfaceDefinition() {
        return UtilConstants.INTERFACE + UtilConstants.SPACE + UtilConstants.BUILDER + UtilConstants.SPACE
                + UtilConstants.OPEN_CURLY_BRACKET + UtilConstants.NEW_LINE;
    }

    /**
     * Returns builder file class definition.
     *
     * @param yangName file name
     * @return definition
     */
    private static String getBuilderClassDefinition(String yangName) {

        return UtilConstants.PUBLIC + UtilConstants.SPACE + UtilConstants.CLASS + UtilConstants.SPACE + yangName
                + UtilConstants.BUILDER + UtilConstants.SPACE + UtilConstants.IMPLEMENTS + UtilConstants.SPACE
                + yangName + UtilConstants.PERIOD + UtilConstants.BUILDER + UtilConstants.SPACE
                + UtilConstants.OPEN_CURLY_BRACKET + UtilConstants.NEW_LINE;
    }

    /**
     * Returns impl file class definition.
     *
     * @param yangName file name
     * @return definition
     */
    private static String getImplClassDefinition(String yangName) {

        return UtilConstants.PUBLIC + UtilConstants.SPACE + UtilConstants.FINAL + UtilConstants.SPACE
                + UtilConstants.CLASS + UtilConstants.SPACE + yangName + UtilConstants.IMPL + UtilConstants.SPACE
                + UtilConstants.IMPLEMENTS + UtilConstants.SPACE + yangName + UtilConstants.OPEN_CURLY_BRACKET
                + UtilConstants.SPACE + UtilConstants.NEW_LINE;
    }

}
