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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.junit.Test;
import org.onosproject.yangutils.datamodel.YangContainer;
import org.onosproject.yangutils.datamodel.YangDerivedInfo;
import org.onosproject.yangutils.datamodel.YangGrouping;
import org.onosproject.yangutils.datamodel.YangLeaf;
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.utils.io.impl.YangFileScanner;
import org.onosproject.yangutils.utils.io.impl.YangPluginConfig;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.onosproject.yangutils.datamodel.YangDataTypes.DERIVED;
import static org.onosproject.yangutils.datamodel.YangDataTypes.STRING;
import static org.onosproject.yangutils.datamodel.utils.ResolvableStatus.RESOLVED;
import static org.onosproject.yangutils.plugin.manager.YangPluginUtils.deSerializeDataModel;
import static org.onosproject.yangutils.plugin.manager.YangPluginUtils.parseJarFile;
import static org.onosproject.yangutils.plugin.manager.YangPluginUtils.serializeDataModel;
import static org.onosproject.yangutils.utils.UtilConstants.SLASH;
import static org.onosproject.yangutils.utils.UtilConstants.TEMP;
import static org.onosproject.yangutils.utils.UtilConstants.YANG_RESOURCES;
import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.deleteDirectory;

/**
 * Unit test case for inter jar linker.
 */
public class InterJarLinkerTest {

    private final YangUtilManager utilManager = new YangUtilManager();

    private static final String TARGET = "target/interJarFileLinking/";
    private static final String YANG_FILES_DIR = "src/test/resources/interJarFileLinking/yangFiles/";
    private static final String TARGET_RESOURCE_PATH = SLASH + TEMP + SLASH + YANG_RESOURCES + SLASH;
    private static final String JAR_FILE_NAME = "onlab-test-1.7.0-SNAPSHOT.jar";
    private static final String SER_FILE_NAME = "portPair.ser";

    private static final String FLOW_CLASSIFIER_FOLDER = "target/interJarFileLinking/org/onosproject"
            + "/yang/gen/v1/sfc/flowclassifier/rev20160524";
    private static final String PORT_PAIR_FOLDER = "target/interJarFileLinking/org/onosproject"
            + "/yang/gen/v1/sfc/portpair/rev20160524";
    private static final String FLOW_CLASSIFIER_MANAGER = FLOW_CLASSIFIER_FOLDER + SLASH + "FlowClassifierManager.java";

    /**
     * Unit test case for a single jar dependency.
     *
     * @throws IOException when fails to do IO operations
     * @throws MojoExecutionException when fails to do mojo operations
     */
    @Test
    public void processSingleJarLinking()
            throws IOException, MojoExecutionException {
        utilManager.createYangFileInfoSet(YangFileScanner.getYangFiles(YANG_FILES_DIR));

        int size1 = utilManager.getYangFileInfoSet().size();
        utilManager.parseYangFileInfoSet();

        provideTestJarFile();
        utilManager.setYangFileInfoSet(removeFileInfoFromSet(utilManager.getYangFileInfoSet()));

        for (String file : getListOfTestJar(TARGET)) {
            addInterJarRootNodes(file);
        }

        utilManager.resolveDependenciesUsingLinker();

        int size2 = utilManager.getYangFileInfoSet().size();
        assertThat(true, is(size1 != size2));
        assertThat(true, is(parseFileInfoSet(utilManager.getYangFileInfoSet().iterator())));

        deleteDirectory(TARGET);
        deleteTestSerFile();
    }

