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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Resource;
import org.apache.maven.project.MavenProject;
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.plugin.manager.YangFileInfo;
import org.onosproject.yangutils.translator.tojava.utils.YangPluginConfig;
import org.slf4j.Logger;
import org.sonatype.plexus.build.incremental.BuildContext;

import static org.onosproject.yangutils.translator.tojava.utils.JavaIdentifierSyntax.getCamelCase;
import static org.onosproject.yangutils.translator.tojava.utils.JavaIdentifierSyntax.getPackageDirPathFromJavaJPackage;
import static org.onosproject.yangutils.utils.UtilConstants.COMMA;
import static org.onosproject.yangutils.utils.UtilConstants.EMPTY_STRING;
import static org.onosproject.yangutils.utils.UtilConstants.HASH;
import static org.onosproject.yangutils.utils.UtilConstants.HYPHEN;
import static org.onosproject.yangutils.utils.UtilConstants.JAR;
import static org.onosproject.yangutils.utils.UtilConstants.NEW_LINE;
import static org.onosproject.yangutils.utils.UtilConstants.OPEN_PARENTHESIS;
import static org.onosproject.yangutils.utils.UtilConstants.ORG;
import static org.onosproject.yangutils.utils.UtilConstants.PACKAGE;
import static org.onosproject.yangutils.utils.UtilConstants.PERIOD;
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
    private static final int LINE_SIZE = 118;
    private static final int SUB_LINE_SIZE = 112;
    private static final int ZERO = 0;
    private static final String SERIALIZED_FILE_EXTENSION = ".ser";

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
     * @param path         directory path
     * @param classInfo    class info for the package
     * @param pack         package of the directory
     * @param isChildNode  is it a child node
     * @param pluginConfig plugin configurations
     * @throws IOException when fails to create package info file
     */
    public static void addPackageInfo(File path, String classInfo, String pack, boolean isChildNode,
            YangPluginConfig pluginConfig)
            throws IOException {

        pack = parsePkg(pack);

        try {

            File packageInfo = new File(path + SLASH + "package-info.java");
            packageInfo.createNewFile();

            FileWriter fileWriter = new FileWriter(packageInfo);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);

            bufferedWriter.write(CopyrightHeader.getCopyrightHeader());
            bufferedWriter.write(getJavaDoc(PACKAGE_INFO, classInfo, isChildNode, pluginConfig));
            String pkg = PACKAGE + SPACE + pack + SEMI_COLAN;
            if (pkg.length() > LINE_SIZE) {
                pkg = whenDelimiterIsPersent(pkg, LINE_SIZE);
            }
            bufferedWriter.write(pkg);
            bufferedWriter.close();
            fileWriter.close();
        } catch (IOException e) {
            throw new IOException("Exception occured while creating package info file.");
        }
    }

    /**
     * Parses package and returns updated package.
     *
     * @param pack package needs to be updated
     * @return updated package
     */
    public static String parsePkg(String pack) {

        if (pack.contains(ORG)) {
            String[] strArray = pack.split(ORG);
            if (strArray.length >= 3) {
                for (int i = 1; i < strArray.length; i++) {
                    if (i == 1) {
                        pack = ORG + strArray[1];
                    } else {
                        pack = pack + ORG + strArray[i];
                    }
                }
            } else {
                pack = ORG + strArray[1];
            }
        }

        return pack;
    }

    /**
     * Cleans the generated directory if already exist in source folder.
     *
     * @param dir generated directory in previous build
     * @throws IOException when failed to delete directory
     */
    public static void deleteDirectory(String dir)
            throws IOException {
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
    public static void searchAndDeleteTempDir(String root)
            throws IOException {
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
     * @param source  directory
     * @param project current maven project
     * @param context current build context
     */
    public static void addToCompilationRoot(String source, MavenProject project, BuildContext context) {
        project.addCompileSourceRoot(source);
        context.refresh(project.getBasedir());
        log.info("Source directory added to compilation root: " + source);
    }

    /**
     * Removes extra char from the string.
     *
     * @param valueString    string to be trimmed
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
     *                        put
     * @param pathOfJavaPkg   java package of the file being generated
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
     *                        put
     * @param pathOfJavaPkg   java package of the file being generated
     * @return absolute path of the package in canonical form
     */
    public static String getAbsolutePackagePath(String baseCodeGenPath, String pathOfJavaPkg) {
        return baseCodeGenPath + pathOfJavaPkg;
    }

    /**
     * Copies YANG files to the current project's output directory.
     *
     * @param yangFileInfo list of YANG files
     * @param outputDir    project's output directory
     * @param project      maven project
     * @throws IOException when fails to copy files to destination resource directory
     */
    public static void copyYangFilesToTarget(Set<YangFileInfo> yangFileInfo, String outputDir, MavenProject project)
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
        addToProjectResource(outputDir + SLASH + TEMP + SLASH, project);
    }

    /**
     * Provides a list of files from list of strings.
     *
     * @param yangFileInfo set of yang file information
     * @return list of files
     */
    private static List<File> getListOfFile(Set<YangFileInfo> yangFileInfo) {
        List<File> files = new ArrayList<>();
        Iterator<YangFileInfo> yangFileIterator = yangFileInfo.iterator();
        while (yangFileIterator.hasNext()) {
            YangFileInfo yangFile = yangFileIterator.next();
            if (yangFile.isForTranslator()) {
                files.add(new File(yangFile.getYangFileName()));
            }
        }
        return files;
    }

    /**
     * Merges the temp java files to main java files.
     *
     * @param appendFile temp file
     * @param srcFile    main file
     * @throws IOException when fails to append contents
     */
    public static void mergeJavaFiles(File appendFile, File srcFile)
            throws IOException {
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
    public static void insertDataIntoJavaFile(File file, String data)
            throws IOException {
        try {
            updateFileHandle(file, data, false);
        } catch (IOException e) {
            throw new IOException("Failed to insert in " + file + "file");
        }
    }

    /**
     * Validates a line size in given file whether it is having more then 120 characters.
     * If yes it will update and give a new file.
     *
     * @param dataFile file in which need to verify all lines.
     * @return updated file
     * @throws IOException when fails to do IO operations.
     */
    public static File validateLineLength(File dataFile)
            throws IOException {
        File tempFile = dataFile;
        FileReader fileReader = new FileReader(dataFile);
        BufferedReader bufferReader = new BufferedReader(fileReader);
        try {
            StringBuilder stringBuilder = new StringBuilder();
            String line = bufferReader.readLine();

            while (line != null) {
                if (line.length() > LINE_SIZE) {
                    if (line.contains(PERIOD)) {
                        line = whenDelimiterIsPersent(line, LINE_SIZE);
                    } else if (line.contains(SPACE)) {
                        line = whenSpaceIsPresent(line, LINE_SIZE);
                    }
                    stringBuilder.append(line);
                } else {
                    stringBuilder.append(line + NEW_LINE);
                }
                line = bufferReader.readLine();
            }
            FileWriter writer = new FileWriter(tempFile);
            writer.write(stringBuilder.toString());
            writer.close();
            return tempFile;
        } finally {
            fileReader.close();
            bufferReader.close();
        }
    }

    /* When delimiters are present in the given line. */
    private static String whenDelimiterIsPersent(String line, int lineSize) {
        StringBuilder stringBuilder = new StringBuilder();

        if (line.length() > lineSize) {
            String[] strArray = line.split(Pattern.quote(PERIOD));
            stringBuilder = updateString(strArray, stringBuilder, PERIOD, lineSize);
        } else {
            stringBuilder.append(line + NEW_LINE);
        }
        String[] strArray = stringBuilder.toString().split(NEW_LINE);
        StringBuilder tempBuilder = new StringBuilder();
        for (String str : strArray) {
            if (str.length() > SUB_LINE_SIZE) {
                if (line.contains(PERIOD) && !line.contains(PERIOD + HASH + OPEN_PARENTHESIS)) {
                    String[] strArr = str.split(Pattern.quote(PERIOD));
                    tempBuilder = updateString(strArr, tempBuilder, PERIOD, SUB_LINE_SIZE);
                } else if (str.contains(SPACE)) {
                    tempBuilder.append(whenSpaceIsPresent(str, SUB_LINE_SIZE));
                }
            } else {
                tempBuilder.append(str + NEW_LINE);
            }
        }
        return tempBuilder.toString();

    }

    /* When spaces are present in the given line. */
    private static String whenSpaceIsPresent(String line, int lineSize) {
        StringBuilder stringBuilder = new StringBuilder();
        if (line.length() > lineSize) {
            String[] strArray = line.split(SPACE);
            stringBuilder = updateString(strArray, stringBuilder, SPACE, lineSize);
        } else {
            stringBuilder.append(line + NEW_LINE);
        }

        String[] strArray = stringBuilder.toString().split(NEW_LINE);
        StringBuilder tempBuilder = new StringBuilder();
        for (String str : strArray) {
            if (str.length() > SUB_LINE_SIZE) {
                if (str.contains(SPACE)) {
                    String[] strArr = str.split(SPACE);
                    tempBuilder = updateString(strArr, tempBuilder, SPACE, SUB_LINE_SIZE);
                }
            } else {
                tempBuilder.append(str + NEW_LINE);
            }
        }
        return tempBuilder.toString();
    }

    /* Updates the given line with the given size conditions. */
    private static StringBuilder updateString(String[] strArray, StringBuilder stringBuilder, String string,
            int lineSize) {

        StringBuilder tempBuilder = new StringBuilder();
        for (String str : strArray) {
            tempBuilder.append(str + string);
            if (tempBuilder.length() > lineSize) {
                String tempString = stringBuilder.toString();
                stringBuilder.delete(ZERO, stringBuilder.length());
                tempString = trimAtLast(tempString, string);
                stringBuilder.append(tempString);
                if (string.equals(PERIOD)) {
                    stringBuilder.append(NEW_LINE + TWELVE_SPACE_INDENTATION + PERIOD + str + string);
                } else {
                    stringBuilder.append(NEW_LINE + TWELVE_SPACE_INDENTATION + str + string);
                }
                tempBuilder.delete(ZERO, tempBuilder.length());
                tempBuilder.append(TWELVE_SPACE_INDENTATION);
            } else {
                stringBuilder.append(str + string);
            }
        }
        String tempString = stringBuilder.toString();
        tempString = trimAtLast(tempString, string);
        stringBuilder.delete(ZERO, stringBuilder.length());
        stringBuilder.append(tempString + NEW_LINE);
        return stringBuilder;
    }

    /**
     * Serializes data-model.
     *
     * @param directory base directory for serialized files
     * @param fileInfoSet YANG file info set
     * @param project maven project
     * @param operation true if need to add to resource
     * @throws IOException when fails to do IO operations
     */
    public static void serializeDataModel(String directory, Set<YangFileInfo> fileInfoSet,
            MavenProject project, boolean operation) throws IOException {

        String serFileDirPath = directory + TARGET_RESOURCE_PATH;
        File dir = new File(serFileDirPath);
        dir.mkdirs();

        if (operation) {
            addToProjectResource(directory + SLASH + TEMP + SLASH, project);
        }

        for (YangFileInfo fileInfo : fileInfoSet) {

            String serFileName = serFileDirPath + getCamelCase(fileInfo.getRootNode().getName(), null)
                    + SERIALIZED_FILE_EXTENSION;
            fileInfo.setSerializedFile(serFileName);
            FileOutputStream fileOutputStream = new FileOutputStream(serFileName);
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
            objectOutputStream.writeObject(fileInfo.getRootNode());
            objectOutputStream.close();
            fileOutputStream.close();
        }
    }

    /* Adds directory to resources of project */
    private static void addToProjectResource(String dir, MavenProject project) {
        Resource rsc = new Resource();
        rsc.setDirectory(dir);
        project.addResource(rsc);
    }

    /**
     * Returns de-serializes YANG data-model nodes.
     *
     * @param serailizedfileInfoSet YANG file info set
     * @return de-serializes YANG data-model nodes
     * @throws IOException when fails do IO operations
     */
    public static List<YangNode> deSerializeDataModel(List<String> serailizedfileInfoSet) throws IOException {

        List<YangNode> nodes = new ArrayList<>();
        for (String fileInfo : serailizedfileInfoSet) {
            YangNode node = null;
            try {
                FileInputStream fileInputStream = new FileInputStream(fileInfo);
                ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
                node = (YangNode) objectInputStream.readObject();
                nodes.add(node);
                objectInputStream.close();
                fileInputStream.close();
            } catch (IOException | ClassNotFoundException e) {
                throw new IOException(fileInfo + " not found.");
            }
        }
        return nodes;
    }

    /**
     * Resolves inter jar dependencies.
     *
     * @param project current maven project
     * @param localRepository local maven repository
     * @param remoteRepos list of remote repository
     * @param directory directory for serialized files
     * @return list of resolved datamodel nodes
     * @throws IOException when fails to do IO operations
     */
    public static List<YangNode> resolveInterJarDependencies(MavenProject project, ArtifactRepository localRepository,
            List<ArtifactRepository> remoteRepos, String directory) throws IOException {

        List<String> dependeciesJarPaths = resolveDependecyJarPath(project, localRepository, remoteRepos);
        List<YangNode> resolvedDataModelNodes = new ArrayList<>();
        for (String dependecy : dependeciesJarPaths) {
            resolvedDataModelNodes.addAll(deSerializeDataModel(parseJarFile(dependecy, directory)));
        }
        return resolvedDataModelNodes;
    }

    /**
     * Returns list of jar path.
     *
     * @return list of jar paths
     */
    private static List<String> resolveDependecyJarPath(MavenProject project, ArtifactRepository localRepository,
            List<ArtifactRepository> remoteRepos) {

        StringBuilder path = new StringBuilder();
        List<String> jarPaths = new ArrayList<>();
        for (Dependency dependency : project.getDependencies()) {

            path.append(localRepository.getBasedir());
            path.append(SLASH);
            path.append(getPackageDirPathFromJavaJPackage(dependency.getGroupId()));
            path.append(SLASH);
            path.append(dependency.getArtifactId());
            path.append(SLASH);
            path.append(dependency.getVersion());
            path.append(SLASH);
            path.append(dependency.getArtifactId() + HYPHEN + dependency.getVersion() + PERIOD + JAR);
            File jarFile = new File(path.toString());
            if (jarFile.exists()) {
                jarPaths.add(path.toString());
            }
            path.delete(0, path.length());
        }

        for (ArtifactRepository repo : remoteRepos) {
            // TODO: add resolver for remote repo.
        }
        return jarPaths;
    }

    /**
     * Parses jar file and returns list of serialized file names.
     *
     * @param jarFile jar file to be parsed
     * @param directory directory for keeping the searized files
     * @return list of serialized files
     * @throws IOException when fails to do IO operations
     */
    public static List<String> parseJarFile(String jarFile, String directory)
            throws IOException {

        List<String> serailizedFiles = new ArrayList<>();
        JarFile jar = new JarFile(jarFile);
        Enumeration<?> enumEntries = jar.entries();

        File serializedFileDir = new File(directory);
        serializedFileDir.mkdirs();
        while (enumEntries.hasMoreElements()) {
            JarEntry file = (JarEntry) enumEntries.nextElement();
            if (file.getName().endsWith(SERIALIZED_FILE_EXTENSION)) {
                if (file.getName().contains(SLASH)) {
                    String[] strArray = file.getName().split(SLASH);
                    String tempPath = "";
                    for (int i = 0; i < strArray.length - 1; i++) {
                        tempPath = SLASH + tempPath + SLASH + strArray[i];
                    }
                    File dir = new File(directory + tempPath);
                    dir.mkdirs();
                }
                File serailizedFile = new File(directory + SLASH + file.getName());
                if (file.isDirectory()) {
                    serailizedFile.mkdirs();
                    continue;
                }
                InputStream inputStream = jar.getInputStream(file);

                FileOutputStream fileOutputStream = new FileOutputStream(serailizedFile);
                while (inputStream.available() > 0) {
                    fileOutputStream.write(inputStream.read());
                }
                fileOutputStream.close();
                inputStream.close();
                serailizedFiles.add(serailizedFile.toString());
            }
        }
        jar.close();
        return serailizedFiles;
    }

}
