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

package org.onosproject.yangutils.utils.io.impl;

import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.onosproject.yangutils.utils.io.impl.JavaDocGen.JavaDocType;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;

/**
 * Tests the java doc that is generated.
 */
public final class JavaDocGenTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /**
     * This test case checks the content recieved for the builder class java doc.
     */
    @Test
    public void builderClassGenerationTest() {

        String builderClassJavaDoc = JavaDocGen.getJavaDoc(JavaDocType.BUILDER_CLASS, "testGeneration1", false);
        assertTrue(builderClassJavaDoc.contains("Provides the builder implementation of")
                && builderClassJavaDoc.contains(" */\n"));
    }

    /**
     * This test case checks the content recieved for the builder interface ge java doc.
     */
    @Test
    public void builderInterfaceGenerationTest() {

        String builderInterfaceJavaDoc = JavaDocGen.getJavaDoc(JavaDocType.BUILDER_INTERFACE, "testGeneration1", false);
        assertTrue(builderInterfaceJavaDoc.contains("Builder for") && builderInterfaceJavaDoc.contains(" */\n"));
    }

    /**
     * This test case checks the content recieved for the build  java doc.
     */
    @Test
    public void buildGenerationTest() {

        String buildDoc = JavaDocGen.getJavaDoc(JavaDocType.BUILD, "testGeneration1", false);
        assertTrue(buildDoc.contains("Builds object of") && buildDoc.contains(" */\n"));
    }

    /**
     * A private constructor is tested.
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

        Class<?>[] classesToConstruct = {JavaDocGen.class };
        for (Class<?> clazz : classesToConstruct) {
            Constructor<?> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            assertNotNull(constructor.newInstance());
        }
    }

    /**
     * This test case checks the content recieved for the constructor java doc.
     */
    @Test
    public void constructorGenerationTest() {

        String constructorDoc = JavaDocGen.getJavaDoc(JavaDocType.CONSTRUCTOR, "testGeneration1", false);
        assertTrue(
                constructorDoc.contains("Construct the object of") && constructorDoc.contains("builder object of")
                && constructorDoc.contains("@param") && constructorDoc.contains("*/\n"));
        JavaDocType.valueOf(JavaDocType.CONSTRUCTOR.toString());
    }

    /**
     * This test case checks the content recieved for the default constructor java doc.
     */
    @Test
    public void defaultConstructorGenerationTest() {

        String defaultConstructorDoc = JavaDocGen.getJavaDoc(JavaDocType.DEFAULT_CONSTRUCTOR, "testGeneration1", false);
        assertTrue(defaultConstructorDoc.contains("Default Constructor") && defaultConstructorDoc.contains(" */\n"));
    }

    /**
     * This test case checks the content recieved for the getter java doc.
     */
    @Test
    public void getterGenerationTest() {

        String getterJavaDoc = JavaDocGen.getJavaDoc(JavaDocType.GETTER, "testGeneration1", false);
        assertTrue(getterJavaDoc.contains("Returns the attribute") && getterJavaDoc.contains(" */\n"));
    }

    /**
     * This test case checks the content recieved for the impl class java doc.
     */
    @Test
    public void implClassGenerationTest() {
        String implClassJavaDoc = JavaDocGen.getJavaDoc(JavaDocType.IMPL_CLASS, "testGeneration1", false);
        assertTrue(implClassJavaDoc.contains("Provides the implementation of") && implClassJavaDoc.contains(" */\n"));
    }

    /**
     * This test case checks the content recieved for the interface java doc.
     */
    @Test
    public void interfaceGenerationTest() {

        String interfaceJavaDoc = JavaDocGen.getJavaDoc(JavaDocType.INTERFACE, "testGeneration1", false);
        assertTrue(interfaceJavaDoc.contains("Abstraction of an entity which provides functionalities of")
                && interfaceJavaDoc.contains(" */\n"));
    }

    /**
     * This test case checks the content recieved for the package info  java doc.
     */
    @Test
    public void packageInfoGenerationTest() {

        String packageInfo = JavaDocGen.getJavaDoc(JavaDocType.PACKAGE_INFO, "testGeneration1", false);
        assertTrue(packageInfo.contains("Generated java code corresponding to YANG") && packageInfo.contains(" */\n"));
    }

    /**
     * This test case checks the content recieved for the setter java doc.
     */
    @Test
    public void setterGenerationTest() {

        String setterJavaDoc = JavaDocGen.getJavaDoc(JavaDocType.SETTER, "testGeneration1", false);
        assertTrue(setterJavaDoc.contains("Returns the builder object of") && setterJavaDoc.contains(" */\n"));
    }

    /**
     * This test case checks the content received for the typedef setter java doc.
     */
    @Test
    public void typeDefSetterGenerationTest() {

        String typeDefSetter = JavaDocGen.getJavaDoc(JavaDocType.TYPE_DEF_SETTER, "testGeneration1", false);
        assertTrue(typeDefSetter.contains("Sets the value of") && typeDefSetter.contains(" */\n"));
    }
}