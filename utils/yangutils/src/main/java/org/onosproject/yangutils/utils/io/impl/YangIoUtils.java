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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.maven.model.Resource;
import org.apache.maven.project.MavenProject;
import org.onosproject.yangutils.utils.UtilConstants;
import org.slf4j.Logger;
import org.sonatype.plexus.build.incremental.BuildContext;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provides common utility functionalities for code generation.
 */
public final class YangIoUtils {

    private static final Logger log = getLogger(YangIoUtils.class);
    private static final String TARGET_RESOURCE_PATH = UtilConstants.SLASH + UtilConstants.TEMP + UtilConstants.SLASH
            + UtilConstants.YANG_RESOURCES + UtilConstants.SLASH;

    /**
     * Default constructor.
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
     * @throws IOException when fails to create package info file
     */
    public static void addPackageInfo(File path, String classInfo, String pack) throws IOException {

        if (pack.contains(UtilConstants.ORG)) {
            String[] strArray = pack.split(UtilConstants.ORG);
            pack = UtilConstants.ORG + strArray[1];
        }
        try {

            File packageInfo = new File(path + File.separator + "package-info.java");
            packageInfo.createNewFile();
            FileWriter fileWriter = null;
            BufferedWriter bufferedWriter = null;
            fileWriter = new FileWriter(packageInfo);
            bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.write(CopyrightHeader.getCopyrightHeader());
            bufferedWriter.write(JavaDocGen.getJavaDoc(JavaDocGen.JavaDocType.PACKAGE_INFO, classInfo, false));
            bufferedWriter.write(UtilConstants.PACKAGE + UtilConstants.SPACE + pack + UtilConstants.SEMI_COLAN);
            bufferedWriter.close();
        } catch (IOException e) {
            throw new IOException("Exception occured while creating package info file.");
        }
    }

    /**
     * Cleans the generated directory if already exist in source folder.
     *
     * @param dir generated directory in previous build
     */
    public static void clean(String dir) {

        File generatedDirectory = new File(dir);
        if (generatedDirectory.exists()) {
            try {
                FileUtils.deleteDirectory(generatedDirectory);
            } catch (IOException e) {
                log.info("Failed to delete the generated files in " + generatedDirectory + " directory");
            }
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

        String[] strArray = partString.split(UtilConstants.COMMA);
        String newString = "";
        for (int i = 0; i < strArray.length; i++) {
            if (i % 4 != 0 || i == 0) {
                newString = newString + strArray[i] + UtilConstants.COMMA;
            } else {
                newString = newString + UtilConstants.NEW_LINE + UtilConstants.TWELVE_SPACE_INDENTATION
                        + strArray[i]
                        + UtilConstants.COMMA;
            }
        }
        return trimAtLast(newString, UtilConstants.COMMA);
    }

    /**
     * Returns backspaced string.
     *
     * @param charString char string
     * @return backspace string
     */
    public static String deleteLastChar(String charString) {

        return charString.substring(0, charString.length() - 1);
    }

    /**
     * Get the directory path of the package in canonical form.
     *
     * @param baseCodeGenPath base path where the generated files needs to be
     *            put.
     * @param pathOfJavaPkg java package of the file being generated
     * @return absolute path of the package in canonical form
     */
    public static String getDirectory(String baseCodeGenPath, String pathOfJavaPkg) {

        if (pathOfJavaPkg.charAt(pathOfJavaPkg.length() - 1) == File.separatorChar) {
            pathOfJavaPkg = trimAtLast(pathOfJavaPkg, UtilConstants.SLASH);
        }
        String[] strArray = pathOfJavaPkg.split(UtilConstants.SLASH);
        if (strArray[0].equals(UtilConstants.EMPTY_STRING)) {
            return pathOfJavaPkg;
        } else {
            return baseCodeGenPath + File.separator + pathOfJavaPkg;
        }
    }

    /**
     * Get the absolute path of the package in canonical form.
     *
     * @param baseCodeGenPath base path where the generated files needs to be
     *            put.
     * @param pathOfJavaPkg java package of the file being generated
     * @return absolute path of the package in canonical form
     */
    public static String getAbsolutePackagePath(String baseCodeGenPath, String pathOfJavaPkg) {

        return baseCodeGenPath + pathOfJavaPkg;
    }

    /**
     * Copy YANG files to the current project's output directory.
     *
     * @param yangFiles list of YANG files
     * @param outputDir project's output directory
     * @param project maven project
     * @throws IOException when fails to copy files to destination resource
     *             directory
     */
    public static void copyYangFilesToTarget(List<String> yangFiles, String outputDir, MavenProject project)
            throws IOException {

        List<File> files = getListOfFile(yangFiles);

        String path = outputDir + TARGET_RESOURCE_PATH;
        File targetDir = new File(path);
        targetDir.mkdirs();

        for (File file : files) {
            Files.copy(file.toPath(),
                    new File(path + file.getName()).toPath(),
                    StandardCopyOption.REPLACE_EXISTING);
        }
        Resource rsc = new Resource();
        rsc.setDirectory(outputDir + UtilConstants.SLASH + UtilConstants.TEMP + UtilConstants.SLASH);
        project.addResource(rsc);
    }

    /**
     * Provides a list of files from list of strings.
     *
     * @param strings list of strings
     * @return list of files
     */
    private static List<File> getListOfFile(List<String> strings) {

        List<File> files = new ArrayList<>();
        for (String file : strings) {
            files.add(new File(file));
        }
        return files;
    }
}
