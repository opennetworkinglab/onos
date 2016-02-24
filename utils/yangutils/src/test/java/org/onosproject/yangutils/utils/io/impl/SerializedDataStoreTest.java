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
import org.onosproject.yangutils.utils.io.impl.SerializedDataStore.SerializedDataStoreType;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import static org.slf4j.LoggerFactory.getLogger;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertNotNull;

/**
 * Unit tests for the serialized data store for its contents.
 */
public final class SerializedDataStoreTest {

    private final Logger log = getLogger(getClass());

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /**
     * A private constructor is tested.
     *
     * @throws SecurityException if any security violation is observed.
     * @throws NoSuchMethodException if when the method is not found.
     * @throws IllegalArgumentException if there is illegal argument found.
     * @throws InstantiationException if instantiation is provoked for the private constructor.
     * @throws IllegalAccessException if instance is provoked or a method is provoked.
     * @throws InvocationTargetException when an exception occurs by the method or constructor.
     */
    @Test
    public void callPrivateConstructors() throws SecurityException, NoSuchMethodException, IllegalArgumentException,
    InstantiationException, IllegalAccessException, InvocationTargetException {

        Class<?>[] classesToConstruct = {SerializedDataStore.class };
        for (Class<?> clazz : classesToConstruct) {
            Constructor<?> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            assertNotNull(constructor.newInstance());
        }
    }

    /**
     * This test case checks the attribute info that is read and put into the list.
     */
    @Test
    public void insertAttributeDataTest() throws IOException, ClassNotFoundException, FileNotFoundException {

        String attributeData = "attribute content lists this";
        SerializedDataStore.setSerializeData(attributeData, SerializedDataStoreType.ATTRIBUTE);
        List<String> attributeInfo = SerializedDataStore.getSerializeData(SerializedDataStoreType.ATTRIBUTE);
        List<String> expectedinfo = new LinkedList<>();
        expectedinfo.add(attributeData);
        assertThat(true, is(attributeInfo.equals(expectedinfo)));
        SerializedDataStoreType.valueOf(SerializedDataStoreType.ATTRIBUTE.toString());
    }

    /**
     * This test case checks the builder interface that is read and put into the list.
     */
    @Test
    public void insertBuilderInterfaceMethodsTest() throws IOException, ClassNotFoundException, FileNotFoundException {

        String builderInterfaceMethodsData = "builder interface methods content lists this";
        SerializedDataStore.setSerializeData(builderInterfaceMethodsData,
                SerializedDataStoreType.BUILDER_INTERFACE_METHODS);
        List<String> attributeInfo = SerializedDataStore
                .getSerializeData(SerializedDataStoreType.BUILDER_INTERFACE_METHODS);
        List<String> expectedinfo = new LinkedList<>();
        expectedinfo.add(builderInterfaceMethodsData);
        assertThat(true, is(attributeInfo.equals(expectedinfo)));
    }

    /**
     * This test case checks the builder methods that is read and put into the list.
     */
    @Test
    public void insertBuilderMethodsTest() throws IOException, ClassNotFoundException, FileNotFoundException {

        String builderMethodsData = "builder methods content lists this";
        SerializedDataStore.setSerializeData(builderMethodsData, SerializedDataStoreType.BUILDER_METHODS);
        List<String> attributeInfo = SerializedDataStore.getSerializeData(SerializedDataStoreType.BUILDER_METHODS);
        List<String> expectedinfo = new LinkedList<>();
        expectedinfo.add(builderMethodsData);
        assertThat(true, is(attributeInfo.equals(expectedinfo)));
    }

    /**
     * This test case checks the impl methods that is read and put into the list.
     */
    @Test
    public void insertImplMethodsTest() throws IOException, ClassNotFoundException, FileNotFoundException {

        String implMethodsData = "impl methods content lists this";
        SerializedDataStore.setSerializeData(implMethodsData, SerializedDataStoreType.IMPL_METHODS);
        List<String> attributeInfo = SerializedDataStore.getSerializeData(SerializedDataStoreType.IMPL_METHODS);
        List<String> expectedinfo = new LinkedList<>();
        expectedinfo.add(implMethodsData);
        assertThat(true, is(attributeInfo.equals(expectedinfo)));
    }

    /**
     * This test case checks the import methods that is read and put into the list.
     */
    @Test
    public void insertImportTest() throws IOException, ClassNotFoundException, FileNotFoundException {

        String importData = "interface methods content lists this";
        SerializedDataStore.setSerializeData(importData, SerializedDataStoreType.IMPORT);
        List<String> attributeInfo = SerializedDataStore.getSerializeData(SerializedDataStoreType.IMPORT);
        List<String> expectedinfo = new LinkedList<>();
        expectedinfo.add(importData);
        assertThat(true, is(attributeInfo.equals(expectedinfo)));
    }

    /**
     * This test case checks the interface methods that is read and put into the list.
     */
    @Test
    public void insertInterfaceMethodsTest() throws IOException, ClassNotFoundException, FileNotFoundException {

        String interfaceMethodsData = "interface methods content lists this";
        SerializedDataStore.setSerializeData(interfaceMethodsData, SerializedDataStoreType.INTERFACE_METHODS);
        List<String> attributeInfo = SerializedDataStore.getSerializeData(SerializedDataStoreType.INTERFACE_METHODS);
        List<String> expectedinfo = new LinkedList<>();
        expectedinfo.add(interfaceMethodsData);
        assertThat(true, is(attributeInfo.equals(expectedinfo)));
    }
}