    /**
     * Unit test case for a multiple jar dependency.
     *
     * @throws IOException when fails to do IO operations
     * @throws MojoExecutionException when fails to do mojo operations
     */
    @Test
    public void processMultipleJarLinking()
            throws IOException, MojoExecutionException {
        utilManager.createYangFileInfoSet(YangFileScanner.getYangFiles(YANG_FILES_DIR));

        int size1 = utilManager.getYangFileInfoSet().size();
        utilManager.parseYangFileInfoSet();

        provideTestJarFile();
        utilManager.setYangFileInfoSet(removeFileInfoFromSet(utilManager.getYangFileInfoSet()));
        for (String file : getListOfTestJar(TARGET)) {
            addInterJarRootNodes(file);
        }

        utilManager.resolveDependenciesUsingLinker();
        int size2 = utilManager.getYangFileInfoSet().size();
        assertThat(true, is(size1 != size2));
        assertThat(true, is(parseFileInfoSet(utilManager.getYangFileInfoSet().iterator())));
        assertThat(true, is(parseFileInfoSet(utilManager.getYangFileInfoSet().iterator())));

        /*
         * grouping flow-classifier {
         *      container flow-classifier {
         *           leaf id {
         *                type flow-classifier-id;
         *           }
         *
         *           leaf tenant-id {
         *                type port-pair:tenant-id;
         *           }
         *           .
         *           .
         *           .
         *
         */

        Iterator<YangFileInfo> yangFileInfoIterator = utilManager.getYangFileInfoSet().iterator();

        YangFileInfo yangFileInfo = yangFileInfoIterator.next();

        while (yangFileInfoIterator.hasNext()) {
            if (yangFileInfo.getRootNode().getName().equals("flow-classifier")) {
                break;
            }
            yangFileInfo = yangFileInfoIterator.next();
        }

        YangNode node = yangFileInfo.getRootNode();
        node = node.getChild();
        while (node != null) {
            if (node instanceof YangGrouping) {
                break;
            }
            node = node.getNextSibling();
        }

        node = node.getChild();
        ListIterator<YangLeaf> leafIterator = ((YangContainer) node).getListOfLeaf().listIterator();
        YangLeaf leafInfo = leafIterator.next();

        assertThat(leafInfo.getName(), is("id"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("flow-classifier-id"));
        assertThat(leafInfo.getDataType().getDataType(), is(DERIVED));

        leafInfo = leafIterator.next();

        assertThat(leafInfo.getName(), is("tenant-id"));
        assertThat(leafInfo.getDataType().getDataType(), is(DERIVED));

        assertThat(true, is(((YangDerivedInfo<?>) leafInfo.getDataType().getDataTypeExtendedInfo()).getReferredTypeDef()
                .getName().equals("tenant-id")));

        assertThat(leafInfo.getDataType().getResolvableStatus(), is(RESOLVED));

        YangDerivedInfo<?> derivedInfo = (YangDerivedInfo<?>) leafInfo.getDataType().getDataTypeExtendedInfo();

        // Check for the effective built-in type.
        assertThat(derivedInfo.getEffectiveBuiltInType(), is(STRING));

        YangPluginConfig yangPluginConfig = new YangPluginConfig();
        yangPluginConfig.setCodeGenDir(TARGET);

        utilManager.translateToJava(utilManager.getYangFileInfoSet(), yangPluginConfig);

        testIfFlowClassifierFilesExists();
        testIfPortPairFileDoesNotExist();
        deleteDirectory(TARGET);
        deleteTestSerFile();
    }

    /**
     * Test if flow classifier code is generated.
     */
    private void testIfFlowClassifierFilesExists() {
        File folder = new File(System.getProperty("user.dir") + SLASH + FLOW_CLASSIFIER_FOLDER);
        File file = new File(System.getProperty("user.dir") + SLASH + FLOW_CLASSIFIER_MANAGER);
        assertThat(true, is(folder.exists()));
        assertThat(true, is(file.exists()));
    }

    /**
     * Tests if port pair code is not generated.
     */
    private void testIfPortPairFileDoesNotExist() {
        File folder = new File(System.getProperty("user.dir") + SLASH + PORT_PAIR_FOLDER);
        assertThat(false, is(folder.exists()));
    }

    /**
     * Need to remove port-pair YANG file info from the set so , serialized file info can be
     * tested.
     *
     * @param fileInfoSet YANG file info set
     * @return updated file info set
     */
    private Set<YangFileInfo> removeFileInfoFromSet(Set<YangFileInfo> fileInfoSet) {
        String portPairFile = System.getProperty("user.dir") + SLASH + YANG_FILES_DIR + "portpair.yang";
        for (YangFileInfo fileInfo : fileInfoSet) {
            if (fileInfo.getYangFileName().equals(portPairFile)) {
                fileInfoSet.remove(fileInfo);
                return fileInfoSet;
            }
        }
        return fileInfoSet;
    }

    /**
     * Provides test jar files for linker.
     *
     * @throws IOException when fails to do IO operations
     */
    private void provideTestJarFile() throws IOException {

        MavenProject project = new MavenProject();
        serializeDataModel(TARGET, utilManager.getYangFileInfoSet(), project, false);
        createTestJar();
    }

    /**
     * Deletes serialized file.
     */
    private void deleteTestSerFile() {
        File ser = new File(System.getProperty("user.dir") + SLASH + YANG_FILES_DIR + SER_FILE_NAME);
        ser.delete();
    }

    /**
     * Parses file info list and returns true if file info list contains the serialized file info.
     *
     * @param yangFileInfoIterator file info list iterator
     * @return true if present
     */
    private boolean parseFileInfoSet(Iterator<YangFileInfo> yangFileInfoIterator) {
        YangFileInfo yangFileInfo = yangFileInfoIterator.next();
        while (yangFileInfoIterator.hasNext()) {
            if (yangFileInfo.getRootNode().getName().equals("port-pair")) {
                return true;
            } else if (yangFileInfo.getRootNode().getName().equals("flow-classifier")) {
                return true;
            }
            yangFileInfo = yangFileInfoIterator.next();
        }
        return false;

    }

    /**
     * Returns list of test jar files.
     *
     * @param searchdir search directory
     * @return list of test jar files
     */
    private List<String> getListOfTestJar(String searchdir) {
        List<String> jarFiles = new ArrayList<>();

        File directory = new File(searchdir + "/");
        File[] files = directory.listFiles();

        for (File file : files) {
            if (!file.isDirectory()) {
                jarFiles.add(file.toString());
            }
        }

        return jarFiles;
    }

    /**
     * Adds data model nodes of jar to file info set.
     *
     * @param jarFile jar file name
     * @throws IOException when fails to do IO operations
     */
    private void addInterJarRootNodes(String jarFile) throws IOException {
        try {
            List<YangNode> interJarResolvedNodes = deSerializeDataModel(parseJarFile(jarFile, TARGET));

            for (YangNode node : interJarResolvedNodes) {
                YangFileInfo dependentFileInfo = new YangFileInfo();
                dependentFileInfo.setRootNode(node);
                dependentFileInfo.setForTranslator(false);
                dependentFileInfo.setYangFileName(node.getName());
                utilManager.getYangFileInfoSet().add(dependentFileInfo);
            }
        } catch (IOException e) {
            throw new IOException("failed to resolve in interjar scenario.");
        }
    }

    /**
     * Creates a temporary test jar files.
     */
    private void createTestJar() {

        File file = new File(TARGET + TARGET_RESOURCE_PATH);
        File[] files = file.listFiles();

        String[] source = new String[files.length];

        for (int i = 0; i < files.length; i++) {
            source[i] = files[i].toString();
        }
        byte[] buf = new byte[1024];

        try {
            String target = TARGET + JAR_FILE_NAME;
            JarOutputStream out = new JarOutputStream(new FileOutputStream(target));
            for (String element : source) {
                FileInputStream in = new FileInputStream(element);
                out.putNextEntry(new JarEntry(element));
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                out.closeEntry();
                in.close();
            }
            out.close();
        } catch (IOException e) {
        }
    }

}
