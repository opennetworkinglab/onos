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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;
import java.util.List;
import java.io.IOException;

import org.slf4j.Logger;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Test the file scanner service.
 */
public final class YangFileScannerTest {

    private final Logger log = getLogger(getClass());

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    String baseDir = "target/UnitTestCase";

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

        Class<?>[] classesToConstruct = {YangFileScanner.class };
        for (Class<?> clazz : classesToConstruct) {
            Constructor<?> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            assertNotNull(constructor.newInstance());
        }
    }

    /**
     * This test case checks for a .java file inside the specified dir.
     */
    @Test
    public void checkJavaFileInsideDirTest() throws IOException {

        String dir = baseDir + File.separator + "scanner2";
        File path = createDirectory(dir);
        createFile(path, "testScanner.java");
        List<String> dirContents = YangFileScanner.getJavaFiles(path.toString());
        List<String> expectedContents = new LinkedList<>();
        expectedContents.add(path.getCanonicalPath() + File.separator + "testScanner.java");
        assertEquals(dirContents, expectedContents);
    }

    /**
     * Method used for creating multiple directories inside the target file.
     *
     * @param path where directories should be created
     * @return the directory path that is created
     */
    public File createDirectory(String path) {

        File myDir = new File(path);
        myDir.mkdirs();
        return myDir;
    }

    /**
     * Method used for creating file inside the specified directory.
     *
     * @param myDir the path where file has to be created inside.
     * @param fileName the name of the file to be created.
     */
    public void createFile(File myDir, String fileName) throws IOException {

        File file = null;
        file = new File(myDir + File.separator + fileName);
        file.createNewFile();
    }

    /**
     * This testcase checks for a java file inside an empty directory.
     */
    @Test
    public void emptyDirJavaScannerTest() throws IOException {

        String emptyDir = baseDir + File.separator + "scanner1";
        File path = createDirectory(emptyDir);
        List<String> emptyDirContents = YangFileScanner.getJavaFiles(path.toString());
        List<String> expectedContents = new LinkedList<>();
        assertEquals(emptyDirContents, expectedContents);
    }

    /**
     * This testcase checks for a yang file inside an empty directory.
     */
    @Test
    public void emptyDirYangScannerTest() throws IOException {

        String emptyYangDir = baseDir + File.separator + "scanner1";
        File path = createDirectory(emptyYangDir);
        List<String> emptyDirContents = YangFileScanner.getYangFiles(path.toString());
        List<String> expectedContents = new LinkedList<>();
        assertEquals(emptyDirContents, expectedContents);
    }

    /**
     * This test case checks with the sub directories in the given path for java files.
     */
    @Test
    public void emptySubDirScannerTest() throws IOException {

        String dir = baseDir + File.separator + "scanner3";
        File path = createDirectory(dir);
        String subDir = path.toString() + File.separator + "subDir1";
        createDirectory(subDir);
        createFile(path, "invalidFile.txt");
        List<String> emptySubDirContents = YangFileScanner.getJavaFiles(path.toString());
        List<String> expectedContents = new LinkedList<>();
        assertEquals(emptySubDirContents, expectedContents);
    }

    /**
     * This test case checks with the sub directories in the given path for java files.
     */
    @Test
    public void exceptionHandleTest() throws IOException {

        String dir = baseDir + File.separator + "scanner4";
        thrown.expect(IOException.class);
        thrown.expectMessage("NullPointerException occured");
        List<String> invalidContents = YangFileScanner.getJavaFiles(dir);
        File path = createDirectory(dir);
        createFile(path, "except.java");
        List<String> dirWithFileName = YangFileScanner
                .getJavaFiles(path + File.separator + "except.java" + File.separator + "scanner5");
    }
}