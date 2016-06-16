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

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.onosproject.yangutils.translator.exception.TranslatorException;
import org.onosproject.yangutils.utils.io.impl.YangToJavaNamingConflictUtil;

import static org.apache.commons.io.FileUtils.deleteDirectory;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;
import static org.onosproject.yangutils.translator.tojava.utils.JavaIdentifierSyntax.doesPackageExist;
import static org.onosproject.yangutils.translator.tojava.utils.JavaIdentifierSyntax.getRootPackage;
import static org.onosproject.yangutils.utils.UtilConstants.DEFAULT_BASE_PKG;
import static org.onosproject.yangutils.utils.UtilConstants.PERIOD;
import static org.onosproject.yangutils.utils.UtilConstants.SLASH;
import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.getCamelCase;
import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.getCapitalCase;
import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.getJavaPackageFromPackagePath;
import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.getPackageDirPathFromJavaJPackage;
import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.getSmallCase;

/**
 * Unit tests for java identifier syntax.
 */
public final class JavaIdentifierSyntaxTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private static final String PARENT_PACKAGE = "test5/test6/test7";
    private static final String CHILD_PACKAGE = "test1:test2:test3";
    private static final String DATE1 = "2000-1-5";
    private static final String DATE2 = "1992-01-25";
    private static final String PARENT_WITH_PERIOD = "test5.test6.test7";
    private static final String CHILD_WITH_PERIOD = "test1.test2.test3";
    private static final String DATE_WITH_REV1 = "rev20000105";
    private static final String DATE_WITH_REV2 = "rev19920125";
    private static final String VERSION_NUMBER = "v1";
    private static final String VALID_PREFIX = "123add-prefix";
    private static final String INVALID_PREFIX = "-*()&^&#$%";
    private static final String INVALID_PREFIX1 = "abc~!@#$%^&*()_+}{:<>?`1234567890-=[]''|,./SS";
    private static final String INVALID_NAME_SPACE_FOR_INVALID_PREFIX = "try:#test3:9case3";
    private static final String INVALID_NAME_SPACE1 = "byte:#test2:9test3";
    private static final String INVALID_NAME_SPACE2 = "const:#test2://9test3";
    private static final String INVALID_NAME_SPACE3 = "CONST:TRY://9test3";
    private static final String VALID_NAME_SPACE1 = "123addprefixbyte.test2.123addprefix9test3";
    private static final String VALID_NAME_SPACE2 = "yangautoprefixconst.test2.yangautoprefix9test3";
    private static final String VALID_NAME_SPACE3 = "abc1234567890ssconst.test2.abc1234567890ss9test3";
    private static final String VALID_NAME_SPACE4 = "yangautoprefixconst.yangautoprefixtry.yangautoprefix9test3";
    private static final String WITHOUT_CAMEL_CASE = "test-camel-case-identifier";
    private static final String WITH_CAMEL_CASE = "testCamelCaseIdentifier";
    private static final String WITHOUT_CAMEL_CASE1 = ".-_try-._-.123";
    private static final String WITH_CAMEL_CASE1 = "try123";
    private static final String WITHOUT_CAMEL_CASE2 = "_try";
    private static final String WITH_CAMEL_CASE2 = "yangAutoPrefixTry";
    private static final String WITHOUT_CAMEL_CASE3 = "-1-123g-123ga-a";
    private static final String WITH_CAMEL_CASE3 = "yangAutoPrefix1123G123Gaa";
    private static final String WITHOUT_CAMEL_CASE4 = "a-b-c-d-e-f-g-h";
    private static final String WITH_CAMEL_CASE4 = "aBcDeFgh";
    private static final String WITHOUT_CAMEL_CASE5 = "TestName";
    private static final String WITH_CAMEL_CASE5 = "testName";
    private static final String WITHOUT_CAMEL_CASE6 = "TEST-NAME";
    private static final String WITH_CAMEL_CASE6 = "testName";
    private static final String WITHOUT_CAMEL_CASE7 = "TESTNAME";
    private static final String WITH_CAMEL_CASE7 = "testname";
    private static final String WITHOUT_CAMEL_CASE8 = "TE-ST-NA-ME";
    private static final String WITH_CAMEL_CASE8 = "teStNaMe";
    private static final String WITHOUT_CAMEL_CASE9 = "TEST3NAME";
    private static final String WITH_CAMEL_CASE9 = "test3Name";
    private static final String WITHOUT_CAMEL_CASE10 = "TEST3";
    private static final String WITH_CAMEL_CASE10 = "test3";
    private static final String WITHOUT_CAMEL_CASE11 = "TEST3nAMe";
    private static final String WITH_CAMEL_CASE11 = "test3Name";
    private static final String WITHOUT_CAMEL_CASE12 = "TEST3name";
    private static final String WITH_CAMEL_CASE12 = "test3Name";
    private static final String WITHOUT_CAMEL_CASE13 = "t-RY";
    private static final String WITH_CAMEL_CASE13 = "tRy";
    private static final String WITHOUT_CAMEL_CASE14 = "TRY";
    private static final String WITH_CAMEL_CASE14 = "yangAutoPrefixTry";
    private static final String WITHOUT_CAPITAL = "test_this";
    private static final String WITH_CAPITAL = "Test_this";
    private static final String WITH_SMALL = "test_this";
    private static final String WITH_CAMEL_CASE_WITH_PREFIX = "123addPrefixTry";
    private static final String WITH_CAMEL_CASE_WITH_PREFIX1 = "abc1234567890Ss1123G123Gaa";
    private static YangToJavaNamingConflictUtil conflictResolver = new YangToJavaNamingConflictUtil();
    private static final String BASE_DIR_PKG = "target.UnitTestCase.";
    private static final String DIR_PATH = "exist1.exist2.exist3";
    private static final String PKG_INFO = "package-info.java";
    private static final String BASE_PKG = "target/UnitTestCase";
    private static final String TEST_DATA_1 = "This is to append a text to the file first1\n";

    /**
     * Unit test for private constructor.
     *
     * @throws SecurityException         if any security violation is observed.
     * @throws NoSuchMethodException     if when the method is not found.
     * @throws IllegalArgumentException  if there is illegal argument found.
     * @throws InstantiationException    if instantiation is provoked for the
     *                                   private constructor.
     * @throws IllegalAccessException    if instance is provoked or a method is
     *                                   provoked.
     * @throws InvocationTargetException when an exception occurs by the method
     *                                   or constructor.
     */
    @Test
    public void callPrivateConstructors() throws SecurityException, NoSuchMethodException, IllegalArgumentException,
            InstantiationException, IllegalAccessException, InvocationTargetException {

        Class<?>[] classesToConstruct = {JavaIdentifierSyntax.class};
        for (Class<?> clazz : classesToConstruct) {
            Constructor<?> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            assertThat(null, not(constructor.newInstance()));
        }
    }

    /**
     * Unit test for root package generation with revision complexity.
     */
    @Test
    public void getRootPackageTest() {
        conflictResolver.setPrefixForIdentifier(null);
        String rootPackage = getRootPackage((byte) 1, CHILD_PACKAGE, DATE1, conflictResolver);
        assertThat(rootPackage.equals(DEFAULT_BASE_PKG + PERIOD + VERSION_NUMBER
                + PERIOD + CHILD_WITH_PERIOD + PERIOD + DATE_WITH_REV1), is(true));
    }

    /**
     * Unit test for root package generation with invalid prefix.
     */
    @Test
    public void getRootPackageWithInvalidPrefix() throws TranslatorException {
        thrown.expect(TranslatorException.class);
        thrown.expectMessage("The given prefix in pom.xml is invalid.");
        conflictResolver.setPrefixForIdentifier(INVALID_PREFIX);
        String rootPackage1 = getRootPackage((byte) 1, INVALID_NAME_SPACE_FOR_INVALID_PREFIX, DATE1, conflictResolver);
    }

    /**
     * Unit test for root package generation with special characters presence.
     */
    @Test
    public void getRootPackageWithSpecialCharactersTest() {
        conflictResolver.setPrefixForIdentifier(VALID_PREFIX);
        String rootPackage = getRootPackage((byte) 1, INVALID_NAME_SPACE1, DATE1, conflictResolver);
        assertThat(rootPackage.equals(DEFAULT_BASE_PKG + PERIOD + VERSION_NUMBER
                + PERIOD + VALID_NAME_SPACE1 + PERIOD + DATE_WITH_REV1), is(true));
        conflictResolver.setPrefixForIdentifier(null);
        String rootPackage1 = getRootPackage((byte) 1, INVALID_NAME_SPACE2, DATE1, conflictResolver);
        assertThat(rootPackage1.equals(DEFAULT_BASE_PKG + PERIOD + VERSION_NUMBER
                + PERIOD + VALID_NAME_SPACE2 + PERIOD + DATE_WITH_REV1), is(true));
        String rootPackage2 = getRootPackage((byte) 1, INVALID_NAME_SPACE3, DATE1, conflictResolver);
        assertThat(rootPackage2.equals(DEFAULT_BASE_PKG + PERIOD + VERSION_NUMBER
                + PERIOD + VALID_NAME_SPACE4 + PERIOD + DATE_WITH_REV1), is(true));
        conflictResolver.setPrefixForIdentifier(INVALID_PREFIX1);
        String rootPackage3 = getRootPackage((byte) 1, INVALID_NAME_SPACE2, DATE1, conflictResolver);
        assertThat(rootPackage3.equals(DEFAULT_BASE_PKG + PERIOD + VERSION_NUMBER
                + PERIOD + VALID_NAME_SPACE3 + PERIOD + DATE_WITH_REV1), is(true));

    }

    /**
     * Unit test for root package generation without complexity in revision.
     */
    @Test
    public void getRootPackageWithRevTest() {
        String rootPkgWithRev = getRootPackage((byte) 1, CHILD_PACKAGE, DATE2, null);
        assertThat(rootPkgWithRev.equals(
                DEFAULT_BASE_PKG + PERIOD + VERSION_NUMBER + PERIOD + CHILD_WITH_PERIOD + PERIOD + DATE_WITH_REV2),
                is(true));
    }

    /**
     * Unit test for capitalizing the incoming string.
     */
    @Test
    public void getCapitalCaseTest() {
        String capitalCase = getCapitalCase(WITHOUT_CAPITAL);
        assertThat(capitalCase.equals(WITH_CAPITAL), is(true));
    }

    /**
     * Unit test for getting the camel case for the received string.
     */
    @Test
    public void getCamelCaseTest() {
        conflictResolver.setPrefixForIdentifier(null);
        String camelCase = getCamelCase(WITHOUT_CAMEL_CASE, conflictResolver);
        assertThat(camelCase.equals(WITH_CAMEL_CASE), is(true));
        String camelCase1 = getCamelCase(WITHOUT_CAMEL_CASE1, conflictResolver);
        assertThat(camelCase1.equals(WITH_CAMEL_CASE1), is(true));
        String camelCase2 = getCamelCase(WITHOUT_CAMEL_CASE2, conflictResolver);
        assertThat(camelCase2.equals(WITH_CAMEL_CASE2), is(true));
        String camelCase3 = getCamelCase(WITHOUT_CAMEL_CASE3, conflictResolver);
        assertThat(camelCase3.equals(WITH_CAMEL_CASE3), is(true));
        String camelCase4 = getCamelCase(WITHOUT_CAMEL_CASE4, conflictResolver);
        assertThat(camelCase4.equals(WITH_CAMEL_CASE4), is(true));
        String camelCase5 = getCamelCase(WITHOUT_CAMEL_CASE5, conflictResolver);
        assertThat(camelCase5.equals(WITH_CAMEL_CASE5), is(true));
        String camelCase6 = getCamelCase(WITHOUT_CAMEL_CASE6, conflictResolver);
        assertThat(camelCase6.equals(WITH_CAMEL_CASE6), is(true));
        String camelCase7 = getCamelCase(WITHOUT_CAMEL_CASE7, conflictResolver);
        assertThat(camelCase7.equals(WITH_CAMEL_CASE7), is(true));
        String camelCase8 = getCamelCase(WITHOUT_CAMEL_CASE8, conflictResolver);
        assertThat(camelCase8.equals(WITH_CAMEL_CASE8), is(true));
        String camelCase9 = getCamelCase(WITHOUT_CAMEL_CASE9, conflictResolver);
        assertThat(camelCase9.equals(WITH_CAMEL_CASE9), is(true));
        String camelCase10 = getCamelCase(WITHOUT_CAMEL_CASE10, conflictResolver);
        assertThat(camelCase10.equals(WITH_CAMEL_CASE10), is(true));
        String camelCase11 = getCamelCase(WITHOUT_CAMEL_CASE11, conflictResolver);
        assertThat(camelCase11.equals(WITH_CAMEL_CASE11), is(true));
        String camelCase12 = getCamelCase(WITHOUT_CAMEL_CASE12, conflictResolver);
        assertThat(camelCase12.equals(WITH_CAMEL_CASE12), is(true));
        String camelCase13 = getCamelCase(WITHOUT_CAMEL_CASE13, conflictResolver);
        assertThat(camelCase13.equals(WITH_CAMEL_CASE13), is(true));
        String camelCase14 = getCamelCase(WITHOUT_CAMEL_CASE14, conflictResolver);
        assertThat(camelCase14.equals(WITH_CAMEL_CASE14), is(true));
    }

    /**
     * Unit test for getting the camel case along with the prefix provided.
     */
    @Test
    public void getCamelCaseWithPrefixTest() {

        conflictResolver.setPrefixForIdentifier(VALID_PREFIX);
        String camelCase = getCamelCase(WITHOUT_CAMEL_CASE2, conflictResolver);
        assertThat(camelCase.equals(WITH_CAMEL_CASE_WITH_PREFIX), is(true));
        conflictResolver.setPrefixForIdentifier(INVALID_PREFIX1);
        String camelCase2 = getCamelCase(WITHOUT_CAMEL_CASE3, conflictResolver);
        assertThat(camelCase2.equals(WITH_CAMEL_CASE_WITH_PREFIX1), is(true));
    }

    /**
     * Unit test for getting the camel case along with the invalid prefix provided.
     */
    @Test
    public void getCamelCaseWithInvalidPrefixTest() throws TranslatorException {

        thrown.expect(TranslatorException.class);
        thrown.expectMessage("The given prefix in pom.xml is invalid.");
        conflictResolver.setPrefixForIdentifier(INVALID_PREFIX);
        String camelCase = getCamelCase(WITHOUT_CAMEL_CASE3, conflictResolver);
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
     * This test  case checks whether the package is existing.
     *
     * @throws IOException when failed to create a test file
     */
    @Test
    public void packageExistTest() throws IOException {

        String strPath = BASE_DIR_PKG + DIR_PATH;
        File createDir = new File(strPath.replace(PERIOD, SLASH));
        createDir.mkdirs();
        File createFile = new File(createDir + SLASH + PKG_INFO);
        createFile.createNewFile();
        assertThat(true, is(doesPackageExist(strPath)));
        createDir.delete();
        deleteDirectory(createDir);
    }
}
