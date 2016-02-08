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

import java.io.IOException;

import org.junit.Test;
import java.io.File;
import java.util.List;
import org.slf4j.Logger;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Unit tests for searching yang files.
 */
public class YangFileScannerTest {

    private final Logger log = getLogger(getClass());

    String baseDir = "target/UnitTestCase";

    /**
     * Checks an empty directory.
     */
    @Test
    public void testWithSingleEmptyDirectoryInRoot() {
        try {
            File dir = new File(baseDir);
            dir.mkdirs();
            List<String> list = YangFileScanner.getYangFiles(baseDir.toString());
        } catch (IOException e) {
            log.info("IO Exception throwed");
        }
    }

    /**
     * Checks multiple empty directories in root directory.
     */
    @Test
    public void testWithMultiEmptyDirectoriesInRoot() {
        try {
            String dir = "emptyDir";
            String dir1 = "emptyDir1";
            String dir2 = "emptyDir2";
            String dir3 = "emptyDir3";
            String dir4 = "emptyDir4";
            File firstpath = createDirectory(dir);
            File firstpath1 = createDirectory(dir1);
            File firstpath2 = createDirectory(dir2);
            File firstpath3 = createDirectory(dir3);
            File firstpath4 = createDirectory(dir4);
            List<String> list = YangFileScanner.getYangFiles(baseDir.toString());
        } catch (IOException e) {
            log.info("IO Exception throwed");
        }
    }

    /**
     * Checks one directory with one .yang file.
     */
    @Test
    public void testWithSingleDirectorySingleFileInRoot() {
        try {
            String dir1 = "level1";
            String firstFileName1 = "secondFile.yang";
            File firstpath1 = createDirectory(dir1);
            createFile(firstpath1, firstFileName1);
            List<String> list = YangFileScanner.getYangFiles(baseDir.toString());
        } catch (IOException e) {
            log.info("IO Exception throwed");
        }
    }

    /**
     * Checks one directory with many .yang file.
     */
    @Test
    public void testWithSingleDirectoryMultiFilesInRoot() {
        try {
            String dir2 = "level2";
            String firstFileName2 = "thirdFile.yang";
            String firstFileName3 = "fourthFile.yang";
            String firstFileName4 = "fifthFile.yang";
            String firstFileName5 = "sixthFile.yang";
            File firstpath2 = createDirectory(dir2);
            createFile(firstpath2, firstFileName2);
            createFile(firstpath2, firstFileName3);
            createFile(firstpath2, firstFileName4);
            createFile(firstpath2, firstFileName5);
            List<String> list = YangFileScanner.getYangFiles(baseDir.toString());
        } catch (IOException e) {
            log.info("IO Exception throwed");
        }
    }

    /**
     * Checks multi directories with many .yang file.
     */
    @Test
    public void testWithMultiDirectoriesMultiFiles() {
        try {
            String dir2 = "newDir1/newDir2/newDir3/newDir4";
            File dir3 = new File("target/UnitTestCase/newDir1");
            File dir4 = new File("target/UnitTestCase/newDir1/newDir2");
            File dir5 = new File("target/UnitTestCase/newDir1/newDir2/newDir3");
            File dir6 = new File("target/UnitTestCase/newDir1/newDir2/newDir3/newDir4");
            String firstFileName2 = "thirdFile.yang";
            String firstFileName3 = "fourthFile.yang";
            String firstFileName4 = "fifthFile.yang";
            String firstFileName5 = "sixthFile.yang";
            File firstpath2 = createDirectory(dir2);
            createFile(firstpath2, firstFileName2);
            createFile(firstpath2, firstFileName3);
            createFile(firstpath2, firstFileName4);
            createFile(dir3, firstFileName5);
            createFile(dir3, firstFileName2);
            createFile(dir3, firstFileName3);
            createFile(dir3, firstFileName4);
            createFile(dir3, firstFileName5);
            createFile(dir4, firstFileName2);
            createFile(dir4, firstFileName3);
            createFile(dir4, firstFileName4);
            createFile(dir4, firstFileName5);
            createFile(dir5, firstFileName2);
            createFile(dir5, firstFileName3);
            createFile(dir5, firstFileName4);
            createFile(dir5, firstFileName5);
            createFile(dir6, firstFileName2);
            createFile(dir6, firstFileName3);
            createFile(dir6, firstFileName4);
            createFile(dir6, firstFileName5);
            List<String> list = YangFileScanner.getYangFiles(baseDir.toString());
        } catch (IOException e) {
            log.info("IO Exception throwed");
        }
    }

    /**
     * Method used for creating multiple directories inside the target file.
     *
     * @param path directory path
     * @return directory path
     */
    public File createDirectory(String path) {
        File myDir = new File(baseDir + File.separator + path);
        myDir.mkdirs();
        return myDir;
    }

    /**
    * Method used for creating file inside the specified directory.
    *
    * @param myDir my current dirctory
    * @param fileName file name
    * @throws IOException io exception when fails to create a file.
    */
    public void createFile(File myDir, String fileName) throws IOException {
        File file = null;
        try {
            file = new File(myDir + File.separator + fileName);
            file.createNewFile();
        } catch (final IOException e) {
            throw new IOException("IOException occured");
        }
    }
}
