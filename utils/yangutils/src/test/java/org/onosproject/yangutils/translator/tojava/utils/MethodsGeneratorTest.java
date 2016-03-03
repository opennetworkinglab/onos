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
import org.onosproject.yangutils.datamodel.YangType;
import org.onosproject.yangutils.translator.GeneratedFileType;
import org.onosproject.yangutils.translator.tojava.AttributeInfo;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

/**
 * Unit tests for generated methods from the file type.
 */
public final class MethodsGeneratorTest {

    public static AttributeInfo testAttr = new AttributeInfo();
    public static YangType<?> attrType = new YangType<>();

    /**
     * Unit test for private constructor.
     *
     * @throws SecurityException if any security violation is observed
     * @throws NoSuchMethodException if when the method is not found
     * @throws IllegalArgumentException if there is illegal argument found
     * @throws InstantiationException if instantiation is provoked for the
     *             private constructor
     * @throws IllegalAccessException if instance is provoked or a method is
     *             provoked
     * @throws InvocationTargetException when an exception occurs by the method
     *             or constructor
     */
    @Test
    public void callPrivateConstructors() throws SecurityException, NoSuchMethodException, IllegalArgumentException,
            InstantiationException, IllegalAccessException, InvocationTargetException {

        Class<?>[] classesToConstruct = {
                MethodsGenerator.class };
        for (Class<?> clazz : classesToConstruct) {
            Constructor<?> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            assertNotNull(constructor.newInstance());
        }
    }

    /**
     * Unit test for checking the values received from constructor, default
     * constructor and build string formation.
     */
    @Test
    public void getValuesTest() {
        String stringConstructor = MethodsGenerator.getConstructorString("testname");
        assertThat(
                stringConstructor.contains("Construct the object of testnameImpl.")
                        && stringConstructor.contains("@param testnameObject builder object of  testname")
                        && stringConstructor.contains("public testnameImpl(testnameBuilder testnameObject) {"),
                is(true));
        String stringDefaultConstructor = MethodsGenerator.getDefaultConstructorString(
                GeneratedFileType.BUILDER_CLASS_MASK,
                "testname");
        assertThat(stringDefaultConstructor.contains("Default Constructor.")
                && stringDefaultConstructor.contains("public testnameBuilder() {")
                && stringDefaultConstructor.contains("}"), is(true));
        String stringBuild = MethodsGenerator.getBuildString("testname");
        assertThat(
                stringBuild.contains("public testname build() {")
                        && stringBuild.contains("return new testnameImpl(this);") && stringBuild.contains("}"),
                is(true));
    }
}