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

import com.facebook.buck.cli.BuckConfig;
import com.facebook.buck.jvm.java.CalculateAbi;
import com.facebook.buck.jvm.java.DefaultJavaLibrary;
import com.facebook.buck.jvm.java.JavaBuckConfig;
import com.facebook.buck.jvm.java.JavaLibraryDescription;
import com.facebook.buck.jvm.java.JavaOptions;
import com.facebook.buck.jvm.java.JavacOptions;
import com.facebook.buck.jvm.java.JavacOptionsAmender;
import com.facebook.buck.jvm.java.JavacOptionsFactory;
import com.facebook.buck.model.BuildTarget;
import com.facebook.buck.model.Flavor;
import com.facebook.buck.parser.NoSuchBuildTargetException;
import com.facebook.buck.rules.BuildRule;
import com.facebook.buck.rules.BuildRuleParams;
import com.facebook.buck.rules.BuildRuleResolver;
import com.facebook.buck.rules.BuildRuleType;
import com.facebook.buck.rules.BuildRules;
import com.facebook.buck.rules.BuildTargetSourcePath;
import com.facebook.buck.rules.Description;
import com.facebook.buck.rules.SourcePathResolver;
import com.facebook.buck.rules.SourcePaths;
import com.facebook.buck.rules.TargetGraph;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Iterables;

import java.nio.file.Path;

import static com.facebook.buck.jvm.common.ResourceValidator.validateResources;

/**
 * Description for the onos_jar rules.
 *
 * Currently, this only does Swagger generation.
 */
public class OnosJarDescription implements Description<OnosJarDescription.Arg> {
    public static final BuildRuleType TYPE = BuildRuleType.of("onos_jar");
    private final JavacOptions defaultJavacOptions;
    private final JavaOptions defaultJavaOptions;

    public OnosJarDescription(BuckConfig config) {
        JavaBuckConfig javaConfig = new JavaBuckConfig(config);
        defaultJavacOptions = javaConfig.getDefaultJavacOptions();
        defaultJavaOptions = javaConfig.getDefaultJavaOptions();
    }

    @Override
    public BuildRuleType getBuildRuleType() {
        return TYPE;
    }

    @Override
    public Arg createUnpopulatedConstructorArg() {
        return new Arg();
    }

    @Override
    public <A extends Arg> BuildRule createBuildRule(TargetGraph targetGraph,
                                                     BuildRuleParams params,
                                                     BuildRuleResolver resolver,
                                                     A args)
            throws NoSuchBuildTargetException {


        SourcePathResolver pathResolver = new SourcePathResolver(resolver);
        BuildTarget target = params.getBuildTarget();

        // We know that the flavour we're being asked to create is valid, since the check is done when
        // creating the action graph from the target graph.

        ImmutableSortedSet<Flavor> flavors = target.getFlavors();
        BuildRuleParams paramsWithMavenFlavor = null;

        JavacOptions javacOptions = JavacOptionsFactory.create(
                defaultJavacOptions,
                params,
                resolver,
                pathResolver,
                args
        );

        BuildTarget abiJarTarget = params.getBuildTarget().withAppendedFlavors(CalculateAbi.FLAVOR);

        ImmutableSortedSet<BuildRule> exportedDeps = resolver.getAllRules(args.exportedDeps.get());

        DefaultJavaLibrary defaultJavaLibrary =
                resolver.addToIndex(
                        new OnosJar(
                                params.appendExtraDeps(
                                        Iterables.concat(
                                                BuildRules.getExportedRules(
                                                        Iterables.concat(
                                                                params.getDeclaredDeps().get(),
                                                                exportedDeps,
                                                                resolver.getAllRules(args.providedDeps.get()))),
                                                pathResolver.filterBuildRuleInputs(
                                                        javacOptions.getInputs(pathResolver)))),
                                pathResolver,
                                args.srcs.get(),
                                validateResources(
                                        pathResolver,
                                        params.getProjectFilesystem(),
                                        args.resources.get()),
                                javacOptions.getGeneratedSourceFolderName(),
                                args.proguardConfig.transform(
                                        SourcePaths.toSourcePath(params.getProjectFilesystem())),
                                args.postprocessClassesCommands.get(), // FIXME this should be forbidden
                                exportedDeps,
                                resolver.getAllRules(args.providedDeps.get()),
                                new BuildTargetSourcePath(abiJarTarget),
                                javacOptions.trackClassUsage(),
                                /* additionalClasspathEntries */ ImmutableSet.<Path>of(),
                                new OnosJarStepFactory(javacOptions, JavacOptionsAmender.IDENTITY,
                                                       args.webContext, args.apiTitle, args.apiVersion,
                                                       args.apiPackage, args.apiDescription, args.resources),
                                args.resourcesRoot,
                                args.mavenCoords,
                                args.tests.get(),
                                javacOptions.getClassesToRemoveFromJar(),
                                args.webContext,
                                args.apiTitle,
                                args.apiVersion,
                                args.apiPackage,
                                args.apiDescription));

        resolver.addToIndex(
                CalculateAbi.of(
                        abiJarTarget,
                        pathResolver,
                        params,
                        new BuildTargetSourcePath(defaultJavaLibrary.getBuildTarget())));

        return defaultJavaLibrary;
    }


    public static class Arg extends JavaLibraryDescription.Arg {
        public Optional<String> webContext;
        public Optional<String> apiTitle;
        public Optional<String> apiVersion;
        public Optional<String> apiPackage;
        public Optional<String> apiDescription;
    }
}