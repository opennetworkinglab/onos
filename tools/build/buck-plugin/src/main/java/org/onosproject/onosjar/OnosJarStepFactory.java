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
import com.facebook.buck.jvm.core.SuggestBuildRules;
import com.facebook.buck.jvm.java.ClassUsageFileWriter;
import com.facebook.buck.jvm.java.JarDirectoryStep;
import com.facebook.buck.jvm.java.JavacOptions;
import com.facebook.buck.jvm.java.JavacOptionsAmender;
import com.facebook.buck.jvm.java.JavacToJarStepFactory;
import com.facebook.buck.model.BuildTarget;
import com.facebook.buck.rules.BuildContext;
import com.facebook.buck.rules.BuildableContext;
import com.facebook.buck.rules.SourcePath;
import com.facebook.buck.rules.SourcePathResolver;
import com.facebook.buck.step.Step;
import com.facebook.buck.step.fs.CopyStep;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Creates the list of build steps for the onos_jar rules.
 */
public class OnosJarStepFactory extends JavacToJarStepFactory {

    private static final String DEFINITIONS = "/definitions/";
    private final String webContext;
    private final String apiTitle;
    private final String apiVersion;
    private final String apiPackage;
    private final String apiDescription;
    private final Optional<ImmutableSortedSet<SourcePath>> resources;
    private final String groupId;
    private final String bundleName;
    private final String bundleVersion;
    private final String bundleLicense;
    private final String bundleDescription;
    private final String importPackages;
    private final String privatePackages;
    private final String exportPackages;
    private final String includeResources;
    private final String dynamicimportPackages;
    private final String embeddedDependencies;

    public OnosJarStepFactory(JavacOptions javacOptions,
                              JavacOptionsAmender amender,
                              Optional<String> webContext,
                              Optional<String> apiTitle,
                              Optional<String> apiVersion,
                              Optional<String> apiPackage,
                              Optional<String> apiDescription,
                              Optional<ImmutableSortedSet<SourcePath>> resources,
                              Optional<String> groupId,
                              Optional<String> bundleName,
                              Optional<String> bundleVersion,
                              Optional<String> bundleLicense,
                              Optional<String> bundleDescription,
                              Optional<String> importPackages,
                              Optional<String> exportPackages,
                              Optional<String> includeResources,
                              Optional<String> dynamicimportPackages,
                              Optional<String> privatePackages,
                              Optional<String> embeddedDependencies) {
        super(javacOptions, amender);
        this.bundleDescription = processParameter(bundleDescription);
        this.importPackages = processParameter(importPackages);
        this.privatePackages = processParameter(privatePackages);
        this.exportPackages = processParameter(exportPackages);
        this.includeResources = processParameter(includeResources);
        this.dynamicimportPackages = processParameter(dynamicimportPackages);
        this.groupId = processParameter(groupId);
        this.bundleName = processParameter(bundleName);
        this.bundleVersion = processParameter(bundleVersion);
        this.bundleLicense = processParameter(bundleLicense);
        this.webContext = processParameter(webContext);
        this.apiTitle = processParameter(apiTitle);
        this.apiVersion = processParameter(apiVersion);
        this.apiPackage = processParameter(apiPackage);
        this.apiDescription = processParameter(apiDescription);
        this.resources = resources;
        this.embeddedDependencies = processParameter(embeddedDependencies);
    }

    private String processParameter(Optional<String> p) {
        return !p.isPresent() || p.get().equals("NONE") ? null : p.get();
    }

