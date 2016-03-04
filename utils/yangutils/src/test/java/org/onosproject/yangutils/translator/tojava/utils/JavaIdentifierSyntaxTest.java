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
import org.onosproject.yangutils.utils.UtilConstants;

import static org.junit.Assert.assertNotNull;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Unit tests for java identifier syntax.
 */
public final class JavaIdentifierSyntaxTest {

    public static final String PARENT_PACKAGE = "test5.test6.test7";
    public static final String CHILD_PACKAGE = "test1:test2:test3";
    public static final String DATE1 = "2000-1-5";
    public static final String DATE2 = "1992-01-25";
    public static final String PARENT_WITH_PERIOD = "test5.test6.test7";
    public static final String CHILD_WITH_PERIOD = "test1.test2.test3";
    public static final String DATE_WITH_REV1 = "rev20000105";
    public static final String DATE_WITH_REV2 = "rev19920125";
    public static final String VERSION_NUMBER = "v1";
    public static final String INVALID_NAME_SPACE1 = "byte:#test2:9test3";
    public static final String INVALID_NAME_SPACE2 = "const:#test2://9test3";
    public static final String VALID_NAME_SPACE1 = "_byte.test2._9test3";
    public static final String VALID_NAME_SPACE2 = "_const.test2._9test3";
    public static final String WITHOUT_CAMEL_CASE = "test-camel-case-identifier";
    public static final String WITH_CAMEL_CASE = "testCamelCaseIdentifier";
    public static final String WITHOUT_CAPITAL = "test_this";
    public static final String WITH_CAPITAL = "Test_this";

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
    @Test
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
        String pkgFromParent = JavaIdentifierSyntax.getPackageFromParent(PARENT_PACKAGE, CHILD_PACKAGE);
        assertThat(pkgFromParent.equals(PARENT_WITH_PERIOD + UtilConstants.PERIOD + CHILD_WITH_PERIOD), is(true));
    }

    /**
     * Unit test for root package generation with revision complexity.
     */
    @Test
    public void getRootPackageTest() {

        String rootPackage = JavaIdentifierSyntax.getRootPackage((byte) 1, CHILD_PACKAGE, DATE1);
        assertThat(rootPackage.equals(UtilConstants.DEFAULT_BASE_PKG + UtilConstants.PERIOD + VERSION_NUMBER
                + UtilConstants.PERIOD + CHILD_WITH_PERIOD + UtilConstants.PERIOD + DATE_WITH_REV1), is(true));
    }

    /**
     * Unit test for root package generation with special characters presence.
     */
    @Test
    public void getRootPackageWithSpecialCharactersTest() {

        String rootPackage = JavaIdentifierSyntax.getRootPackage((byte) 1, INVALID_NAME_SPACE1, DATE1);
        assertThat(rootPackage.equals(UtilConstants.DEFAULT_BASE_PKG + UtilConstants.PERIOD + VERSION_NUMBER
                + UtilConstants.PERIOD + VALID_NAME_SPACE1 + UtilConstants.PERIOD + DATE_WITH_REV1), is(true));
        String rootPackage1 = JavaIdentifierSyntax.getRootPackage((byte) 1, INVALID_NAME_SPACE2, DATE1);
        assertThat(rootPackage1.equals(UtilConstants.DEFAULT_BASE_PKG + UtilConstants.PERIOD + VERSION_NUMBER
                + UtilConstants.PERIOD + VALID_NAME_SPACE2 + UtilConstants.PERIOD + DATE_WITH_REV1), is(true));
    }

    /**
     * Unit test for root package generation without complexity in revision.
     */
    @Test
    public void getRootPackageWithRevTest() {

        String rootPkgWithRev = JavaIdentifierSyntax.getRootPackage((byte) 1, CHILD_PACKAGE, DATE2);
        assertThat(rootPkgWithRev.equals(UtilConstants.DEFAULT_BASE_PKG + UtilConstants.PERIOD
                + VERSION_NUMBER + UtilConstants.PERIOD + CHILD_WITH_PERIOD + UtilConstants.PERIOD + DATE_WITH_REV2),
                is(true));
    }

    /**
     * Unit test for capitalizing the incoming string.
     */
    @Test
    public void getCapitalCaseTest() {

        String capitalCase = JavaIdentifierSyntax.getCaptialCase(WITHOUT_CAPITAL);
        assertThat(capitalCase.equals(WITH_CAPITAL), is(true));
    }

    /**
     * Unit test for getting the camel case for the received string.
     */
    @Test
    public void getCamelCaseTest() {
        String camelCase = JavaIdentifierSyntax.getCamelCase(WITHOUT_CAMEL_CASE);
        assertThat(camelCase.equals(WITH_CAMEL_CASE), is(true));
    }
}
