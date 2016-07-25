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
package org.onosproject.onosjar;

import com.facebook.buck.io.ProjectFilesystem;
import com.facebook.buck.rules.SourcePathResolver;
import com.facebook.buck.step.AbstractExecutionStep;
import com.facebook.buck.step.ExecutionContext;
import com.facebook.buck.step.StepExecutionResult;
import com.google.common.collect.ImmutableSortedSet;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Buck build step to trigger SwaggerGenerator.
 */
public class SwaggerStep extends AbstractExecutionStep {

    private final ProjectFilesystem filesystem;

    private final ImmutableSortedSet<Path> srcs;
    private final ImmutableSortedSet<Path> resources;
    private final Path genSourcesOutput;
    private final Path genResourcesOutput;

    private final String webContext;
    private final String apiTitle;
    private final String apiVersion;
    private final String apiPackage;
    private final String apiDescription;


    public SwaggerStep(ProjectFilesystem filesystem,
                       ImmutableSortedSet<Path> srcs,
                       ImmutableSortedSet<Path> resources,
                       Path genSourcesOutput, Path genResourcesOutput,
                       String webContext, String apiTitle, String apiVersion,
                       String apiPackage, String apiDescription) {
        super("swagger");
        this.filesystem = filesystem;
        this.srcs = srcs;
        this.resources = resources;
        this.genSourcesOutput = genSourcesOutput;
        this.genResourcesOutput = genResourcesOutput;
        this.webContext = webContext;
        this.apiTitle = apiTitle;
        this.apiVersion = apiVersion;
        this.apiPackage = apiPackage;
        this.apiDescription = apiDescription;
    }

    @Override
    public StepExecutionResult execute(ExecutionContext executionContext)
            throws IOException, InterruptedException {
        try {
            List<File> srcFiles = srcs.stream()
                    .map(src -> filesystem.resolve(src).toFile())
                    .collect(Collectors.toList());
            List<File> resourceFiles = resources.stream()
                    .map(rsrc -> filesystem.resolve(rsrc).toFile())
                    .collect(Collectors.toList());
            new SwaggerGenerator(srcFiles, resourceFiles, null, null,
                                 filesystem.resolve(genSourcesOutput).toFile(),
                                 filesystem.resolve(genResourcesOutput).toFile(),
                                 webContext,
                                 apiTitle,
                                 apiVersion,
                                 apiPackage,
                                 apiDescription).execute();

            return StepExecutionResult.SUCCESS;
        } catch (Exception e) {
            e.printStackTrace();
            // FIXME print the exception
            return StepExecutionResult.ERROR;
        }
    }

    Path apiRegistratorPath() {
        return genSourcesOutput.resolve(SwaggerGenerator.apiRegistratorPath(apiPackage));
    }
}
