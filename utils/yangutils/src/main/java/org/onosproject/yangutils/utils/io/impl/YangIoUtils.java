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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import org.apache.commons.io.FileUtils;
import org.apache.maven.model.Resource;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.sonatype.plexus.build.incremental.BuildContext;

import org.onosproject.yangutils.plugin.manager.YangFileInfo;
import static org.onosproject.yangutils.utils.UtilConstants.COMMA;
import static org.onosproject.yangutils.utils.UtilConstants.EMPTY_STRING;
import static org.onosproject.yangutils.utils.UtilConstants.NEW_LINE;
import static org.onosproject.yangutils.utils.UtilConstants.ORG;
import static org.onosproject.yangutils.utils.UtilConstants.PACKAGE;
import static org.onosproject.yangutils.utils.UtilConstants.SEMI_COLAN;
import static org.onosproject.yangutils.utils.UtilConstants.SLASH;
import static org.onosproject.yangutils.utils.UtilConstants.SPACE;
import static org.onosproject.yangutils.utils.UtilConstants.TEMP;
import static org.onosproject.yangutils.utils.UtilConstants.TWELVE_SPACE_INDENTATION;
import static org.onosproject.yangutils.utils.UtilConstants.YANG_RESOURCES;
import static org.onosproject.yangutils.utils.io.impl.FileSystemUtil.appendFileContents;
import static org.onosproject.yangutils.utils.io.impl.FileSystemUtil.updateFileHandle;
import static org.onosproject.yangutils.utils.io.impl.JavaDocGen.getJavaDoc;
import static org.onosproject.yangutils.utils.io.impl.JavaDocGen.JavaDocType.PACKAGE_INFO;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Represents common utility functionalities for code generation.
 */
public final class YangIoUtils {

    private static final Logger log = getLogger(YangIoUtils.class);
    private static final String TARGET_RESOURCE_PATH = SLASH + TEMP + SLASH + YANG_RESOURCES + SLASH;

    /**
     * Creates an instance of YANG io utils.
     */
    private YangIoUtils() {
    }

    /**
     * Creates the directory structure.
     *
     * @param path directory path
     * @return directory structure
     */
    public static File createDirectories(String path) {
        File generatedDir = new File(path);
        generatedDir.mkdirs();
        return generatedDir;
    }

