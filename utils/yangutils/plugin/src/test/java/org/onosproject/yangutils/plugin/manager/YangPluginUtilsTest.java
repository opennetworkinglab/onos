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
 * Created by root1 on 16/6/16.
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
