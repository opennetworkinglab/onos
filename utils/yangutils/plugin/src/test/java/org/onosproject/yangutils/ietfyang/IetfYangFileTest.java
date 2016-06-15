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

package org.onosproject.yangutils.ietfyang;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Test;
import org.onosproject.yangutils.linker.impl.YangLinkerManager;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.YangUtilsParserManager;
import org.onosproject.yangutils.plugin.manager.YangUtilManager;
import org.onosproject.yangutils.utils.io.impl.YangPluginConfig;
import org.onosproject.yangutils.utils.io.impl.YangFileScanner;

import java.io.IOException;

import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.deleteDirectory;

/**
 * Test cases for testing IETF YANG files.
 */
public class IetfYangFileTest {

    private final YangUtilsParserManager manager = new YangUtilsParserManager();
    private final YangUtilManager utilManager = new YangUtilManager();
    private final YangLinkerManager yangLinkerManager = new YangLinkerManager();

    /**
     * Checks hierarchical intra with inter file type linking.
     * Reference: https://datatracker.ietf.org/doc/draft-zha-l3sm-l3vpn-onos-deployment
     */
    @Test
    public void l3vpnserviceyang()
            throws IOException, ParserException, MojoExecutionException {

        String searchDir = "src/test/resources/ietfyang/l3vpnservice";
        utilManager.createYangFileInfoSet(YangFileScanner.getYangFiles(searchDir));
        utilManager.parseYangFileInfoSet();
        utilManager.resolveDependenciesUsingLinker();

        String userDir = System.getProperty("user.dir");
        YangPluginConfig yangPluginConfig = new YangPluginConfig();
        yangPluginConfig.setCodeGenDir("target/ietfyang/l3vpnservice/");

        utilManager.translateToJava(utilManager.getYangFileInfoSet(), yangPluginConfig);

        deleteDirectory(userDir + "/target/ietfyang/");
    }

}