    /**
     * Adds package info file for the created directory.
     *
     * @param path directory path
     * @param classInfo class info for the package
     * @param pack package of the directory
     * @param isChildNode is it a child node
     * @throws IOException when fails to create package info file
     */
    public static void addPackageInfo(File path, String classInfo, String pack, boolean isChildNode)
            throws IOException {

        if (pack.contains(ORG)) {
            String[] strArray = pack.split(ORG);
            pack = ORG + strArray[1];
        }
        try {

            File packageInfo = new File(path + SLASH + "package-info.java");
            packageInfo.createNewFile();

            FileWriter fileWriter = new FileWriter(packageInfo);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);

            bufferedWriter.write(CopyrightHeader.getCopyrightHeader());
            bufferedWriter.write(getJavaDoc(PACKAGE_INFO, classInfo, isChildNode));
            bufferedWriter.write(PACKAGE + SPACE + pack + SEMI_COLAN);

            bufferedWriter.close();
            fileWriter.close();
        } catch (IOException e) {
            throw new IOException("Exception occured while creating package info file.");
        }
    }

    /**
     * Cleans the generated directory if already exist in source folder.
     *
     * @param dir generated directory in previous build
     * @throws IOException when failed to delete directory
     */
    public static void deleteDirectory(String dir) throws IOException {
        File generatedDirectory = new File(dir);
        if (generatedDirectory.exists()) {
            try {
                FileUtils.deleteDirectory(generatedDirectory);
            } catch (IOException e) {
                throw new IOException(
                        "Failed to delete the generated files in " + generatedDirectory + " directory");
            }
        }
    }

    /**
     * Searches and deletes generated temporary directories.
     *
     * @param root root directory
     * @throws IOException when fails to do IO operations.
     */
    public static void searchAndDeleteTempDir(String root) throws IOException {
        List<File> store = new LinkedList<>();
        Stack<String> stack = new Stack<>();
        stack.push(root);

        while (!stack.empty()) {
            root = stack.pop();
            File file = new File(root);
            File[] filelist = file.listFiles();
            if (filelist == null || filelist.length == 0) {
                continue;
            }
            for (File current : filelist) {
                if (current.isDirectory()) {
                    stack.push(current.toString());
                    if (current.getName().endsWith("-Temp")) {
                        store.add(current);
                    }
                }
            }
        }

        for (File dir : store) {
            FileUtils.deleteDirectory(dir);
        }
    }

    /**
     * Adds generated source directory to the compilation root.
     *
     * @param source directory
     * @param project current maven project
     * @param context current build context
     */
    public static void addToSource(String source, MavenProject project, BuildContext context) {
        project.addCompileSourceRoot(source);
        context.refresh(project.getBasedir());
        log.info("Source directory added to compilation root: " + source);
    }

    /**
     * Removes extra char from the string.
     *
     * @param valueString string to be trimmed
     * @param removealStirng extra chars
     * @return new string
     */
    public static String trimAtLast(String valueString, String removealStirng) {
        StringBuilder stringBuilder = new StringBuilder(valueString);
        int index = valueString.lastIndexOf(removealStirng);
        stringBuilder.deleteCharAt(index);
        return stringBuilder.toString();
    }

    /**
     * Returns new parted string.
     *
     * @param partString string to be parted
     * @return parted string
     */
    public static String partString(String partString) {
        String[] strArray = partString.split(COMMA);
        String newString = EMPTY_STRING;
        for (int i = 0; i < strArray.length; i++) {
            if (i % 4 != 0 || i == 0) {
                newString = newString + strArray[i] + COMMA;
            } else {
                newString = newString + NEW_LINE + TWELVE_SPACE_INDENTATION
                        + strArray[i] + COMMA;
            }
        }
        return trimAtLast(newString, COMMA);
    }

    /**
     * Returns the directory path of the package in canonical form.
     *
     * @param baseCodeGenPath base path where the generated files needs to be
     *            put
     * @param pathOfJavaPkg java package of the file being generated
     * @return absolute path of the package in canonical form
     */
    public static String getDirectory(String baseCodeGenPath, String pathOfJavaPkg) {

        if (pathOfJavaPkg.charAt(pathOfJavaPkg.length() - 1) == File.separatorChar) {
            pathOfJavaPkg = trimAtLast(pathOfJavaPkg, SLASH);
        }
        String[] strArray = pathOfJavaPkg.split(SLASH);
        if (strArray[0].equals(EMPTY_STRING)) {
            return pathOfJavaPkg;
        } else {
            return baseCodeGenPath + SLASH + pathOfJavaPkg;
        }
    }

    /**
     * Returns the absolute path of the package in canonical form.
     *
     * @param baseCodeGenPath base path where the generated files needs to be
     *            put
     * @param pathOfJavaPkg java package of the file being generated
     * @return absolute path of the package in canonical form
     */
    public static String getAbsolutePackagePath(String baseCodeGenPath, String pathOfJavaPkg) {
        return baseCodeGenPath + pathOfJavaPkg;
    }

    /**
     * Copies YANG files to the current project's output directory.
     *
     * @param yangFileInfo list of YANG files
     * @param outputDir project's output directory
     * @param project maven project
     * @throws IOException when fails to copy files to destination resource directory
     */
    public static void copyYangFilesToTarget(List<YangFileInfo> yangFileInfo, String outputDir, MavenProject project)
            throws IOException {

        List<File> files = getListOfFile(yangFileInfo);

        String path = outputDir + TARGET_RESOURCE_PATH;
        File targetDir = new File(path);
        targetDir.mkdirs();

        for (File file : files) {
            Files.copy(file.toPath(),
                    new File(path + file.getName()).toPath(),
                    StandardCopyOption.REPLACE_EXISTING);
        }
        Resource rsc = new Resource();
        rsc.setDirectory(outputDir + SLASH + TEMP + SLASH);
        project.addResource(rsc);
    }

    /**
     * Provides a list of files from list of strings.
     *
     * @param yangFileInfo list of yang file information
     * @return list of files
     */
    private static List<File> getListOfFile(List<YangFileInfo> yangFileInfo) {
        List<File> files = new ArrayList<>();
        Iterator<YangFileInfo> yangFileIterator = yangFileInfo.iterator();
        while (yangFileIterator.hasNext()) {
            YangFileInfo yangFile = yangFileIterator.next();
            files.add(new File(yangFile.getYangFileName()));
        }
        return files;
    }

    /**
     * Merges the temp java files to main java files.
     *
     * @param appendFile temp file
     * @param srcFile main file
     * @throws IOException when fails to append contents
     */
    public static void mergeJavaFiles(File appendFile, File srcFile) throws IOException {
        try {
            appendFileContents(appendFile, srcFile);
        } catch (IOException e) {
            throw new IOException("Failed to append " + appendFile + " in " + srcFile);
        }
    }

    /**
     * Inserts data in the generated file.
     *
     * @param file file in which need to be inserted
     * @param data data which need to be inserted
     * @throws IOException when fails to insert into file
     */
    public static void insertDataIntoJavaFile(File file, String data) throws IOException {
        try {
            updateFileHandle(file, data, false);
        } catch (IOException e) {
            throw new IOException("Failed to insert in " + file + "file");
        }
    }
}
