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
import static org.junit.Assert.assertNotNull;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Unit tests for java identifier syntax.
 */
public final class JavaIdentifierSyntaxTest {

    /**
     * Unit test for private constructor.
     *
     * @throws SecurityException if any security violation is observed.
     * @throws NoSuchMethodException if when the method is not found.
     * @throws IllegalArgumentException if there is illegal argument found.
     * @throws InstantiationException if instantiation is provoked for the private constructor.
     * @throws IllegalAccessException if instance is provoked or a method is provoked.
     * @throws InvocationTargetException when an exception occurs by the method or constructor.
     */
    public void callPrivateConstructors() throws SecurityException, NoSuchMethodException, IllegalArgumentException,
    InstantiationException, IllegalAccessException, InvocationTargetException {
        Class<?>[] classesToConstruct = {JavaIdentifierSyntax.class };
        for (Class<?> clazz : classesToConstruct) {
            Constructor<?> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            assertNotNull(constructor.newInstance());
        }
    }

    /**
     * Unit test for testing the package path generation from a parent package.
     */
    @Test
    public void getPackageFromParentTest() {
        String pkgFromParent = JavaIdentifierSyntax.getPackageFromParent("test5.test6.test7", "test1:test2:test3");
        assertThat(pkgFromParent.equals("test5.test6.test7.test1.test2.test3"), is(true));
    }

    /**
     * Unit test for root package generation with revision complexity.
     */
    @Test
    public void getRootPackageTest() {

        String rootPackage = JavaIdentifierSyntax.getRootPackage((byte) 0, "test1:test2:test3", "5-1-2000");
        assertThat(rootPackage.equals("org.onosproject.yang.gen.v0.test1.test2.test3.rev05012000"), is(true));
    }

    /**
     * Unit test for root package generation without revision complexity.
     */
    @Test
    public void getRootPackageWithRevTest() {

        String rootPkgWithRev = JavaIdentifierSyntax.getRootPackage((byte) 0, "test1:test2:test3", "25-01-1992");
        assertThat(rootPkgWithRev.equals("org.onosproject.yang.gen.v0.test1.test2.test3.rev25011992"), is(true));
    }

    /**
     * Unit test for capitalizing the incoming string.
     */
    @Test
    public void getCapitalCaseTest() {

        String capitalCase = JavaIdentifierSyntax.getCaptialCase("test_this");
        assertThat(capitalCase.equals("Test_this"), is(true));
    }

    /**
     * Unit test for getting the camel case for the received string.
     */
    @Test
    public void getCamelCaseTest() {
        String camelCase = JavaIdentifierSyntax.getCamelCase("test-camel-case-identifier");
        assertThat(camelCase.equals("testCamelCaseIdentifier"), is(true));
    }
}
