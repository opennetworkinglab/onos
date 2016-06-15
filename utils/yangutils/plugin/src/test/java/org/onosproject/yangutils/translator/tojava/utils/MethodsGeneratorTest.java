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
import org.onosproject.yangutils.datamodel.YangType;
import org.onosproject.yangutils.translator.tojava.JavaAttributeInfo;
import org.onosproject.yangutils.translator.tojava.JavaQualifiedTypeInfo;
import org.onosproject.yangutils.utils.io.impl.YangPluginConfig;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;
import static org.onosproject.yangutils.datamodel.YangDataTypes.STRING;
import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.GENERATE_SERVICE_AND_MANAGER;
import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.getCapitalCase;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getBuild;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getBuildForInterface;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getCheckNotNull;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getConstructor;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getConstructorStart;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getEqualsMethod;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getGetterForClass;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getGetterForInterface;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getOfMethod;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getOverRideString;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getSetterForClass;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getSetterForInterface;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getSetterForTypeDefClass;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getToStringMethod;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getTypeConstructorStringAndJavaDoc;
import static org.onosproject.yangutils.utils.UtilConstants.ADD_STRING;
import static org.onosproject.yangutils.utils.UtilConstants.BUILD;
import static org.onosproject.yangutils.utils.UtilConstants.BUILDER;
import static org.onosproject.yangutils.utils.UtilConstants.CHECK_NOT_NULL_STRING;
import static org.onosproject.yangutils.utils.UtilConstants.CLOSE_CURLY_BRACKET;
import static org.onosproject.yangutils.utils.UtilConstants.CLOSE_PARENTHESIS;
import static org.onosproject.yangutils.utils.UtilConstants.COMMA;
import static org.onosproject.yangutils.utils.UtilConstants.EIGHT_SPACE_INDENTATION;
import static org.onosproject.yangutils.utils.UtilConstants.EQUAL;
import static org.onosproject.yangutils.utils.UtilConstants.EQUALS_STRING;
import static org.onosproject.yangutils.utils.UtilConstants.FOUR_SPACE_INDENTATION;
import static org.onosproject.yangutils.utils.UtilConstants.GET_METHOD_PREFIX;
import static org.onosproject.yangutils.utils.UtilConstants.IMPL;
import static org.onosproject.yangutils.utils.UtilConstants.JAVA_LANG;
import static org.onosproject.yangutils.utils.UtilConstants.NEW;
import static org.onosproject.yangutils.utils.UtilConstants.NEW_LINE;
import static org.onosproject.yangutils.utils.UtilConstants.OBJECT;
import static org.onosproject.yangutils.utils.UtilConstants.OBJECT_STRING;
import static org.onosproject.yangutils.utils.UtilConstants.OF;
import static org.onosproject.yangutils.utils.UtilConstants.OPEN_CURLY_BRACKET;
import static org.onosproject.yangutils.utils.UtilConstants.OPEN_PARENTHESIS;
import static org.onosproject.yangutils.utils.UtilConstants.OVERRIDE;
import static org.onosproject.yangutils.utils.UtilConstants.PERIOD;
import static org.onosproject.yangutils.utils.UtilConstants.PUBLIC;
import static org.onosproject.yangutils.utils.UtilConstants.QUOTES;
import static org.onosproject.yangutils.utils.UtilConstants.RETURN;
import static org.onosproject.yangutils.utils.UtilConstants.SEMI_COLAN;
import static org.onosproject.yangutils.utils.UtilConstants.SET_METHOD_PREFIX;
import static org.onosproject.yangutils.utils.UtilConstants.SIXTEEN_SPACE_INDENTATION;
import static org.onosproject.yangutils.utils.UtilConstants.SPACE;
import static org.onosproject.yangutils.utils.UtilConstants.STATIC;
import static org.onosproject.yangutils.utils.UtilConstants.STRING_DATA_TYPE;
import static org.onosproject.yangutils.utils.UtilConstants.SUFFIX_S;
import static org.onosproject.yangutils.utils.UtilConstants.THIS;
import static org.onosproject.yangutils.utils.UtilConstants.TWELVE_SPACE_INDENTATION;
import static org.onosproject.yangutils.utils.UtilConstants.VALUE;
import static org.onosproject.yangutils.utils.UtilConstants.VOID;

/**
 * Unit tests for generated methods from the file type.
 */
public final class MethodsGeneratorTest {

    private static final String CLASS_NAME = "testname";
    private static final String ATTRIBUTE_NAME = "testname";

