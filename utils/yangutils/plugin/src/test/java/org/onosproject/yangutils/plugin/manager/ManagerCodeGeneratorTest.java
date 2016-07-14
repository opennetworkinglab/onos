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
import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Test;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.utils.io.impl.YangFileScanner;
import org.onosproject.yangutils.utils.io.impl.YangPluginConfig;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.deleteDirectory;

/**
 * Unit test case to test code generation for root nodes.
 */
public class ManagerCodeGeneratorTest {

    private final YangUtilManager utilManager = new YangUtilManager();

    /**
     * Checks manager translation should not result in any exception.
     *
     * @throws MojoExecutionException
     */
    @Test
    public void processManagerTranslator() throws IOException, ParserException, MojoExecutionException {

        String searchDir = "src/test/resources/manager";
        utilManager.createYangFileInfoSet(YangFileScanner.getYangFiles(searchDir));
        utilManager.parseYangFileInfoSet();
        utilManager.createYangNodeSet();
        utilManager.resolveDependenciesUsingLinker();

        YangPluginConfig yangPluginConfig = new YangPluginConfig();
        yangPluginConfig.setCodeGenDir("target/manager/");
        yangPluginConfig.setManagerCodeGenDir("target/manager/");

        utilManager.translateToJava(utilManager.getYangFileInfoSet(), yangPluginConfig);
        String file1 = "target/manager/org/onosproject/yang/gen/v1/test5/test/rev20160704/Test5Manager.java";
        String file2 = "target/manager/org/onosproject/yang/gen/v1/test5/test/rev20160704/Test6Manager.java";
        String file3 = "target/manager/org/onosproject/yang/gen/v1/test5/test/rev20160704/Test7Manager.java";
        File manager = new File(file1);
        assertThat(false, is(manager.exists()));

        File manager2 = new File(file2);
        assertThat(false, is(manager2.exists()));

        File manager3 = new File(file3);
        assertThat(true, is(manager3.exists()));

        deleteDirectory("target/manager/");
    }

    /**
     * Checks manager translation in different package should not result in any exception.
     *
     * @throws MojoExecutionException
     */
    @Test
    public void processManagerInDifferentPackageTranslator() throws IOException, ParserException,
            MojoExecutionException {

        String searchDir = "src/test/resources/manager";
        utilManager.createYangFileInfoSet(YangFileScanner.getYangFiles(searchDir));
        utilManager.parseYangFileInfoSet();
        utilManager.createYangNodeSet();
        utilManager.resolveDependenciesUsingLinker();

        YangPluginConfig yangPluginConfig = new YangPluginConfig();
        yangPluginConfig.setCodeGenDir("target/manager/");
        yangPluginConfig.setManagerCodeGenDir("target/manager1/");

        utilManager.translateToJava(utilManager.getYangFileInfoSet(), yangPluginConfig);
        String file3 = "target/manager1/org/onosproject/yang/gen/v1/test5/test/rev20160704/Test7Manager.java";

        File manager3 = new File(file3);
        assertThat(true, is(manager3.exists()));

        deleteDirectory("target/manager/");
        deleteDirectory("target/manager1/");
    }
}
