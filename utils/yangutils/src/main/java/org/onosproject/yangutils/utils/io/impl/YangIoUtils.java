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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.maven.model.Resource;
import org.apache.maven.project.MavenProject;
import org.onosproject.yangutils.utils.UtilConstants;
import org.slf4j.Logger;
import org.sonatype.plexus.build.incremental.BuildContext;

/**
 * Provides common utility functionalities for code generation.
 */
public final class YangIoUtils {

    private static final Logger log = getLogger(YangIoUtils.class);

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

        if (pack.contains(UtilConstants.YANG_GEN_DIR)) {
            String[] strArray = pack.split(UtilConstants.YANG_GEN_DIR);
            pack = strArray[1];
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
     * @param baseDir generated directory in previous build
     */
    public static void clean(String baseDir) {
        File generatedDirectory = new File(baseDir + File.separator + UtilConstants.YANG_GEN_DIR
                + UtilConstants.DEFAULT_BASE_PKG.replace(UtilConstants.PERIOD, UtilConstants.SLASH));
        if (generatedDirectory.exists()) {
            List<String> javafiles;
            try {
                javafiles = YangFileScanner.getJavaFiles(generatedDirectory.toString());
                for (String file : javafiles) {
                    File currentFile = new File(file);
                    currentFile.delete();
                }
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
        Resource rsc = new Resource();
        rsc.setDirectory(source);
        project.addResource(rsc);
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

}
