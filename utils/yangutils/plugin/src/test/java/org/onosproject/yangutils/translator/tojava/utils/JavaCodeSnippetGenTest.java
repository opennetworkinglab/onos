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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.junit.Test;
import org.onosproject.yangutils.translator.tojava.JavaQualifiedTypeInfo;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.onosproject.yangutils.translator.tojava.utils.JavaCodeSnippetGen.getImportText;
import static org.onosproject.yangutils.translator.tojava.utils.JavaCodeSnippetGen.getJavaAttributeDefination;
import static org.onosproject.yangutils.translator.tojava.utils.JavaCodeSnippetGen.getJavaClassDefClose;
import static org.onosproject.yangutils.utils.UtilConstants.CLOSE_CURLY_BRACKET;
import static org.onosproject.yangutils.utils.UtilConstants.DIAMOND_CLOSE_BRACKET;
import static org.onosproject.yangutils.utils.UtilConstants.DIAMOND_OPEN_BRACKET;
import static org.onosproject.yangutils.utils.UtilConstants.IMPORT;
import static org.onosproject.yangutils.utils.UtilConstants.JAVA_LANG;
import static org.onosproject.yangutils.utils.UtilConstants.LIST;
import static org.onosproject.yangutils.utils.UtilConstants.NEW_LINE;
import static org.onosproject.yangutils.utils.UtilConstants.PERIOD;
import static org.onosproject.yangutils.utils.UtilConstants.PRIVATE;
import static org.onosproject.yangutils.utils.UtilConstants.SEMI_COLAN;
import static org.onosproject.yangutils.utils.UtilConstants.SPACE;
import static org.onosproject.yangutils.utils.UtilConstants.STRING_DATA_TYPE;

/**
 * Unit test cases for java code snippet generator.
 */
public class JavaCodeSnippetGenTest {

    private static final String PKG_INFO = "org.onosproject.unittest";
    private static final String CLASS_INFO = "JavaCodeSnippetGenTest";
    private static final String YANG_NAME = "Test";

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
    public void callPrivateConstructors()
            throws SecurityException, NoSuchMethodException, IllegalArgumentException,
            InstantiationException, IllegalAccessException, InvocationTargetException {

        Class<?>[] classesToConstruct = {JavaCodeSnippetGen.class };
        for (Class<?> clazz : classesToConstruct) {
            Constructor<?> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            assertThat(null, not(constructor.newInstance()));
        }
    }

    /**
     * Unit test case for import text.
     */
    @Test
    public void testForImportText() {
        JavaQualifiedTypeInfo importInfo = new JavaQualifiedTypeInfo();
        importInfo.setPkgInfo(PKG_INFO);
        importInfo.setClassInfo(CLASS_INFO);

        String imports = getImportText(importInfo);

        assertThat(true, is(imports.equals(IMPORT + PKG_INFO + PERIOD + CLASS_INFO + SEMI_COLAN + NEW_LINE)));
    }

    /**
     * Unit test case for java class interface definition close.
     */
    @Test
    public void testForJavaClassDefClose() {
        String interfaceDef = getJavaClassDefClose();
        assertThat(true, is(interfaceDef.equals(CLOSE_CURLY_BRACKET)));
    }

    /**
     * Unit test case for java attribute info.
     */
    @Test
    public void testForJavaAttributeInfo() {

        String attributeWithoutTypePkg = getJavaAttributeDefination(null, STRING_DATA_TYPE, YANG_NAME, false);
        assertThat(true, is(attributeWithoutTypePkg.equals(
                PRIVATE + SPACE + STRING_DATA_TYPE + SPACE + YANG_NAME + SEMI_COLAN + NEW_LINE)));

        String attributeWithTypePkg = getJavaAttributeDefination(JAVA_LANG, STRING_DATA_TYPE, YANG_NAME, false);
        assertThat(true, is(attributeWithTypePkg.equals(PRIVATE + SPACE + JAVA_LANG + PERIOD
                + STRING_DATA_TYPE + SPACE + YANG_NAME + SEMI_COLAN + NEW_LINE)));

        String attributeWithListPkg = getJavaAttributeDefination(JAVA_LANG, STRING_DATA_TYPE, YANG_NAME, true);
        assertThat(true, is(attributeWithListPkg.equals(
                PRIVATE + SPACE + LIST + DIAMOND_OPEN_BRACKET + JAVA_LANG + PERIOD + STRING_DATA_TYPE
                        + DIAMOND_CLOSE_BRACKET + SPACE + YANG_NAME + SEMI_COLAN + NEW_LINE)));

        String attributeWithListWithoutPkg = getJavaAttributeDefination(null, STRING_DATA_TYPE, YANG_NAME, true);
        assertThat(true, is(attributeWithListWithoutPkg.equals(
                PRIVATE + SPACE + LIST + DIAMOND_OPEN_BRACKET + STRING_DATA_TYPE + DIAMOND_CLOSE_BRACKET + SPACE
                        + YANG_NAME + SEMI_COLAN + NEW_LINE)));
    }
}
