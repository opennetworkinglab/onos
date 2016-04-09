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

package org.onosproject.yangutils.utils.io.impl;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.apache.maven.project.MavenProject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.onosproject.yangutils.utils.UtilConstants;
import org.sonatype.plexus.build.incremental.BuildContext;
import org.sonatype.plexus.build.incremental.DefaultBuildContext;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;
import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.addPackageInfo;
import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.addToSource;
import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.clean;
import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.createDirectories;
import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.trimAtLast;

/**
 * Unit tests for YANG IO utils.
 */
public final class YangIoUtilsTest {

    private static final String BASE_DIR = "target/UnitTestCase";
    private static final String CREATE_PATH = BASE_DIR + File.separator + "dir1/dir2/dir3/dir4/";
    private static final String CHECK_STRING = "one, two, three, four, five, six";
    private static final String TRIM_STRING = "one, two, three, four, five, ";

    /**
     * Expected exceptions.
     */
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /**
     * This test case checks whether the package-info file is created.
     *
     * @throws IOException when fails to do IO operations for test case
     */
    @Test
    public void addPackageInfoTest() throws IOException {

        File dirPath = new File(CREATE_PATH);
        dirPath.mkdirs();
        addPackageInfo(dirPath, "check1", CREATE_PATH);
        File filePath = new File(dirPath + File.separator + "package-info.java");
        assertThat(filePath.isFile(), is(true));
    }

    /**
     * This test case checks with an additional info in the path.
     *
     * @throws IOException when fails to do IO operations for test case
     */
    @Test
    public void addPackageInfoWithPathTest() throws IOException {

        File dirPath = new File(CREATE_PATH);
        dirPath.mkdirs();
        addPackageInfo(dirPath, "check1", "src/main/yangmodel/" + CREATE_PATH);
        File filePath = new File(dirPath + File.separator + "package-info.java");
        assertThat(filePath.isFile(), is(true));
    }

    /**
     * This test case checks whether the package-info file is created when invalid path is given.
     *
     * @throws IOException when fails to do IO operations for test case
     */
    @Test
    public void addPackageInfoWithEmptyPathTest() throws IOException {

        File dirPath = new File("invalid/check");
        thrown.expect(IOException.class);
        thrown.expectMessage("Exception occured while creating package info file.");
        addPackageInfo(dirPath, "check1", CREATE_PATH);
        File filePath1 = new File(dirPath + File.separator + "package-info.java");
        assertThat(filePath1.isFile(), is(false));
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

        Class<?>[] classesToConstruct = {YangIoUtils.class };
        for (Class<?> clazz : classesToConstruct) {
            Constructor<?> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            assertThat(null, not(constructor.newInstance()));
        }
    }

    /**
     * This test case checks if the directory is cleaned.
     *
     * @throws IOException when fails to do IO operations for test case
     */
    @Test
    public void cleanGeneratedDirTest() throws IOException {

        File baseDirPath = new File(BASE_DIR);
        File createNewDir = new File(BASE_DIR + File.separator + UtilConstants.YANG_GEN_DIR);
        createNewDir.mkdirs();
        File createFile = new File(createNewDir + File.separator + "check1.java");
        createFile.createNewFile();
        clean(baseDirPath.getAbsolutePath());
    }

    /**
     * This test case checks the cleaning method when an invalid path is provided.
     *
     * @throws IOException when fails to do IO operations for test case
     */
    @Test
    public void cleanWithInvalidDirTest() throws IOException {

        File baseDirPath = new File(BASE_DIR + "invalid");
        clean(baseDirPath.getAbsolutePath());
    }

    /**
     * This test case tests whether the directories are getting created.
     */
    @Test
    public void createDirectoryTest() {

        File dirPath = createDirectories(CREATE_PATH);
        assertThat(dirPath.isDirectory(), is(true));
    }

    /**
     * This test case checks whether the source is getting added.
     */
    @Test
    public void testForAddSource() {

        MavenProject project = new MavenProject();
        BuildContext context = new DefaultBuildContext();
        File sourceDir = new File(BASE_DIR + File.separator + "yang");
        sourceDir.mkdirs();
        addToSource(sourceDir.toString(), project, context);
    }

    /*
     * Unit test case for trim at last method.
     */
    @Test
    public void testForTrimAtLast() {

        String test = trimAtLast(CHECK_STRING, "six");
        assertThat(test.contains(TRIM_STRING), is(true));
    }

}
