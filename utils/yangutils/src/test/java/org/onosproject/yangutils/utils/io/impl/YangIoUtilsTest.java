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
import org.onosproject.yangutils.utils.UtilConstants;

import java.io.File;
import java.io.IOException;

import org.apache.maven.project.MavenProject;
import org.sonatype.plexus.build.incremental.BuildContext;
import org.sonatype.plexus.build.incremental.DefaultBuildContext;

import org.slf4j.Logger;
import static org.slf4j.LoggerFactory.getLogger;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.hamcrest.core.Is.is;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Unit tests for YANG io utils.
 */
public final class YangIoUtilsTest {

    public static String baseDir = "target/UnitTestCase";

    public static String createPath = baseDir + File.separator + "dir1/dir2/dir3/dir4/";

    private final Logger log = getLogger(getClass());

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /**
     * This test case checks whether the package-info file is created.
     */
    @Test
    public void addPackageInfoTest() throws IOException {

        File dirPath = new File(createPath);
        dirPath.mkdirs();
        CopyrightHeader.parseCopyrightHeader();
        YangIoUtils.addPackageInfo(dirPath, "check1", createPath);
        File filePath = new File(dirPath + File.separator + "package-info.java");
        assertThat(filePath.isFile(), is(true));
    }

    /**
     * This test case checks with an additional info in the path.
     */
    @Test
    public void addPackageInfoWithPathTest() throws IOException {

        File dirPath = new File(createPath);
        dirPath.mkdirs();
        CopyrightHeader.parseCopyrightHeader();
        YangIoUtils.addPackageInfo(dirPath, "check1", "src/main/yangmodel/" + createPath);
        File filePath = new File(dirPath + File.separator + "package-info.java");
        assertThat(filePath.isFile(), is(true));
    }

    /**
     * This test case checks whether the package-info file is created when invalid path is given.
     */
    @Test
    public void addPackageInfoWithEmptyPathTest() throws IOException {

        File dirPath = new File("invalid/check");
        thrown.expect(IOException.class);
        thrown.expectMessage("Exception occured while creating package info file.");
        YangIoUtils.addPackageInfo(dirPath, "check1", createPath);
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
            assertNotNull(constructor.newInstance());
        }
    }

    /**
     * This test case checks if the directory is cleaned.
     */
    @Test
    public void cleanGeneratedDirTest() throws IOException {

        File baseDirPath = new File(baseDir);
        File createNewDir = new File(baseDir + File.separator + UtilConstants.YANG_GEN_DIR);
        createNewDir.mkdirs();
        File createFile = new File(createNewDir + File.separator + "check1.java");
        createFile.createNewFile();
        YangIoUtils.clean(baseDirPath.getAbsolutePath());
    }

    /**
     * This test case checks the cleaning method when an invalid path is provided.
     */
    @Test
    public void cleanWithInvalidDirTest() throws IOException {

        File baseDirPath = new File(baseDir + "invalid");
        YangIoUtils.clean(baseDirPath.getAbsolutePath());
    }

    /**
     * This test case tests whether the directories are getting created.
     */
    @Test
    public void createDirectoryTest() {

        File dirPath = YangIoUtils.createDirectories(createPath);
        assertThat(dirPath.isDirectory(), is(true));
    }

    /**
     * This testcase checks whether the source is getting added.
     */
    @Test
    public void testForAddSource() {

        MavenProject project = new MavenProject();
        BuildContext context = new DefaultBuildContext();
        File sourceDir = new File(baseDir + File.separator + "yang");
        sourceDir.mkdirs();
        YangIoUtils.addToSource(sourceDir.toString(), project, context);
    }
}
