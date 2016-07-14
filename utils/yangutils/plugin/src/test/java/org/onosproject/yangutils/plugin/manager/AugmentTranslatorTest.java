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
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.utils.io.impl.YangFileScanner;
import org.onosproject.yangutils.utils.io.impl.YangPluginConfig;

import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.deleteDirectory;

/**
 * Unit test case for augment translator.
 */
public class AugmentTranslatorTest {

    private final YangUtilManager utilManager = new YangUtilManager();

    /**
     * Checks augment translation should not result in any exception.
     *
     * @throws MojoExecutionException
     */
    @Test
    public void processAugmentTranslator() throws IOException, ParserException, MojoExecutionException {

        String searchDir = "src/test/resources/augmentTranslator";
        utilManager.createYangFileInfoSet(YangFileScanner.getYangFiles(searchDir));
        utilManager.parseYangFileInfoSet();
        utilManager.createYangNodeSet();
        utilManager.resolveDependenciesUsingLinker();

        YangPluginConfig yangPluginConfig = new YangPluginConfig();
        yangPluginConfig.setCodeGenDir("target/augmentTranslator/");
        yangPluginConfig.setManagerCodeGenDir("target/augmentTranslator/");
        utilManager.translateToJava(utilManager.getYangFileInfoSet(), yangPluginConfig);

        deleteDirectory("target/augmentTranslator/");
    }

}
