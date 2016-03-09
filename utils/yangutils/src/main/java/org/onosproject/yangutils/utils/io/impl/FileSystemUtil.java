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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import org.onosproject.yangutils.translator.CachedFileHandle;
import org.onosproject.yangutils.translator.tojava.CachedJavaFileHandle;
import org.onosproject.yangutils.translator.tojava.utils.JavaIdentifierSyntax;
import org.onosproject.yangutils.utils.UtilConstants;

/**
 * Utility to handle file system operations.
 */
public final class FileSystemUtil {
    /**
     * Hiding constructor of a utility class.
     */
    private FileSystemUtil() {
    }

    /**
     * Check if the package directory structure created.
     *
     * @param pkg Package to check if it is created
     * @return existence status of package
     */
    public static boolean doesPackageExist(String pkg) {
        File pkgDir = new File(pkg.replace(UtilConstants.PERIOD, UtilConstants.SLASH));
        File pkgWithFile = new File(pkgDir + File.separator + "package-info.java");
        if (pkgDir.exists() && pkgWithFile.isFile()) {
            return true;
        }
        return false;
    }

    /**
     * Create a package structure with package info java file if not present.
     *
     * @param pkg java package string
     * @param pkgInfo description of package
     * @throws IOException any IO exception
     */
    public static void createPackage(String pkg, String pkgInfo) throws IOException {
        if (!doesPackageExist(pkg)) {
            try {
                File pack = YangIoUtils
                        .createDirectories(pkg.replace(UtilConstants.PERIOD, UtilConstants.SLASH));
                YangIoUtils.addPackageInfo(pack, pkgInfo, pkg);
            } catch (IOException e) {
                throw new IOException("failed to create package-info file");
            }
        }
    }

    /**
     * Create a java source file in the specified package.
     *
     * @param pkg java package under which the interface file needs to be
     *            created
     * @param yangName YANG name of the node for which java file needs to be
     *            created
     * @param types types of files to be created
     * @throws IOException when fails to create interface file
     * @return the cached java file handle, which can be used to further add
     *         methods
     */
    public static CachedFileHandle createSourceFiles(String pkg, String yangName, int types)
            throws IOException {
        yangName = JavaIdentifierSyntax.getCamelCase(yangName);
        CachedFileHandle handler = new CachedJavaFileHandle(pkg, yangName, types);

        return handler;
    }

    /**
     * Read the contents from source file and append its contents to append
     * file.
     *
     * @param toAppend destination file in which the contents of source file is
     *            appended
     * @param srcFile source file from which data is read and added to to append
     *            file
     * @throws IOException any IO errors
     */
    public static void appendFileContents(File toAppend, File srcFile) throws IOException {

        updateFileHandle(srcFile, UtilConstants.NEW_LINE + readAppendFile(toAppend.toString()), false);
        return;
    }

    /**
     * Reads file and convert it to string.
     *
     * @param toAppend file to be converted
     * @return string of file
     * @throws IOException when fails to convert to string
     */
    private static String readAppendFile(String toAppend) throws IOException {
        BufferedReader bufferReader = new BufferedReader(new FileReader(toAppend));
        try {
            StringBuilder stringBuilder = new StringBuilder();
            String line = bufferReader.readLine();

            while (line != null) {
                if (line.equals(UtilConstants.FOUR_SPACE_INDENTATION)
                        || line.equals(UtilConstants.EIGHT_SPACE_INDENTATION)
                        || line.equals(UtilConstants.SPACE) || line.equals("") || line.equals(UtilConstants.NEW_LINE)) {
                    stringBuilder.append("\n");
                } else {
                    stringBuilder.append(UtilConstants.FOUR_SPACE_INDENTATION + line);
                    stringBuilder.append("\n");
                }
                line = bufferReader.readLine();
            }
            return stringBuilder.toString();
        } finally {
            bufferReader.close();
        }
    }

    /**
     * Update the generated file handle.
     *
     * @param inputFile input file
     * @param contentTobeAdded content to be appended to the file
     * @param isClose when close of file is called.
     * @throws IOException when fails to append content to the file
     */
    public static void updateFileHandle(File inputFile, String contentTobeAdded, boolean isClose) throws IOException {
        FileWriter fileWriter = new FileWriter(inputFile, true);
        PrintWriter outputPrintWriter = new PrintWriter(fileWriter);
        if (!isClose) {
            outputPrintWriter.write(contentTobeAdded);
            outputPrintWriter.flush();
            outputPrintWriter.close();
        } else {
            fileWriter.flush();
            fileWriter.close();
        }
    }
}