    /**
     * Unit test for private constructor.
     *
     * @throws SecurityException         if any security violation is observed
     * @throws NoSuchMethodException     if when the method is not found
     * @throws IllegalArgumentException  if there is illegal argument found
     * @throws InstantiationException    if instantiation is provoked for the private constructor
     * @throws IllegalAccessException    if instance is provoked or a method is provoked
     * @throws InvocationTargetException when an exception occurs by the method or constructor
     */
    @Test
    public void callPrivateConstructors()
            throws SecurityException, NoSuchMethodException, IllegalArgumentException,
            InstantiationException, IllegalAccessException, InvocationTargetException {

        Class<?>[] classesToConstruct = {MethodsGenerator.class };
        for (Class<?> clazz : classesToConstruct) {
            Constructor<?> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            assertThat(null, not(constructor.newInstance()));
        }
    }

    /**
     * Unit test case for checking the parse builder and type constructor.
     */
    @Test
    public void getTypeConstructorTest() {

        YangPluginConfig pluginConfig = new YangPluginConfig();
        JavaAttributeInfo testAttr = getTestAttribute();
        String test = getTypeConstructorStringAndJavaDoc(testAttr, CLASS_NAME, pluginConfig);
        assertThat(true, is(test.contains(PUBLIC + SPACE + CLASS_NAME + OPEN_PARENTHESIS)));
    }

    /**
     * Test for build method for class.
     */
    @Test
    public void getBuildTest() {
        String method = getBuild(CLASS_NAME);
        assertThat(true, is(method.equals(FOUR_SPACE_INDENTATION + PUBLIC + SPACE + CLASS_NAME + SPACE + BUILD
                + OPEN_PARENTHESIS + CLOSE_PARENTHESIS + SPACE + OPEN_CURLY_BRACKET + NEW_LINE + EIGHT_SPACE_INDENTATION
                + RETURN + SPACE + NEW + SPACE + CLASS_NAME + IMPL + OPEN_PARENTHESIS + THIS + CLOSE_PARENTHESIS
                + SEMI_COLAN + NEW_LINE + FOUR_SPACE_INDENTATION + CLOSE_CURLY_BRACKET)));

    }

    /**
     * Test for build method of interface.
     */
    @Test
    public void getBuildForInterfaceTest() {
        String method = getBuildForInterface(CLASS_NAME);
        assertThat(true, is(method.equals(FOUR_SPACE_INDENTATION + CLASS_NAME + SPACE + BUILD +
                OPEN_PARENTHESIS + CLOSE_PARENTHESIS + SEMI_COLAN + NEW_LINE)));
    }

    /**
     * Test for check not null method.
     */
    @Test
    public void getCheckNotNullTest() {
        String method = getCheckNotNull(CLASS_NAME);
        assertThat(true, is(method.equals(EIGHT_SPACE_INDENTATION + CHECK_NOT_NULL_STRING + OPEN_PARENTHESIS
                + CLASS_NAME + COMMA + SPACE + CLASS_NAME + CLOSE_PARENTHESIS + SEMI_COLAN + NEW_LINE)));
    }

    /**
     * Test case for constructor.
     */
    @Test
    public void getConstructorTest() {
        JavaAttributeInfo testAttr = getTestAttribute();
        YangPluginConfig pluginConfig = new YangPluginConfig();
        String method = getConstructor(CLASS_NAME, testAttr, GENERATE_SERVICE_AND_MANAGER, pluginConfig);
        assertThat(true, is(method.contains(THIS + PERIOD + CLASS_NAME + SPACE + EQUAL + SPACE + "builder" + OBJECT
                + PERIOD + GET_METHOD_PREFIX + "Testname" + OPEN_PARENTHESIS + CLOSE_PARENTHESIS + SEMI_COLAN)));
    }

    /**
     * Test for constrcutor start method.
     */
    @Test
    public void getConstructorStartTest() {
        YangPluginConfig pluginConfig = new YangPluginConfig();
        String method = getConstructorStart(CLASS_NAME, pluginConfig);
        assertThat(true, is(method.contains(PUBLIC + SPACE + CLASS_NAME + IMPL + OPEN_PARENTHESIS + CLASS_NAME
                + BUILDER + SPACE + BUILDER.toLowerCase() + OBJECT + CLOSE_PARENTHESIS + SPACE
                + OPEN_CURLY_BRACKET + NEW_LINE)));
    }

    /**
     * Test case for equals method.
     */
    @Test
    public void getEqualsMethodTest() {
        JavaAttributeInfo testAttr = getTestAttribute();
        String method = getEqualsMethod(testAttr);
        assertThat(true, is(method.contains(SIXTEEN_SPACE_INDENTATION + SPACE + OBJECT_STRING + SUFFIX_S + PERIOD
                + EQUALS_STRING + OPEN_PARENTHESIS)));
    }

