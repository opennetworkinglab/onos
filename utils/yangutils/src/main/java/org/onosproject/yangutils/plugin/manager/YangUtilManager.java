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
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.Component;
import org.sonatype.plexus.build.incremental.BuildContext;

import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.parser.YangUtilsParser;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.utils.io.impl.YangFileScanner;

/**
 * ONOS YANG utility maven plugin.
 * Goal of plugin is yang2java
 * Execution phase in generate-sources
 * requiresDependencyResolution at compile time
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

    private YangUtilsParser yangUtilsParser;
    private String baseDir;
    private String searchDir;

    /**
     * Set current project.
     *
     * @param project maven project.
     */
    public void setCurrentProject(final MavenProject project) {
        this.project = project;
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        try {
            baseDir = project.getBasedir().toString();
            searchDir = baseDir + File.separator + yangFilesDir;

            List<String> yangFiles = YangFileScanner.getYangFiles(searchDir);
            Iterator<String> yangFileIterator = yangFiles.iterator();
            while (yangFileIterator.hasNext()) {
                String yangFile = yangFileIterator.next();
                try {
                    YangNode yangNode = yangUtilsParser.getDataModel(yangFile);
                    //TODO: send this data model to translator and create the corresponding java files.
                } catch (ParserException e) {
                    getLog().info("Invalid yang file.");
                }
            }
        } catch (final IOException e) {
            getLog().info("Exception occured");
        }
    }
}