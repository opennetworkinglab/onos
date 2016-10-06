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
import com.facebook.buck.jvm.java.JavaLibrary;
import com.facebook.buck.jvm.java.JavadocJar;
import com.facebook.buck.model.BuildTarget;
import com.facebook.buck.model.Flavor;
import com.facebook.buck.model.Flavored;
import com.facebook.buck.model.Pair;
import com.facebook.buck.parser.NoSuchBuildTargetException;
import com.facebook.buck.rules.BuildRule;
import com.facebook.buck.rules.BuildRuleParams;
import com.facebook.buck.rules.BuildRuleResolver;
import com.facebook.buck.rules.BuildRuleType;
import com.facebook.buck.rules.Description;
import com.facebook.buck.rules.SourcePath;
import com.facebook.buck.rules.SourcePathResolver;
import com.facebook.buck.rules.TargetGraph;
import com.google.common.base.Optional;
import com.google.common.base.Suppliers;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Ordering;

import java.nio.file.Path;
import java.util.Map;

/**
 * Description for the onos_jar rules.
 *
 * Currently, this only does Swagger generation.
 */
public class ProjectJavadocDescription implements Description<ProjectJavadocDescription.Arg>, Flavored {
    public static final BuildRuleType TYPE = BuildRuleType.of("project_javadoc");

    public ProjectJavadocDescription(BuckConfig config) {
        //TODO
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

        ImmutableSet.Builder<SourcePath> srcs = ImmutableSet.builder();
        ImmutableSet.Builder<BuildRule> deps = ImmutableSet.builder();
        ImmutableSortedMap.Builder<SourcePath, Path> docfiles = ImmutableSortedMap.naturalOrder();
        for(BuildTarget dep : args.deps) {
            BuildRule rule = resolver.requireRule(dep.withFlavors(JavaLibrary.JAVADOC_JAR));
            if (rule instanceof JavadocJar) {
                JavadocJar jarRule = (JavadocJar) rule;
                srcs.addAll(jarRule.getSources());
                deps.addAll(jarRule.getDeps());
                docfiles.putAll(jarRule.getDocFiles());
            } else {
                throw new RuntimeException("rule is not a javalib"); //FIXME
            }
        }

        BuildRuleParams newParams = params.copyWithDeps(
                Suppliers.ofInstance(
                    FluentIterable.from(deps.build())
                                  .toSortedSet(Ordering.<BuildRule>natural())),
                Suppliers.ofInstance(ImmutableSortedSet.<BuildRule>of()));

        SourcePathResolver sourceResolver = new SourcePathResolver(resolver);
        ImmutableList.Builder<SourcePath> auxSources = ImmutableList.builder();

        JavadocJar.JavadocArgs.Builder javadocArgs = JavadocJar.JavadocArgs.builder()
                .addArg("-windowtitle", args.projectTitle)
                .addArg("-doctitle", args.projectTitle)
                .addArg("-link", "http://docs.oracle.com/javase/8/docs/api")
                .addArg("-tag", "onos.rsModel:a:\"onos model\""); //FIXME from buckconfig + rule

        if (args.groups.isPresent()) {
            for (Pair<String, ImmutableList<String>> pair : args.groups.get()) {
                javadocArgs.addArg("-group", pair.getFirst(), pair.getSecond());
            }
        }

        if (args.excludePackages.isPresent() &&
                !args.excludePackages.get().isEmpty()) {
            javadocArgs.addArg("-exclude", args.excludePackages.get());
        }

        if (args.overview.isPresent()) {
            javadocArgs.addArg("-overview",
                   sourceResolver.getAbsolutePath(args.overview.get()).toString());

        }

        if (args.javadocFiles.isPresent()) {
            for (SourcePath path : args.javadocFiles.get()) {
                docfiles.put(path,
                             JavadocJar.getDocfileWithPath(sourceResolver, path, args.javadocFilesRoot.orNull()));
            }
        }


        JavadocJar javadocJar =  new JavadocJar(newParams,
                              sourceResolver,
                              ImmutableSortedSet.copyOf(srcs.build()),
                              docfiles.build(),
                              javadocArgs.build(),
                              /* mavenCoords */ Optional.absent());
        return javadocJar;

    }

    @Override
    public boolean hasFlavors(ImmutableSet<Flavor> flavors) {
        return false;
    }

    public static class Arg {
        public ImmutableSortedSet<BuildTarget> deps;
        public String projectTitle;

        public Optional<SourcePath> overview;
        public Optional<ImmutableSortedSet<SourcePath>> javadocFiles;
        public Optional<Path> javadocFilesRoot;

        public Optional<ImmutableList<String>> excludePackages;
        public Optional<ImmutableList<Pair<String, ImmutableList<String>>>> groups;
    }
}