    /**
     * Test for to string method.
     */
    @Test
    public void getToStringMethodTest() {
        JavaAttributeInfo testAttr = getTestAttribute();
        String method = getToStringMethod(testAttr);
        assertThat(true, is(method.equals(
                TWELVE_SPACE_INDENTATION + PERIOD + ADD_STRING + OPEN_PARENTHESIS + QUOTES + testAttr.getAttributeName()
                        + QUOTES + COMMA + SPACE + testAttr.getAttributeName() + CLOSE_PARENTHESIS)));
    }

    /**
     * Test for getter method of class.
     */
    @Test
    public void getGetterForClassTest() {
        JavaAttributeInfo testAttr = getTestAttribute();
        String method = getGetterForClass(testAttr, GENERATE_SERVICE_AND_MANAGER);
        assertThat(true, is(method.contains(PUBLIC + SPACE + STRING_DATA_TYPE + SPACE + GET_METHOD_PREFIX)));
    }

    /**
     * Test for getter of interface.
     */
    @Test
    public void getGetterForInterfaceTest() {
        String method = getGetterForInterface(CLASS_NAME, STRING_DATA_TYPE, false, GENERATE_SERVICE_AND_MANAGER);
        assertThat(true, is(method.contains(STRING_DATA_TYPE + SPACE + GET_METHOD_PREFIX)));
    }

    /**
     * Test case for setter method of class.
     */
    @Test
    public void getSetterForClassTest() {
        JavaAttributeInfo testAttr = getTestAttribute();
        String method = getSetterForClass(testAttr, CLASS_NAME, GENERATE_SERVICE_AND_MANAGER);
        assertThat(true, is(
                method.contains(PUBLIC + SPACE + VOID + SPACE +
                        SET_METHOD_PREFIX + getCapitalCase(CLASS_NAME) + OPEN_PARENTHESIS +
                        STRING_DATA_TYPE + SPACE + ATTRIBUTE_NAME)));
    }

    /**
     * Test for setter method of interface.
     */
    @Test
    public void getSetterForInterfaceTest() {
        String method = getSetterForInterface(CLASS_NAME, STRING_DATA_TYPE, CLASS_NAME, false,
                GENERATE_SERVICE_AND_MANAGER);
        assertThat(true, is(method.contains(VOID + SPACE +
                SET_METHOD_PREFIX + "Testname")));
    }

    /**
     * Test case for of method.
     */
    @Test
    public void getOfMethodest() {
        JavaAttributeInfo testAttr = getTestAttribute();
        String method = getOfMethod(CLASS_NAME, testAttr);
        assertThat(true, is(method.contains(PUBLIC + SPACE + STATIC + SPACE + CLASS_NAME + SPACE + OF + OPEN_PARENTHESIS
                + STRING_DATA_TYPE + SPACE + VALUE + CLOSE_PARENTHESIS)));
    }

    /**
     * Test case for setter in type def class.
     */
    @Test
    public void getSetterForTypeDefClassTest() {
        JavaAttributeInfo testAttr = getTestAttribute();
        String method = getSetterForTypeDefClass(testAttr);
        assertThat(true, is(method.contains(PUBLIC + SPACE + VOID + SPACE + SET_METHOD_PREFIX)));
    }

    /**
     * Test case for over ride string.
     */
    @Test
    public void getOverRideStringTest() {
        String method = getOverRideString();
        assertThat(true, is(method.contains(OVERRIDE)));
    }

    /**
     * Returns java attribute.
     *
     * @return java attribute
     */
    private JavaAttributeInfo getTestAttribute() {
        JavaAttributeInfo testAttr = new JavaAttributeInfo(getTestYangType(), ATTRIBUTE_NAME, false, false);
        testAttr.setAttributeName(ATTRIBUTE_NAME);
        testAttr.setAttributeType(getTestYangType());
        testAttr.setImportInfo(getTestJavaQualifiedTypeInfo());
        return testAttr;
    }

    /**
     * Returns java qualified info.
     *
     * @return java qualified info
     */
    private JavaQualifiedTypeInfo getTestJavaQualifiedTypeInfo() {
        JavaQualifiedTypeInfo info = new JavaQualifiedTypeInfo();
        info.setPkgInfo(JAVA_LANG);
        info.setClassInfo(STRING_DATA_TYPE);
        return info;
    }

    /**
     * Returns stub YANG type.
     *
     * @return test YANG type
     */
    private YangType<?> getTestYangType() {
        YangType<?> attrType = new YangType<>();
        attrType.setDataTypeName(STRING_DATA_TYPE);
        attrType.setDataType(STRING);
        return attrType;
    }
}
