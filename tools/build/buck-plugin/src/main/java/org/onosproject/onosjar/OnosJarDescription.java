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
import com.facebook.buck.jvm.java.JavaLibrary;
import com.facebook.buck.jvm.java.JavaLibraryDescription;
import com.facebook.buck.jvm.java.JavaOptions;
import com.facebook.buck.jvm.java.JavaSourceJar;
import com.facebook.buck.jvm.java.JavacOptions;
import com.facebook.buck.jvm.java.JavacOptionsAmender;
import com.facebook.buck.jvm.java.JavacOptionsFactory;
import com.facebook.buck.jvm.java.JavacToJarStepFactory;
import com.facebook.buck.jvm.java.JavadocJar;
import com.facebook.buck.jvm.java.MavenUberJar;
import com.facebook.buck.maven.AetherUtil;
import com.facebook.buck.model.BuildTarget;
import com.facebook.buck.model.Flavor;
import com.facebook.buck.model.Flavored;
import com.facebook.buck.model.ImmutableFlavor;
import com.facebook.buck.parser.NoSuchBuildTargetException;
import com.facebook.buck.rules.BuildRule;
import com.facebook.buck.rules.BuildRuleParams;
import com.facebook.buck.rules.BuildRuleResolver;
import com.facebook.buck.rules.BuildRuleType;
import com.facebook.buck.rules.BuildRules;
import com.facebook.buck.rules.BuildTargetSourcePath;
import com.facebook.buck.rules.Description;
import com.facebook.buck.rules.SourcePath;
import com.facebook.buck.rules.SourcePathResolver;
import com.facebook.buck.rules.SourcePaths;
import com.facebook.buck.rules.TargetGraph;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Iterables;

import java.nio.file.Path;

import static com.facebook.buck.jvm.common.ResourceValidator.validateResources;

/**
 * Description for the onos_jar rules.
 *
 * Currently, this only does Swagger generation.
 */
public class OnosJarDescription implements Description<OnosJarDescription.Arg>, Flavored {
    public static final BuildRuleType TYPE = BuildRuleType.of("onos_jar");
    public static final Flavor NON_OSGI_JAR = ImmutableFlavor.of("non-osgi");

    public static final ImmutableSet<Flavor> SUPPORTED_FLAVORS = ImmutableSet.of(
            JavaLibrary.SRC_JAR,
            JavaLibrary.MAVEN_JAR,
            JavaLibrary.JAVADOC_JAR,
            NON_OSGI_JAR);

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

        if (flavors.contains(JavaLibrary.MAVEN_JAR)) {
            paramsWithMavenFlavor = params;

            // Maven rules will depend upon their vanilla versions, so the latter have to be constructed
            // without the maven flavor to prevent output-path conflict
            params = params.copyWithBuildTarget(
                    params.getBuildTarget().withoutFlavors(ImmutableSet.of(JavaLibrary.MAVEN_JAR)));
        }

        if (flavors.contains(JavaLibrary.SRC_JAR)) {
            args.mavenCoords = args.mavenCoords.transform(
                    new Function<String, String>() {
                        @Override
                        public String apply(String input) {
                            return AetherUtil.addClassifier(input, AetherUtil.CLASSIFIER_SOURCES);
                        }
                    });

            if (!flavors.contains(JavaLibrary.MAVEN_JAR)) {
                return new JavaSourceJar(
                        params,
                        pathResolver,
                        args.srcs.get(),
                        args.mavenCoords);
            } else {
                return MavenUberJar.SourceJar.create(
                        Preconditions.checkNotNull(paramsWithMavenFlavor),
                        pathResolver,
                        args.srcs.get(),
                        args.mavenCoords,
                        Optional.absent()); //FIXME
            }
        }

