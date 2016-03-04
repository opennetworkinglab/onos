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
import static org.junit.Assert.assertNotNull;
import static org.hamcrest.core.Is.is;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

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
     * Unit test for private constructor.
     *
     * @throws SecurityException if any security violation is observed
     * @throws NoSuchMethodException if when the method is not found
     * @throws IllegalArgumentException if there is illegal argument found
     * @throws InstantiationException if instantiation is provoked for the private constructor
     * @throws IllegalAccessException if instance is provoked or a method is provoked
     * @throws InvocationTargetException when an exception occurs by the method or constructor
     */
    @Test
    public void callPrivateConstructors() throws SecurityException, NoSuchMethodException, IllegalArgumentException,
            InstantiationException, IllegalAccessException, InvocationTargetException {
        Class<?>[] classesToConstruct = {JavaCodeSnippetGen.class };
        for (Class<?> clazz : classesToConstruct) {
            Constructor<?> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            assertNotNull(constructor.newInstance());
        }
    }

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
     * Unit test case for list attribute.
     */
    @Test
    public void testForListAttribute() {
        String listAttribute = JavaCodeSnippetGen.getListAttribute(STRING);
        assertThat(true, is(listAttribute.equals(UtilConstants.LIST + UtilConstants.DIAMOND_OPEN_BRACKET + STRING
                + UtilConstants.DIAMOND_CLOSE_BRACKET)));
    }

    /**
     * Unit test case for java class interface definition close.
     */
    @Test
    public void testForJavaClassDefInterfaceClose() {
        String interfaceDef = JavaCodeSnippetGen.getJavaClassDefClose(FILE_GEN_TYPE, YANG_NAME);
        assertThat(true, is(interfaceDef.equals(UtilConstants.CLOSE_CURLY_BRACKET)));
    }

    /**
     * Unit test case for java class builder class definition close.
     */
    @Test
    public void testForJavaClassDefBuilderClassClose() {
        String builderClassDef = JavaCodeSnippetGen.getJavaClassDefClose(GeneratedFileType.BUILDER_CLASS_MASK,
                YANG_NAME);
        assertThat(true, is(builderClassDef.equals(UtilConstants.CLOSE_CURLY_BRACKET)));
    }

    /**
     * Unit test case for java class typedef definition close.
     */
    @Test
    public void testForJavaClassDefTypeDefClose() {
        String typeDef = JavaCodeSnippetGen.getJavaClassDefClose(GeneratedFileType.GENERATE_TYPEDEF_CLASS, YANG_NAME);
        assertThat(true, is(typeDef.equals(UtilConstants.CLOSE_CURLY_BRACKET)));
    }

    /**
     * Unit test case for java attribute info.
     */
    @SuppressWarnings("rawtypes")
    @Test
    public void testForJavaAttributeInfo() {

        String attributeWithoutTypePkg = JavaCodeSnippetGen.getJavaAttributeDefination(null, "String", YANG_NAME,
                false);
        assertThat(true, is(attributeWithoutTypePkg.equals(UtilConstants.PRIVATE + UtilConstants.SPACE + "String"
                + UtilConstants.SPACE + YANG_NAME + UtilConstants.SEMI_COLAN)));
        String attributeWithTypePkg = JavaCodeSnippetGen.getJavaAttributeDefination("java.lang", "String", YANG_NAME,
                false);
        assertThat(true, is(attributeWithTypePkg.equals(UtilConstants.PRIVATE + UtilConstants.SPACE + "java.lang."
                + "String" + UtilConstants.SPACE + YANG_NAME + UtilConstants.SEMI_COLAN)));
        String attributeWithListPkg = JavaCodeSnippetGen.getJavaAttributeDefination("java.lang", "String", YANG_NAME,
                true);
        assertThat(true,
                is(attributeWithListPkg.equals(UtilConstants.PRIVATE + UtilConstants.SPACE + UtilConstants.LIST
                        + UtilConstants.DIAMOND_OPEN_BRACKET + "java.lang."
                        + "String" + UtilConstants.DIAMOND_CLOSE_BRACKET + UtilConstants.SPACE + YANG_NAME
                        + UtilConstants.SEMI_COLAN)));
        String attributeWithListWithoutPkg = JavaCodeSnippetGen.getJavaAttributeDefination(null, "String", YANG_NAME,
                true);
        assertThat(true,
                is(attributeWithListWithoutPkg.equals(UtilConstants.PRIVATE + UtilConstants.SPACE + UtilConstants.LIST
                        + UtilConstants.DIAMOND_OPEN_BRACKET + "String"
                        + UtilConstants.DIAMOND_CLOSE_BRACKET + UtilConstants.SPACE + YANG_NAME
                        + UtilConstants.SEMI_COLAN)));
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
