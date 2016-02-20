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

import java.util.List;
import java.util.SortedSet;

import org.onosproject.yangutils.datamodel.YangType;
import org.onosproject.yangutils.translator.GeneratedFileType;
import org.onosproject.yangutils.translator.tojava.GeneratedMethodTypes;
import org.onosproject.yangutils.translator.tojava.ImportInfo;
import org.onosproject.yangutils.utils.UtilConstants;

/**
 * Utility class to generate the java snippet.
 */
public final class JavaCodeSnippetGen {

    /**
     * Default constructor.
     */
    private JavaCodeSnippetGen() {
    }

    /**
     * Get the java file header comment.
     *
     * @return the java file header comment.
     */
    public static String getFileHeaderComment() {

        /**
         * TODO return the file header.
         */
        return null;
    }

    /**
     * reorder the import list based on the ONOS import rules.
     *
     * @param importInfo the set of classes/interfaces to be imported.
     * @return string of import info.
     */
    public List<ImportInfo> sortImportOrder(SortedSet<ImportInfo> importInfo) {
        /* TODO: reorder the import list based on the ONOS import rules. */
        return null;
    }

    /**
     * Get the textual java code information corresponding to the import list.
     *
     * @param importInfo sorted list of import info.
     * @return the textual java code information corresponding to the import
     *         list.
     */
    public static String getImportText(List<ImportInfo> importInfo) {
        /*
         * TODO: get the textual java code information corresponding to the
         * import list
         */
        return null;
    }

    /**
     * Based on the file type and the YANG name of the file, generate the class
     * / interface definition start.
     *
     * @param genFileTypes type of file being generated.
     * @param yangName YANG name.
     * @return corresponding textual java code information.
     */
    public static String getJavaClassDefStart(GeneratedFileType genFileTypes, String yangName) {
        /*
         * get the camel case name for java class / interface.
         */
        yangName = JavaIdentifierSyntax.getCamelCase(yangName);
        return ClassDefinitionGenerator.generateClassDefinition(genFileTypes, yangName);
    }

    /**
     * Get the textual java code for attribute definition in class.
     *
     * @param genFileTypes type of file being generated.
     * @param yangName YANG name of the the attribute.
     * @param type type of the the attribute.
     * @return the textual java code for attribute definition in class.
     */
    public static String getJavaAttributeInfo(GeneratedFileType genFileTypes, String yangName, YangType<?> type) {
        yangName = JavaIdentifierSyntax.getCamelCase(yangName);
        return UtilConstants.PRIVATE + UtilConstants.SPACE + type.getDataTypeName() + UtilConstants.SPACE + yangName
                + UtilConstants.SEMI_COLAN;
    }

    /**
     * Based on the file type and method type(s) and the YANG name of the
     * method, generate the method definitions(s).
     *
     * @param genFileTypes type of file being generated
     * @param yangName name if the attribute whose getter / setter is required.
     * @param methodTypes getter and / or setter type of method indicator.
     * @param returnType type return type of the method.
     * @return based on the file type and method type(s) the method
     *         definitions(s).
     */
    public static String getJavaMethodInfo(GeneratedFileType genFileTypes, String yangName,
            GeneratedMethodTypes methodTypes, YangType<?> returnType) {

        yangName = JavaIdentifierSyntax.getCamelCase(yangName);
        return MethodsGenerator.constructMethodInfo(genFileTypes, yangName, methodTypes, returnType);
    }

    /**
     * Based on the file type and the YANG name of the file, generate the class
     * / interface definition close.
     *
     * @param genFileTypes type of file being generated.
     * @param yangName YANG name.
     * @return corresponding textual java code information.
     */
    public static String getJavaClassDefClose(GeneratedFileType genFileTypes, String yangName) {

        if (genFileTypes.equals(GeneratedFileType.INTERFACE)) {

            return UtilConstants.CLOSE_CURLY_BRACKET;
        } else if (genFileTypes.equals(GeneratedFileType.BUILDER_CLASS)) {

            return UtilConstants.CLOSE_CURLY_BRACKET;
        }
        return null;
    }

}
