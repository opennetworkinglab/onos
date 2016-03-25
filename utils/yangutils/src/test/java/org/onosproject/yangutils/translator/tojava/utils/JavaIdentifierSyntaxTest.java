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
import static org.onosproject.yangutils.translator.tojava.utils.JavaIdentifierSyntax.getCamelCase;
import static org.onosproject.yangutils.translator.tojava.utils.JavaIdentifierSyntax.getCaptialCase;
import static org.onosproject.yangutils.translator.tojava.utils.JavaIdentifierSyntax.getJavaPackageFromPackagePath;
import static org.onosproject.yangutils.translator.tojava.utils.JavaIdentifierSyntax.getPackageDirPathFromJavaJPackage;
import static org.onosproject.yangutils.translator.tojava.utils.JavaIdentifierSyntax.getRootPackage;
import static org.onosproject.yangutils.translator.tojava.utils.JavaIdentifierSyntax.getSmallCase;
import static org.onosproject.yangutils.translator.tojava.utils.JavaIdentifierSyntax.getYangRevisionStr;
import static org.onosproject.yangutils.utils.UtilConstants.DEFAULT_BASE_PKG;
import static org.onosproject.yangutils.utils.UtilConstants.PERIOD;

/**
 * Unit tests for java identifier syntax.
 */
public final class JavaIdentifierSyntaxTest {

    private static final String PARENT_PACKAGE = "test5/test6/test7";
    private static final String CHILD_PACKAGE = "test1:test2:test3";
    private static final String DATE1 = "2000-1-5";
    private static final String DATE2 = "1992-01-25";
    private static final String PARENT_WITH_PERIOD = "test5.test6.test7";
    private static final String CHILD_WITH_PERIOD = "test1.test2.test3";
    private static final String DATE_WITH_REV1 = "rev20000105";
    private static final String DATE_WITH_REV2 = "rev19920125";
    private static final String VERSION_NUMBER = "v1";
    private static final String INVALID_NAME_SPACE1 = "byte:#test2:9test3";
    private static final String INVALID_NAME_SPACE2 = "const:#test2://9test3";
    private static final String VALID_NAME_SPACE1 = "_byte.test2._9test3";
    private static final String VALID_NAME_SPACE2 = "_const.test2._9test3";
    private static final String WITHOUT_CAMEL_CASE = "test-camel-case-identifier";
    private static final String WITH_CAMEL_CASE = "testCamelCaseIdentifier";
    private static final String WITHOUT_CAPITAL = "test_this";
    private static final String WITH_CAPITAL = "Test_this";
    private static final String WITH_SMALL = "test_this";

    /**
     * Unit test for private constructor.
     *
     * @throws SecurityException if any security violation is observed.
     * @throws NoSuchMethodException if when the method is not found.
     * @throws IllegalArgumentException if there is illegal argument found.
     * @throws InstantiationException if instantiation is provoked for the
     *             private constructor.
     * @throws IllegalAccessException if instance is provoked or a method is
     *             provoked.
     * @throws InvocationTargetException when an exception occurs by the method
     *             or constructor.
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
     * Unit test for root package generation with revision complexity.
     */
    @Test
    public void getRootPackageTest() {

        String rootPackage = getRootPackage((byte) 1, CHILD_PACKAGE, DATE1);
        assertThat(rootPackage.equals(DEFAULT_BASE_PKG + PERIOD + VERSION_NUMBER
                + PERIOD + CHILD_WITH_PERIOD + PERIOD + DATE_WITH_REV1), is(true));
    }

    /**
     * Unit test for root package generation with special characters presence.
     */
    @Test
    public void getRootPackageWithSpecialCharactersTest() {

        String rootPackage = getRootPackage((byte) 1, INVALID_NAME_SPACE1, DATE1);
        assertThat(rootPackage.equals(DEFAULT_BASE_PKG + PERIOD + VERSION_NUMBER
                + PERIOD + VALID_NAME_SPACE1 + PERIOD + DATE_WITH_REV1), is(true));
        String rootPackage1 = getRootPackage((byte) 1, INVALID_NAME_SPACE2, DATE1);
        assertThat(rootPackage1.equals(DEFAULT_BASE_PKG + PERIOD + VERSION_NUMBER
                + PERIOD + VALID_NAME_SPACE2 + PERIOD + DATE_WITH_REV1), is(true));
    }

    /**
     * Unit test for root package generation without complexity in revision.
     */
    @Test
    public void getRootPackageWithRevTest() {

        String rootPkgWithRev = getRootPackage((byte) 1, CHILD_PACKAGE, DATE2);
        assertThat(rootPkgWithRev.equals(
                DEFAULT_BASE_PKG + PERIOD + VERSION_NUMBER + PERIOD + CHILD_WITH_PERIOD + PERIOD + DATE_WITH_REV2),
                is(true));
    }

    /**
     * Unit test for capitalizing the incoming string.
     */
    @Test
    public void getCapitalCaseTest() {

        String capitalCase = getCaptialCase(WITHOUT_CAPITAL);
        assertThat(capitalCase.equals(WITH_CAPITAL), is(true));
    }

    /**
     * Unit test for getting the camel case for the received string.
     */
    @Test
    public void getCamelCaseTest() {

        String camelCase = getCamelCase(WITHOUT_CAMEL_CASE);
        assertThat(camelCase.equals(WITH_CAMEL_CASE), is(true));
    }

    /**
     * Unit test for getting the camel case for the received string.
     */
    @Test
    public void getSmallCaseTest() {

        String smallCase = getSmallCase(WITHOUT_CAPITAL);
        assertThat(smallCase.equals(WITH_SMALL), is(true));
    }

    /**
     * Unit test for getting the camel case for the received string.
     */
    @Test
    public void getPackageFromPathTest() {

        String pkg = getJavaPackageFromPackagePath(PARENT_PACKAGE);
        assertThat(pkg.equals(PARENT_WITH_PERIOD), is(true));
    }

    /**
     * Unit test for getting the camel case for the received string.
     */
    @Test
    public void getPathFromPackageTest() {

        String path = getPackageDirPathFromJavaJPackage(PARENT_WITH_PERIOD);
        assertThat(path.equals(PARENT_PACKAGE), is(true));
    }

    /**
     * Unit test for getting the camel case for the received string.
     */
    @Test
    public void getYangRevTest() {

        String rev = getYangRevisionStr(DATE1);
        assertThat(rev.equals(DATE_WITH_REV1), is(true));
    }
}
