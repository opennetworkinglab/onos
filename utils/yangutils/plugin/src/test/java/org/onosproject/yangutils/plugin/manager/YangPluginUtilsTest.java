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
import org.apache.commons.io.FileUtils;
import org.apache.maven.project.MavenProject;
import org.junit.Test;
import org.sonatype.plexus.build.incremental.BuildContext;
import org.sonatype.plexus.build.incremental.DefaultBuildContext;

import static org.onosproject.yangutils.plugin.manager.YangPluginUtils.addToCompilationRoot;

/**
 * Unit test case for YANG plugin utils.
 */
public class YangPluginUtilsTest {

    private static final String BASE_DIR = "target/UnitTestCase";

    /**
     * This test case checks whether the source is getting added.
     */
    @Test
    public void testForAddSource() throws IOException {

        MavenProject project = new MavenProject();
        BuildContext context = new DefaultBuildContext();
        File sourceDir = new File(BASE_DIR + File.separator + "yang");
        sourceDir.mkdirs();
        addToCompilationRoot(sourceDir.toString(), project, context);
        FileUtils.deleteDirectory(sourceDir);
    }
}
