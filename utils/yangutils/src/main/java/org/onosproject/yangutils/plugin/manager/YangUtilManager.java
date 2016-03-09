/*
 * Copyright 2016 Open Networking Laboratory
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
import java.util.Iterator;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.parser.YangUtilsParser;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.YangUtilsParserManager;
import org.onosproject.yangutils.translator.tojava.JavaCodeGenerator;
import org.onosproject.yangutils.utils.UtilConstants;
import org.onosproject.yangutils.utils.io.impl.CopyrightHeader;
import org.onosproject.yangutils.utils.io.impl.YangFileScanner;
import org.onosproject.yangutils.utils.io.impl.YangIoUtils;
import org.sonatype.plexus.build.incremental.BuildContext;

/**
 * ONOS YANG utility maven plugin. Goal of plugin is yang2java Execution phase
 * in generate-sources requiresDependencyResolution at compile time.
 */
@Mojo(name = "yang2java", defaultPhase = LifecyclePhase.GENERATE_SOURCES,
        requiresDependencyResolution = ResolutionScope.COMPILE, requiresProject = true)
public class YangUtilManager extends AbstractMojo {

    /**
     * Source directory for YANG files.
     */
    @Parameter(property = "yangFilesDir", defaultValue = "src/main/yang")
    private String yangFilesDir;

    /**
     * Source directory for generated files.
     */
    @Parameter(property = "genFilesDir", defaultValue = "src/main/java")
    private String genFilesDir;

    /**
     * Base directory for project.
     */
    @Parameter(property = "basedir", defaultValue = "${basedir}")
    private String baseDir;

    /**
     * Output directory.
     */
    @Parameter(property = "project.build.outputDirectory", required = true, defaultValue = "target/classes")
    private File outputDirectory;

    /**
     * Current maven project.
     */
    @Parameter(property = "project", required = true, readonly = true, defaultValue = "${project}")
    private MavenProject project;

    /**
     * Build context.
     */
    @Component
    private BuildContext context;

    private YangUtilsParser yangUtilsParser = new YangUtilsParserManager();
    private String searchDir;
    private String codeGenDir;

    /**
     * Set current project.
     *
     * @param curProject maven project
     */
    public void setCurrentProject(final MavenProject curProject) {
        project = curProject;
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        try {

            CopyrightHeader.parseCopyrightHeader();

            /**
             * For deleting the generated code in previous build.
             */
            YangIoUtils.clean(baseDir);

            searchDir = baseDir + File.separator + yangFilesDir;
            codeGenDir = baseDir + File.separator + genFilesDir + File.separator;

            List<String> yangFiles = YangFileScanner.getYangFiles(searchDir);
            Iterator<String> yangFileIterator = yangFiles.iterator();
            while (yangFileIterator.hasNext()) {
                String yangFile = yangFileIterator.next();
                try {
                    YangNode yangNode = yangUtilsParser.getDataModel(yangFile);
                    JavaCodeGenerator.generateJavaCode(yangNode, codeGenDir);
                } catch (ParserException e) {
                    String logInfo = "Error in file: " + e.getFileName();
                    if (e.getLineNumber() != 0) {
                        logInfo = logInfo + " at line: " + e.getLineNumber() + " at position: "
                                + e.getCharPositionInLine();

                    }
                    if (e.getMessage() != null) {
                        logInfo = logInfo + "\n" + e.getMessage();
                    }
                    getLog().info(logInfo);
                }
            }

            YangIoUtils.addToSource(baseDir + File.separator + UtilConstants.YANG_GEN_DIR, project, context);
        } catch (Exception e) {
            getLog().info(e);
        }
    }

}