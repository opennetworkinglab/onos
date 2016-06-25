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

package org.onosproject.yangutils.plugin.manager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Resource;
import org.apache.maven.project.MavenProject;
import org.onosproject.yangutils.datamodel.YangNode;
import org.slf4j.Logger;
import org.sonatype.plexus.build.incremental.BuildContext;

import static org.onosproject.yangutils.utils.UtilConstants.HYPHEN;
import static org.onosproject.yangutils.utils.UtilConstants.JAR;
import static org.onosproject.yangutils.utils.UtilConstants.PERIOD;
import static org.onosproject.yangutils.utils.UtilConstants.SLASH;
import static org.onosproject.yangutils.utils.UtilConstants.TEMP;
import static org.onosproject.yangutils.utils.UtilConstants.YANG_RESOURCES;
import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.getCamelCase;
import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.getPackageDirPathFromJavaJPackage;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Represents YANG plugin utilities.
 */
public final class YangPluginUtils {

    private static final Logger log = getLogger(YangPluginUtils.class);

    private static final String TARGET_RESOURCE_PATH = SLASH + TEMP + SLASH + YANG_RESOURCES + SLASH;

    private static final String SERIALIZED_FILE_EXTENSION = ".ser";

    private YangPluginUtils() {
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
     * Serializes data-model.
     *
     * @param directory   base directory for serialized files
     * @param fileInfoSet YANG file info set
     * @param project     maven project
     * @param operation   true if need to add to resource
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
     * Returns list of jar path.
     *
     * @param project         maven project
     * @param localRepository local repository
     * @param remoteRepos     remote repository
     * @return list of jar paths
     */
    private static List<String> resolveDependecyJarPath(MavenProject project, ArtifactRepository localRepository,
                                                        List<ArtifactRepository> remoteRepos) {

        StringBuilder path = new StringBuilder();
        List<String> jarPaths = new ArrayList<>();
        for (Object obj : project.getDependencies()) {

            Dependency dependency = (Dependency) obj;
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
     * Resolves inter jar dependencies.
     *
     * @param project         current maven project
     * @param localRepository local maven repository
     * @param remoteRepos     list of remote repository
     * @param directory       directory for serialized files
     * @return list of resolved datamodel nodes
     * @throws IOException when fails to do IO operations
     */
    public static List<YangNode> resolveInterJarDependencies(MavenProject project, ArtifactRepository localRepository,
                                                             List<ArtifactRepository> remoteRepos, String directory)
            throws IOException {

        List<String> dependeciesJarPaths = resolveDependecyJarPath(project, localRepository, remoteRepos);
        List<YangNode> resolvedDataModelNodes = new ArrayList<>();
        for (String dependecy : dependeciesJarPaths) {
            resolvedDataModelNodes.addAll(deSerializeDataModel(parseJarFile(dependecy, directory)));
        }
        return resolvedDataModelNodes;
    }

    /**
     * Parses jar file and returns list of serialized file names.
     *
     * @param jarFile   jar file to be parsed
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

    /* Adds directory to resources of project */
    private static void addToProjectResource(String dir, MavenProject project) {
        Resource rsc = new Resource();
        rsc.setDirectory(dir);
        project.addResource(rsc);
    }
}
