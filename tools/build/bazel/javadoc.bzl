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

JAVA_DOCS = "-link https://docs.oracle.com/en/java/javase/11/docs/api/"

def _impl(ctx):
    dir = ctx.label.name
    outjar = ctx.outputs.jar

    dep_list = []
    for dep in ctx.files.deps:
        dep_list += [dep.path]

    src_list = []
    for src in ctx.files.srcs:
        src_list += [src.path]

    java_runtime = ctx.attr._jdk[java_common.JavaRuntimeInfo]
    jar_exe_path = "%s/bin/jar" % java_runtime.java_home

    cmd = [
        "mkdir %s" % dir,
        "javadoc -encoding UTF-8 -quiet -tag onos.rsModel:a:\"onos model\" %s -d %s -cp %s %s" %
        (JAVA_DOCS, dir, ":".join(dep_list), " ".join(src_list)),
        "%s cf %s -C %s ." % (jar_exe_path, outjar.path, dir),
    ]

    ctx.actions.run_shell(
        inputs = ctx.files.srcs + ctx.files.deps,
        outputs = [outjar],
        progress_message = "Generating javadocs jar for %s" % ctx.attr.name,
        command = ";\n".join(cmd),
        tools = java_runtime.files,
    )

javadoc = rule(
    attrs = {
        "deps": attr.label_list(allow_files = True),
        "srcs": attr.label_list(allow_files = True),
        "_jdk": attr.label(
            default = Label("@bazel_tools//tools/jdk:current_java_runtime"),
            providers = [java_common.JavaRuntimeInfo],
        ),
    },
    implementation = _impl,
    outputs = {"jar": "%{name}.jar"},
)
