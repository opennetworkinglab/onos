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

package org.onosproject.yangutils.linker;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Test;
import org.onosproject.yangutils.datamodel.YangContainer;
import org.onosproject.yangutils.datamodel.YangDerivedInfo;
import org.onosproject.yangutils.datamodel.YangGrouping;
import org.onosproject.yangutils.datamodel.YangLeaf;
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.plugin.manager.YangFileInfo;
import org.onosproject.yangutils.plugin.manager.YangUtilManager;
import org.onosproject.yangutils.utils.io.impl.YangFileScanner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.onosproject.yangutils.datamodel.YangDataTypes.DERIVED;
import static org.onosproject.yangutils.datamodel.YangDataTypes.STRING;
import static org.onosproject.yangutils.datamodel.utils.ResolvableStatus.RESOLVED;
import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.deSerializeDataModel;
import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.deleteDirectory;
import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.parseJarFile;

/**
 * Unit test case for inter-jar linker.
 */
public class InterJarLinkingTest {

    private final YangUtilManager utilManager = new YangUtilManager();

    private static final String TARGET = "target/interJarFileLinking";
    private static final String SEARCH_DIR_FOR_YANG_FILES = "src/test/resources/interJarFileLinking/yangFiles";
    private static final String SEARCH_DIR_FOR_SINGLE_JAR_FILES = "src/test/resources/interJarFileLinking/"
            + "jarFiles/single";
    private static final String SEARCH_DIR_FOR_MULTI_JAR_FILES = "src/test/resources/interJarFileLinking/"
            + "jarFiles/multi";

    /**
     * Unit test case for a single jar dependency.
     *
     * @throws IOException when fails to do IO operations
     * @throws MojoExecutionException when fails to do mojo operations
     */
    @Test
    public void processSingleJarLinking()
            throws IOException, MojoExecutionException {
        utilManager.createYangFileInfoSet(YangFileScanner.getYangFiles(SEARCH_DIR_FOR_YANG_FILES));

        int size1 = utilManager.getYangFileInfoSet().size();

        for (String file : getListOfTestJar(SEARCH_DIR_FOR_SINGLE_JAR_FILES)) {
            addInterJarRootNodes(file);
        }

        utilManager.parseYangFileInfoSet();

        utilManager.resolveDependenciesUsingLinker();

        Iterator<YangFileInfo> yangFileInfoIterator = utilManager.getYangFileInfoSet().iterator();

        YangFileInfo yangFileInfo = yangFileInfoIterator.next();

        int size2 = utilManager.getYangFileInfoSet().size();
        assertThat(true, is(size1 != size2));
        assertThat(true, is(yangFileInfo.getRootNode().getName().equals("port-pair")));

        deleteDirectory(TARGET);

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
        utilManager.createYangFileInfoSet(YangFileScanner.getYangFiles(SEARCH_DIR_FOR_YANG_FILES));

        int size1 = utilManager.getYangFileInfoSet().size();

        for (String file : getListOfTestJar(SEARCH_DIR_FOR_MULTI_JAR_FILES)) {
            addInterJarRootNodes(file);
        }

        utilManager.parseYangFileInfoSet();

        utilManager.resolveDependenciesUsingLinker();

        Iterator<YangFileInfo> yangFileInfoIterator = utilManager.getYangFileInfoSet().iterator();

        YangFileInfo yangFileInfo = yangFileInfoIterator.next();

        int size2 = utilManager.getYangFileInfoSet().size();
        assertThat(true, is(size1 != size2));
        assertThat(true, is(yangFileInfo.getRootNode().getName().equals("port-pair")));

        yangFileInfo = yangFileInfoIterator.next();
        assertThat(true, is(yangFileInfo.getRootNode().getName().equals("flow-classifier")));

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

        deleteDirectory(TARGET);
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
            jarFiles.add(file.toString());
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

}
