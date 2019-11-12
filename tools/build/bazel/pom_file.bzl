# Copyright 2015 The Bazel Authors. All rights reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

load("//tools/build/bazel:generate_workspace.bzl", "maven_coordinates")

def _impl_pom_file(ctx):
    arguments = [
        ctx.outputs.pom.path,
        maven_coordinates(ctx.attr.artifact),
    ]

    for dep in ctx.attr.deps:
        arguments += [maven_coordinates(dep.label)]

    ctx.actions.run(
        inputs = ctx.files.deps,
        outputs = [ctx.outputs.pom],
        progress_message = "Generating pom file for %s" % ctx.attr.name,
        arguments = arguments,
        executable = ctx.executable._pom_generator,
    )

pom_file = rule(
    attrs = {
        "artifact": attr.string(),
        "deps": attr.label_list(allow_files = True),
        "_pom_generator": attr.label(
            executable = True,
            cfg = "host",
            allow_files = True,
            default = Label("//tools/build/bazel:pom_generator"),
        ),
    },
    implementation = _impl_pom_file,
    outputs = {"pom": "%{name}.pom"},
)

def _impl_dependencies_pom(ctx):
    arguments = [
        "-o",
        ctx.outputs.pom.path,
        "-p",
        ctx.file.pom_template.path,
        "-d",
    ] + [maven_coordinates(d.label) for d in ctx.attr.deps] + [
        "-c",
    ] + [maven_coordinates(d.label) for d in ctx.attr.deps_provided] + [
        "-t",
    ] + [maven_coordinates(d.label) for d in ctx.attr.deps_test] + [
        "-v",
    ] + ctx.attr.vars

    ctx.actions.run(
        inputs = [ctx.file.pom_template],
        outputs = [ctx.outputs.pom],
        progress_message = "Generating dependencies pom for %s" % ctx.attr.name,
        arguments = arguments,
        executable = ctx.executable._pom_generator,
    )

dependencies_pom = rule(
    attrs = {
        "pom_template": attr.label(allow_single_file = True),
        "deps_provided": attr.label_list(),
        "deps_test": attr.label_list(),
        "deps": attr.label_list(),
        "vars": attr.string_list(),
        "_pom_generator": attr.label(
            executable = True,
            cfg = "host",
            allow_files = True,
            default = Label("//tools/build/bazel:dependencies_pom_generator"),
        ),
    },
    implementation = _impl_dependencies_pom,
    outputs = {"pom": "%{name}.pom"},
)
