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

import org.junit.Test;
import org.onosproject.yangutils.datamodel.YangDataTypes;
import org.onosproject.yangutils.datamodel.YangType;
import org.onosproject.yangutils.translator.GeneratedFileType;
import org.onosproject.yangutils.translator.tojava.GeneratedMethodTypes;
import org.onosproject.yangutils.translator.tojava.ImportInfo;
import org.onosproject.yangutils.utils.UtilConstants;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Unit test cases for java code snippet generator.
 */
public class JavaCodeSnippetGenTest {

    private static final String PKG_INFO = "org.onosproject.unittest";
    private static final String CLASS_INFO = "JavaCodeSnippetGenTest";
    private static final int FILE_GEN_TYPE = GeneratedFileType.INTERFACE_MASK;
    private static final GeneratedMethodTypes METHOD_GEN_TYPE = GeneratedMethodTypes.GETTER;
    private static final String YANG_NAME = "Test";
    private static final String STRING = "String";

    /**
     * Unit test case for import text.
     */
    @Test
    public void testForImportText() {
        ImportInfo importInfo = new ImportInfo();
        importInfo.setPkgInfo(PKG_INFO);
        importInfo.setClassInfo(CLASS_INFO);

        String imports = JavaCodeSnippetGen.getImportText(importInfo);

        assertThat(true, is(imports.equals(UtilConstants.IMPORT + PKG_INFO + UtilConstants.PERIOD + CLASS_INFO
                + UtilConstants.SEMI_COLAN + UtilConstants.NEW_LINE)));
    }

    /**
     * Unit test case for java class definition start.
     */
    @Test
    public void testForJavaClassDefStart() {
        String classDef = JavaCodeSnippetGen.getJavaClassDefStart(FILE_GEN_TYPE, YANG_NAME);
        assertThat(true,
                is(classDef.equals(UtilConstants.PUBLIC + UtilConstants.SPACE + UtilConstants.INTERFACE
                        + UtilConstants.SPACE + YANG_NAME + UtilConstants.SPACE + UtilConstants.OPEN_CURLY_BRACKET
                        + UtilConstants.NEW_LINE)));
    }

    /**
     * Unit test case for java attribute info.
     */
    @SuppressWarnings("rawtypes")
    @Test
    public void testForJavaAttributeInfo() {
        // TODO: need to update for new framework
        //        String attributeWithType
        //        = JavaCodeSnippetGen.getJavaAttributeDefination(FILE_GEN_TYPE, YANG_NAME, getType());
        //        assertThat(true, is(attributeWithType.equals(UtilConstants.PRIVATE + UtilConstants.SPACE
        //                + getType().getDataTypeName() + UtilConstants.SPACE + YANG_NAME + UtilConstants.SEMI_COLAN)));
        //
        //        String attributeWithoutType
        //        = JavaCodeSnippetGen.getJavaAttributeDefination(FILE_GEN_TYPE, YANG_NAME, null);
        //        assertThat(true,
        //                is(attributeWithoutType.equals(
        //                        UtilConstants.PRIVATE
        //        + UtilConstants.SPACE + JavaIdentifierSyntax.getCaptialCase(YANG_NAME)
        //                                + UtilConstants.SPACE + YANG_NAME + UtilConstants.SEMI_COLAN)));

    }

    /**
     * Unit test case for list attribute.
     */
    @Test
    public void testForListAttribute() {
        String listAttribute = JavaCodeSnippetGen.getListAttribute(STRING);
        assertThat(true, is(listAttribute.equals(UtilConstants.LIST + UtilConstants.DIAMOND_OPEN_BRACKET + STRING
                + UtilConstants.DIAMOND_CLOSE_BRACKET)));
    }

    /**
     * Unit test case for java method info.
     */
    @Test
    public void testForJavaMethodInfo() {
        //TODO: update to new framework.
        //        String method
        //        = JavaCodeSnippetGen.getJavaMethodInfo(FILE_GEN_TYPE, YANG_NAME, METHOD_GEN_TYPE, getType());
        //        assertThat(true,
        //                is(method.equals(UtilConstants.FOUR_SPACE_INDENTATION
        //                        + JavaIdentifierSyntax.getCaptialCase(getType().getDataTypeName())
        //        + UtilConstants.SPACE
        //                        + UtilConstants.GET_METHOD_PREFIX + JavaIdentifierSyntax.getCaptialCase(YANG_NAME)
        //                        + UtilConstants.OPEN_PARENTHESIS + UtilConstants.CLOSE_PARENTHESIS
        //                        + UtilConstants.SEMI_COLAN)));
    }

    /**
     * Unit test case for java class definition close.
     */
    @Test
    public void testForJavaClassDefClose() {
        String classDef = JavaCodeSnippetGen.getJavaClassDefClose(FILE_GEN_TYPE, YANG_NAME);
        assertThat(true, is(classDef.equals(UtilConstants.CLOSE_CURLY_BRACKET)));
    }

    /**
     * Returns YANG type.
     *
     * @return type
     */
    @SuppressWarnings("rawtypes")
    private YangType<?> getType() {
        YangType<?> type = new YangType();
        type.setDataTypeName(STRING);
        type.setDataType(YangDataTypes.STRING);
        return type;
    }
}
