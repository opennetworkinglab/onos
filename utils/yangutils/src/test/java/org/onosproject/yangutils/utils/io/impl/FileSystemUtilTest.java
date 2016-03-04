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

import static org.slf4j.LoggerFactory.getLogger;

import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.onosproject.yangutils.translator.GeneratedFileType;
import org.onosproject.yangutils.utils.UtilConstants;
import org.slf4j.Logger;


/**
 * Tests the file handle utilities.
 */
public final class FileSystemUtilTest {

    public static String baseDirPkg = "target.UnitTestCase.";
    public static String packageInfoContent = "testGeneration6";
    public static String baseDir = "target/UnitTestCase";

    private final Logger log = getLogger(getClass());

    @Rule
    public ExpectedException thrown = ExpectedException.none();

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

        Class<?>[] classesToConstruct = {FileSystemUtil.class};
        for (Class<?> clazz : classesToConstruct) {
            Constructor<?> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            assertNotNull(constructor.newInstance());
        }
    }

    /**
     * This test case checks the creation of source files.
     */
    @Test
    public void createSourceFilesTest() throws IOException {

        FileSystemUtil.createSourceFiles(baseDirPkg + "srcFile1", packageInfoContent, GeneratedFileType.INTERFACE_MASK);
    }

    /**
     * This test case checks the contents to be written in the file.
     */
    @Test
    public void updateFileHandleTest() throws IOException {
        File dir = new File(baseDir + File.separator + "File1");
        dir.mkdirs();
        File createFile = new File(dir + "testFile");
        createFile.createNewFile();
        File createSourceFile = new File(dir + "sourceTestFile");
        createSourceFile.createNewFile();
        FileSystemUtil.updateFileHandle(createFile, "This is to append a text to the file first1\n", false);
        FileSystemUtil.updateFileHandle(createFile, "This is next second line\n", false);
        FileSystemUtil.updateFileHandle(createFile, "This is next third line in the file", false);
        FileSystemUtil.appendFileContents(createFile, createSourceFile);
        FileSystemUtil.updateFileHandle(createFile, null, true);
    }

    /**
     * This test  case checks whether the package is existing.
     */
    @Test
    public void packageExistTest() throws IOException {
        String dirPath = "exist1.exist2.exist3";
        String strPath = baseDirPkg + dirPath;
        File createDir = new File(strPath.replace(UtilConstants.PERIOD, UtilConstants.SLASH));
        createDir.mkdirs();
        File createFile = new File(createDir + File.separator + "package-info.java");
        createFile.createNewFile();
        assertTrue(FileSystemUtil.doesPackageExist(strPath));
        FileSystemUtil.createPackage(strPath, packageInfoContent);
        createDir.delete();
    }

    /**
     * This test case checks the package does not exist.
     */
    @Test
    public void packageNotExistTest() throws IOException {
        String dirPath = "notexist1.notexist2";
        String strPath = baseDirPkg + dirPath;
        File createDir = new File(strPath.replace(UtilConstants.PERIOD, UtilConstants.SLASH));
        assertFalse(FileSystemUtil.doesPackageExist(strPath));
        createDir.mkdirs();
        assertFalse(FileSystemUtil.doesPackageExist(strPath));
        CopyrightHeader.parseCopyrightHeader();
        FileSystemUtil.createPackage(strPath, packageInfoContent);
        assertTrue(FileSystemUtil.doesPackageExist(strPath));
        createDir.delete();
    }
}
