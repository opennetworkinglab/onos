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
import org.onosproject.yangutils.translator.GeneratedFileType;
import org.onosproject.yangutils.translator.tojava.GeneratedMethodTypes;
import org.onosproject.yangutils.translator.tojava.TraversalType;
import org.onosproject.yangutils.utils.UtilConstants;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import static org.junit.Assert.assertNotNull;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * Unit tests for class definition generator for generated files.
 */
public final class ClassDefinitionGeneratorTest {

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

        String builderClassDefinition = ClassDefinitionGenerator
                .generateClassDefinition(GeneratedFileType.BUILDER_CLASS_MASK, "BuilderClass");
        assertThat(true, is(builderClassDefinition.contains(UtilConstants.BUILDER)));
        assertThat(true, is(builderClassDefinition.contains(UtilConstants.CLASS)));
    }

    /**
     * Unit test for builder interface definition.
     */
    @Test
    public void generateBuilderInterfaceDefinitionTest() {

        String builderInterfaceDefinition = ClassDefinitionGenerator
                .generateClassDefinition(GeneratedFileType.BUILDER_INTERFACE_MASK, "BuilderInterfaceClass");
        assertThat(true, is(builderInterfaceDefinition.contains(UtilConstants.BUILDER)));
    }

    /**
     * Unit test for impl class definition.
     */
    @Test
    public void generateImplDefinitionTest() {

        String implDefinition = ClassDefinitionGenerator.generateClassDefinition(GeneratedFileType.IMPL_CLASS_MASK,
                "ImplClass");
        assertThat(true, is(implDefinition.contains(UtilConstants.IMPL)));
    }

    /**
     * Unit test for interface definition.
     */
    @Test
    public void generateinterfaceDefinitionTest() {

        String interfaceDefinition = ClassDefinitionGenerator.generateClassDefinition(GeneratedFileType.INTERFACE_MASK,
                "InterfaceClass");
        assertThat(true, is(interfaceDefinition.contains(UtilConstants.INTERFACE)));
    }

    /**
     * Unit test for typedef generated type.
     */
    @Test
    public void generateTypeDefTest() {

        String typeDef = ClassDefinitionGenerator.generateClassDefinition(GeneratedFileType.GENERATE_TYPEDEF_CLASS,
                "invalid");
        assertThat(true, is(typeDef.contains(UtilConstants.CLASS)));
    }

    /**
     * Unit test for enum data types.
     */
    @Test
    public void enumDataTypesTest() {

        TraversalType.valueOf(TraversalType.CHILD.toString());
        GeneratedMethodTypes.valueOf(GeneratedMethodTypes.CONSTRUCTOR.toString());
        TempDataStoreTypes.valueOf(TempDataStoreTypes.CONSTRUCTOR.toString());
    }
}
