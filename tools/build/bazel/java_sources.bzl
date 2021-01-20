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

def _impl(ctx):
    outjar = ctx.outputs.jar

    src_list = ""
    for src in ctx.files.srcs:
        if src.path.endswith(".srcjar"):
            src_list += " " + src.path

    java_runtime = ctx.attr._jdk[java_common.JavaRuntimeInfo]
    jar_path = "%s/bin/jar" % java_runtime.java_home

    cmd = [
        "for sj in %s; do %s xf $sj; done" % (src_list, jar_path),
        "dir=$(find . -type d -name java)",
        "[ -n \"$dir\" -a -d \"$dir\" ] && %s cf %s -C $dir ." % (jar_path, outjar.path),
    ]

    ctx.actions.run_shell(
        inputs = ctx.files.srcs,
        outputs = [outjar],
        progress_message = "Generating source jar for %s" % ctx.attr.name,
        command = ";\n".join(cmd),
        tools = java_runtime.files,
    )

def _impl_alt(ctx):
    ending = "-src.jar"
    src_files = []
    out_files = []
    if len(ctx.files.srcs) == 0:
        fail("Cannot generate source jars from and empty input")
    for src in ctx.files.srcs:
        if src.path.endswith(ending):
            prefix = src.path[:-len(ending)]
            src_files.append(src)
            out_file = ctx.actions.declare_file(prefix + ".srcjar")
            out_files.append(out_file)
    if len(src_files) == 0:
        fail("None of the given input files is a valid source jar (%s)" %
             ", ".join([s.path for s in ctx.files.srcs]))
    for i in range(len(src_files)):
        cmd = "cp %s %s" % (src_files[i].path, out_files[i].path)
        ctx.actions.run_shell(
            inputs = [src_files[i]],
            outputs = [out_files[i]],
            progress_message = "Generating source jar %s" % out_files[i].basename,
            command = cmd,
        )
    return DefaultInfo(files = depset(out_files))

"""
Creates a single source jar file from a set of .srcjar files.

Args:
    srcs: List of source files. Only files ending with .srcjar will be considered.
"""

java_sources = rule(
    attrs = {
        "srcs": attr.label_list(allow_files = True),
        "_jdk": attr.label(
            default = Label("@bazel_tools//tools/jdk:current_java_runtime"),
            providers = [java_common.JavaRuntimeInfo],
        ),
    },
    implementation = _impl,
    outputs = {"jar": "%{name}.jar"},
)

"""
Returns a collection of source jar files ending with .srcjar from the given list of java_library or file labels.
Input files are expected to end with "-src.jar".

Args:
    srcs: List of java_library labels.

"""

java_sources_alt = rule(
    attrs = {
        "srcs": attr.label_list(allow_files = True),
    },
    implementation = _impl_alt,
)