        if (flavors.contains(JavaLibrary.JAVADOC_JAR)) {
            args.mavenCoords = args.mavenCoords.transform(
                    new Function<String, String>() {
                        @Override
                        public String apply(String input) {
                            return AetherUtil.addClassifier(input, AetherUtil.CLASSIFIER_JAVADOC);
                        }
                    });

            JavadocJar.JavadocArgs.Builder javadocArgs = JavadocJar.JavadocArgs.builder()
                    .addArg("-windowtitle", target.getShortName())
                    .addArg("-link", "http://docs.oracle.com/javase/8/docs/api")
                    .addArg("-tag", "onos.rsModel:a:\"onos model\""); //FIXME from buckconfig + rule

            final ImmutableSortedMap.Builder<SourcePath, Path> javadocFiles = ImmutableSortedMap.naturalOrder();
            if (args.javadocFiles.isPresent()) {
                for (SourcePath path : args.javadocFiles.get()) {
                    javadocFiles.put(path,
                                     JavadocJar.getDocfileWithPath(pathResolver, path, args.javadocFilesRoot.orNull()));
                }
            }


            if (!flavors.contains(JavaLibrary.MAVEN_JAR)) {
                return new JavadocJar(
                        params,
                        pathResolver,
                        args.srcs.get(),
                        javadocFiles.build(),
                        javadocArgs.build(),
                        args.mavenCoords);
            } else {
                return MavenUberJar.MavenJavadocJar.create(
                        Preconditions.checkNotNull(paramsWithMavenFlavor),
                        pathResolver,
                        args.srcs.get(),
                        javadocFiles.build(),
                        javadocArgs.build(),
                        args.mavenCoords,
                        Optional.absent()); //FIXME
            }
        }

        JavacOptions javacOptions = JavacOptionsFactory.create(
                defaultJavacOptions,
                params,
                resolver,
                pathResolver,
                args
        );

        BuildTarget abiJarTarget = params.getBuildTarget().withAppendedFlavors(CalculateAbi.FLAVOR);

        ImmutableSortedSet<BuildRule> exportedDeps = resolver.getAllRules(args.exportedDeps.get());

        final DefaultJavaLibrary defaultJavaLibrary;
        if (!flavors.contains(NON_OSGI_JAR)) {
            defaultJavaLibrary =
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
                                                           args.apiPackage, args.apiDescription, args.resources,
                                                           args.groupId, args.bundleName, args.bundleVersion,
                                                           args.bundleLicense, args.bundleDescription, args.importPackages,
                                                           args.exportPackages, args.includeResources, args.dynamicimportPackages),
                                    args.resourcesRoot,
                                    args.manifestFile,
                                    args.mavenCoords,
                                    args.tests.get(),
                                    javacOptions.getClassesToRemoveFromJar(),
                                    args.webContext,
                                    args.apiTitle,
                                    args.apiVersion,
                                    args.apiPackage,
                                    args.apiDescription));
        } else {
            defaultJavaLibrary =
                    resolver.addToIndex(
                            new DefaultJavaLibrary(
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
                                    args.postprocessClassesCommands.get(),
                                    exportedDeps,
                                    resolver.getAllRules(args.providedDeps.get()),
                                    new BuildTargetSourcePath(abiJarTarget),
                                    javacOptions.trackClassUsage(),
                                    /* additionalClasspathEntries */ ImmutableSet.<Path>of(),
                                    new JavacToJarStepFactory(javacOptions, JavacOptionsAmender.IDENTITY),
                                    args.resourcesRoot,
                                    args.manifestFile,
                                    args.mavenCoords,
                                    args.tests.get(),
                                    javacOptions.getClassesToRemoveFromJar()));
        }

        resolver.addToIndex(
                CalculateAbi.of(
                        abiJarTarget,
                        pathResolver,
                        params,
                        new BuildTargetSourcePath(defaultJavaLibrary.getBuildTarget())));

        return defaultJavaLibrary;
    }

    @Override
    public boolean hasFlavors(ImmutableSet<Flavor> flavors) {
        return SUPPORTED_FLAVORS.containsAll(flavors);
    }

    public static class Arg extends JavaLibraryDescription.Arg {
        public Optional<String> webContext;
        public Optional<String> apiTitle;
        public Optional<String> apiVersion;
        public Optional<String> apiPackage;
        public Optional<String> apiDescription;

        public Optional<String> groupId;
        public Optional<String> bundleName;
        public Optional<String> bundleVersion;
        public Optional<String> bundleLicense;
        public Optional<String> bundleDescription;

        public Optional<String> importPackages;
        public Optional<String> exportPackages;
        public Optional<String> includeResources;
        public Optional<String> dynamicimportPackages;
    }
}