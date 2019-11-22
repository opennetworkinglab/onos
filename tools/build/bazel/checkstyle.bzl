"""
 Copyright 2018-present Open Networking Foundation

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
"""

"""
    Implementation of the rule to call checkstyle
"""

def _checkstyle_impl(ctx):
    classpath = ""
    need_colon = False
    for file in ctx.files._classpath:
        if need_colon:
            classpath += ":"
        need_colon = True
        classpath += file.path

    java_runtime = ctx.attr._jdk[java_common.JavaRuntimeInfo]
    java_exe_path = java_runtime.java_executable_runfiles_path

    cmd = " ".join(
        ["%s -cp %s com.puppycrawl.tools.checkstyle.Main" % (java_exe_path, classpath)] +
        ["-c %s" % ctx.attr._config.files.to_list()[0].path] +
        [src_file.path for src_file in ctx.files.srcs],
    )

    ctx.actions.write(
        output = ctx.outputs.executable,
        content = cmd,
    )

    inputs = (ctx.files.srcs +
              ctx.files._classpath +
              ctx.attr._config.files.to_list() +
              ctx.attr._suppressions.files.to_list() +
              ctx.attr._java_header.files.to_list())

    runfiles = ctx.runfiles(
        files = inputs,
        transitive_files = java_runtime.files,
    )
    return [DefaultInfo(runfiles = runfiles)]

"""
    Rule definition for calling checkstyle
"""
_execute_checkstyle_test = rule(
    test = True,
    attrs = {
        "_classpath": attr.label_list(default = [
            Label("@checkstyle//jar"),
            Label("@commons_beanutils//jar"),
            Label("@commons_cli//jar"),
            Label("@commons_collections//jar"),
            Label("@antlr//jar"),
            Label("@com_google_guava_guava//jar"),
            Label("@commons_logging//jar"),
        ]),
        "srcs": attr.label_list(allow_files = [".java"]),
        "_config": attr.label(default = Label("//tools/build/conf:checkstyle_xml")),
        "_suppressions": attr.label(default = Label("//tools/build/conf:suppressions_xml")),
        "_java_header": attr.label(default = Label("//tools/build/conf:onos_java_header")),
        "_jdk": attr.label(
            default = Label("@bazel_tools//tools/jdk:current_java_runtime"),
            providers = [java_common.JavaRuntimeInfo],
        ),
    },
    implementation = _checkstyle_impl,
)

"""
    Macro to instantiate the checkstyle rule for a given set of sources.

    Args:
        name: name of the target to generate. Required.
        srcs: list of source file targets to run checkstyle on. Required.
        size: test size constraint. Optional, defaults to "small"
"""

def checkstyle_test(name, srcs):
    _execute_checkstyle_test(name = name, srcs = srcs, size = "small")