    @Override
    public void createCompileToJarStep(BuildContext context,
                                       ImmutableSortedSet<Path> sourceFilePaths,
                                       BuildTarget invokingRule,
                                       SourcePathResolver resolver,
                                       ProjectFilesystem filesystem,
                                       ImmutableSortedSet<Path> declaredClasspathEntries,
                                       Path outputDirectory,
                                       Optional<Path> workingDirectory,
                                       Path pathToSrcsList,
                                       Optional<SuggestBuildRules> suggestBuildRules,
                                       ImmutableList<String> postprocessClassesCommands,
                                       ImmutableSortedSet<Path> entriesToJar,
                                       Optional<String> mainClass,
                                       Optional<Path> manifestFile,
                                       Path outputJar,
                                       ClassUsageFileWriter usedClassesFileWriter,
                                       ImmutableList.Builder<Step> steps,
                                       BuildableContext buildableContext,
                                       ImmutableSet<Pattern> classesToRemoveFromJar) {

        ImmutableSet.Builder<Path> sourceFilePathBuilder = ImmutableSet.builder();
        sourceFilePathBuilder.addAll(sourceFilePaths);

        ImmutableSet.Builder<Pattern> blacklistBuilder = ImmutableSet.builder();
        blacklistBuilder.addAll(classesToRemoveFromJar);

        // precompilation steps
        //   - generate sources
        //     add all generated sources ot pathToSrcsList
        if (webContext != null && apiTitle != null && resources.isPresent()) {
            ImmutableSortedSet<Path> resourceFilePaths = findSwaggerModelDefs(resolver, resources.get());
            blacklistBuilder.addAll(resourceFilePaths.stream()
                                            .map(rp -> Pattern.compile(rp.getFileName().toString(), Pattern.LITERAL))
                                            .collect(Collectors.toSet()));
            Path genSourcesOutput = workingDirectory.get();

            SwaggerStep swaggerStep = new SwaggerStep(filesystem, sourceFilePaths, resourceFilePaths,
                                                      genSourcesOutput, outputDirectory,
                                                      webContext, apiTitle, apiVersion,
                                                      apiPackage, apiDescription);
            sourceFilePathBuilder.add(swaggerStep.apiRegistratorPath());
            steps.add(swaggerStep);

//            steps.addAll(sourceFilePaths.stream()
//                    .filter(sp -> sp.startsWith("src/main/webapp/"))
//                    .map(sp -> CopyStep.forFile(filesystem, sp, outputDirectory))
//                    .iterator());
        }

        createCompileStep(context,
                          ImmutableSortedSet.copyOf(sourceFilePathBuilder.build()),
                          invokingRule,
                          resolver,
                          filesystem,
                          declaredClasspathEntries,
                          outputDirectory,
                          workingDirectory,
                          pathToSrcsList,
                          suggestBuildRules,
                          usedClassesFileWriter,
                          steps,
                          buildableContext);

        // post compilation steps


        // FIXME BOC: add mechanism to inject new Steps
        //context.additionalStepFactory(JavaStep.class);

        // build the jar
        steps.add(new JarDirectoryStep(filesystem,
                                       outputJar,
                                       ImmutableSortedSet.of(outputDirectory),
                                       mainClass.orNull(),
                                       manifestFile.orNull(),
                                       true,
                                       blacklistBuilder.build()));

        OSGiWrapper osgiStep = new OSGiWrapper(
                outputJar, //input jar
                outputJar, //Paths.get(outputJar.toString() + ".jar"), //output jar
                invokingRule.getBasePath(), // sources dir
                outputDirectory, // classes dir
                declaredClasspathEntries, // classpath
                bundleName, // bundle name
                groupId, // groupId
                bundleVersion, // bundle version
                bundleLicense, // bundle license
                importPackages, // import packages
                exportPackages, // export packages
                includeResources, // include resources
                webContext, // web context
                dynamicimportPackages, // dynamic import packages
                embeddedDependencies, // embedded dependencies
                bundleDescription,  // bundle description
                privatePackages // private packages
        );
        steps.add(osgiStep);

        //steps.add(CopyStep.forFile(filesystem, Paths.get(outputJar.toString() + ".jar"), outputJar));

    }

    private ImmutableSortedSet<Path> findSwaggerModelDefs(SourcePathResolver resolver,
                                                          ImmutableSortedSet<SourcePath> resourcePaths) {
        if (resourcePaths == null) {
            return ImmutableSortedSet.of();
        }
        return ImmutableSortedSet.copyOf(resourcePaths.stream()
                                                 .filter(sp -> sp.toString().contains(DEFINITIONS))
                                                 .map(resolver::getRelativePath)
                                                 .collect(Collectors.toList()));
    }
}
