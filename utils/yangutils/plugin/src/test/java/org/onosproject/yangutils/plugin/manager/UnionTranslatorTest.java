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

import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Test;
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.YangUtilsParserManager;
import org.onosproject.yangutils.utils.io.impl.YangFileScanner;
import org.onosproject.yangutils.utils.io.impl.YangPluginConfig;

import static org.onosproject.yangutils.translator.tojava.JavaCodeGeneratorUtil.generateJavaCode;
import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.deleteDirectory;

/**
 * Unit tests for union translator.
 */
public final class UnionTranslatorTest {

    private final YangUtilsParserManager manager = new YangUtilsParserManager();

    /**
     * Checks union translation should not result in any exception.
     */
    @Test
    public void processUnionTranslator()
            throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/UnionTranslator.yang");

        YangPluginConfig yangPluginConfig = new YangPluginConfig();
        yangPluginConfig.setCodeGenDir("target/UnionTestGenFile/");
        yangPluginConfig.setManagerCodeGenDir("target/UnionTestGenFile/");

        generateJavaCode(node, yangPluginConfig);

        deleteDirectory("target/UnionTestGenFile/");
    }

    /**
     * Unit test case to test conflicting types.
     *
     * @throws IOException when fails to do IO operations
     * @throws MojoExecutionException when fails to do mojo operations
     */
    @Test
    public void processUnionIntUintConflictingTypes() throws IOException, MojoExecutionException {
        String searchDir = "src/test/resources/unionTranslator/intuint";
        YangUtilManager utilManager = new YangUtilManager();
        utilManager.createYangFileInfoSet(YangFileScanner.getYangFiles(searchDir));
        utilManager.parseYangFileInfoSet();
        utilManager.createYangNodeSet();
        utilManager.resolveDependenciesUsingLinker();

        YangPluginConfig yangPluginConfig = new YangPluginConfig();
        yangPluginConfig.setCodeGenDir("target/unionTranslator/");
        yangPluginConfig.setManagerCodeGenDir("target/unionTranslator/");

        utilManager.translateToJava(yangPluginConfig);
        deleteDirectory("target/unionTranslator/");
    }

    /**
     * Unit test case to test conflicting types.
     *
     * @throws IOException when fails to do IO operations
     * @throws MojoExecutionException when fails to do mojo operations
     */
    @Test
    public void processUnionUintIntConflictingTypes() throws IOException, MojoExecutionException {
        String searchDir = "src/test/resources/unionTranslator/uintint";
        YangUtilManager utilManager = new YangUtilManager();
        utilManager.createYangFileInfoSet(YangFileScanner.getYangFiles(searchDir));
        utilManager.parseYangFileInfoSet();
        utilManager.createYangNodeSet();
        utilManager.resolveDependenciesUsingLinker();

        YangPluginConfig yangPluginConfig = new YangPluginConfig();
        yangPluginConfig.setCodeGenDir("target/unionTranslator/");
        yangPluginConfig.setManagerCodeGenDir("target/unionTranslator/");

        utilManager.translateToJava(yangPluginConfig);
        deleteDirectory("target/unionTranslator/");
    }

    /**
     * Unit test case to test conflicting types.
     *
     * @throws IOException when fails to do IO operations
     * @throws MojoExecutionException when fails to do mojo operations
     */
    @Test
    public void processUnionLongUlongConflictingTypes() throws IOException, MojoExecutionException {
        String searchDir = "src/test/resources/unionTranslator/longulong";
        YangUtilManager utilManager = new YangUtilManager();
        utilManager.createYangFileInfoSet(YangFileScanner.getYangFiles(searchDir));
        utilManager.parseYangFileInfoSet();
        utilManager.createYangNodeSet();
        utilManager.resolveDependenciesUsingLinker();

        YangPluginConfig yangPluginConfig = new YangPluginConfig();
        yangPluginConfig.setCodeGenDir("target/unionTranslator/");
        yangPluginConfig.setManagerCodeGenDir("target/unionTranslator/");

        utilManager.translateToJava(yangPluginConfig);
        deleteDirectory("target/unionTranslator/");
    }

    /**
     * Unit test case to test conflicting types.
     *
     * @throws IOException when fails to do IO operations
     * @throws MojoExecutionException when fails to do mojo operations
     */
    @Test
    public void processUnionUlongLongConflictingTypes() throws IOException, MojoExecutionException {
        String searchDir = "src/test/resources/unionTranslator/ulonglong";
        YangUtilManager utilManager = new YangUtilManager();
        utilManager.createYangFileInfoSet(YangFileScanner.getYangFiles(searchDir));
        utilManager.parseYangFileInfoSet();
        utilManager.createYangNodeSet();
        utilManager.resolveDependenciesUsingLinker();

        YangPluginConfig yangPluginConfig = new YangPluginConfig();
        yangPluginConfig.setCodeGenDir("target/unionTranslator/");
        yangPluginConfig.setManagerCodeGenDir("target/unionTranslator/");

        utilManager.translateToJava(yangPluginConfig);
        deleteDirectory("target/unionTranslator/");
    }

    /**
     * Unit test case to test conflicting types.
     *
     * @throws IOException when fails to do IO operations
     * @throws MojoExecutionException when fails to do mojo operations
     */
    @Test
    public void processUnionIntUintUlongLongConflictingTypes() throws IOException, MojoExecutionException {
        String searchDir = "src/test/resources/unionTranslator/intuintulonglong";
        YangUtilManager utilManager = new YangUtilManager();
        utilManager.createYangFileInfoSet(YangFileScanner.getYangFiles(searchDir));
        utilManager.parseYangFileInfoSet();
        utilManager.createYangNodeSet();
        utilManager.resolveDependenciesUsingLinker();

        YangPluginConfig yangPluginConfig = new YangPluginConfig();
        yangPluginConfig.setCodeGenDir("target/unionTranslator/");
        yangPluginConfig.setManagerCodeGenDir("target/unionTranslator/");

        utilManager.translateToJava(yangPluginConfig);
        deleteDirectory("target/unionTranslator/");
    }

    /**
     * Unit test case to test conflicting types.
     *
     * @throws IOException when fails to do IO operations
     * @throws MojoExecutionException when fails to do mojo operations
     */
    @Test
    public void processUnionIntUintUlongLongStringConflictingTypes() throws IOException,
            MojoExecutionException {
        String searchDir = "src/test/resources/unionTranslator/intuintulonglongstring";
        YangUtilManager utilManager = new YangUtilManager();
        utilManager.createYangFileInfoSet(YangFileScanner.getYangFiles(searchDir));
        utilManager.parseYangFileInfoSet();
        utilManager.createYangNodeSet();
        utilManager.resolveDependenciesUsingLinker();

        YangPluginConfig yangPluginConfig = new YangPluginConfig();
        yangPluginConfig.setCodeGenDir("target/unionTranslator/");
        yangPluginConfig.setManagerCodeGenDir("target/unionTranslator/");

        utilManager.translateToJava(yangPluginConfig);
        deleteDirectory("target/unionTranslator/");
    }

    /**
     * Unit test case to test conflicting types.
     *
     * @throws IOException when fails to do IO operations
     * @throws MojoExecutionException when fails to do mojo operations
     */
    @Test
    public void processUnionIntUintStringConflictingTypes() throws IOException,
            MojoExecutionException {
        String searchDir = "src/test/resources/unionTranslator/intuintstring";
        YangUtilManager utilManager = new YangUtilManager();
        utilManager.createYangFileInfoSet(YangFileScanner.getYangFiles(searchDir));
        utilManager.parseYangFileInfoSet();
        utilManager.createYangNodeSet();
        utilManager.resolveDependenciesUsingLinker();

        YangPluginConfig yangPluginConfig = new YangPluginConfig();
        yangPluginConfig.setCodeGenDir("target/unionTranslator/");
        yangPluginConfig.setManagerCodeGenDir("target/unionTranslator/");

        utilManager.translateToJava(yangPluginConfig);
        deleteDirectory("target/unionTranslator/");
    }

    // TODO enhance the test cases, after having a framework of translator test.
}
