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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.BUILDER_CLASS_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.BUILDER_INTERFACE_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.GENERATE_TYPEDEF_CLASS;
import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.IMPL_CLASS_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.INTERFACE_MASK;
import static org.onosproject.yangutils.translator.tojava.utils.ClassDefinitionGenerator.generateClassDefinition;
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
 * Unit tests for class definition generator for generated files.
 */
public final class ClassDefinitionGeneratorTest {

    private static final String CLASS_NAME = "testclass";

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

        Class<?>[] classesToConstruct = {ClassDefinitionGenerator.class };
        for (Class<?> clazz : classesToConstruct) {
            Constructor<?> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            assertNotNull(constructor.newInstance());
        }
    }

    /**
     * Unit test for builder class definition.
     */
    @Test
    public void generateBuilderClassDefinitionTest() {

        String builderClassDefinition = generateClassDefinition(BUILDER_CLASS_MASK, CLASS_NAME);
        assertThat(true, is(builderClassDefinition.equals(
                PUBLIC + SPACE + CLASS + SPACE + CLASS_NAME + BUILDER + SPACE + IMPLEMENTS + SPACE + CLASS_NAME + PERIOD
                        + CLASS_NAME + BUILDER + SPACE + OPEN_CURLY_BRACKET + NEW_LINE)));
    }

    /**
     * Unit test for builder interface definition.
     */
    @Test
    public void generateBuilderInterfaceDefinitionTest() {

        String builderInterfaceDefinition = generateClassDefinition(BUILDER_INTERFACE_MASK, CLASS_NAME);
        assertThat(true, is(builderInterfaceDefinition
                .equals(INTERFACE + SPACE + CLASS_NAME + BUILDER + SPACE + OPEN_CURLY_BRACKET + NEW_LINE + NEW_LINE)));
    }

    /**
     * Unit test for impl class definition.
     */
    @Test
    public void generateImplDefinitionTest() {

        String implDefinition = generateClassDefinition(IMPL_CLASS_MASK, CLASS_NAME);
        assertThat(true, is(implDefinition.equals(
                PUBLIC + SPACE + FINAL + SPACE + CLASS + SPACE + CLASS_NAME + IMPL + SPACE + IMPLEMENTS + SPACE
                        + CLASS_NAME + SPACE + OPEN_CURLY_BRACKET + NEW_LINE)));
    }

    /**
     * Unit test for interface definition.
     */
    @Test
    public void generateinterfaceDefinitionTest() {

        String interfaceDefinition = generateClassDefinition(INTERFACE_MASK, CLASS_NAME);
        assertThat(true, is(interfaceDefinition
                .equals(PUBLIC + SPACE + INTERFACE + SPACE + CLASS_NAME + SPACE + OPEN_CURLY_BRACKET + NEW_LINE)));
    }

    /**
     * Unit test for typedef generated type.
     */
    @Test
    public void generateTypeDefTest() {

        String typeDef = generateClassDefinition(GENERATE_TYPEDEF_CLASS, CLASS_NAME);
        assertThat(true, is(typeDef.equals(
                PUBLIC + SPACE + FINAL + SPACE + CLASS + SPACE + CLASS_NAME + SPACE + OPEN_CURLY_BRACKET + NEW_LINE)));
    }
}
