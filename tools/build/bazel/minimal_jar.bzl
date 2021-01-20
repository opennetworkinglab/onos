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

    java_runtime = ctx.attr._jdk[java_common.JavaRuntimeInfo]
    jar_exe_path = "%s/bin/jar" % java_runtime.java_home

    cmd = [
        "mkdir readme && touch readme/README && %s cf %s readme/README" % (jar_exe_path, outjar.path),
    ]

    ctx.actions.run_shell(
        outputs = [outjar],
        progress_message = "Generating minimal jar for %s" % ctx.attr.name,
        command = ";\n".join(cmd),
        tools = java_runtime.files,
    )

minimal_jar = rule(
    attrs = {
        "_jdk": attr.label(
            default = Label("@bazel_tools//tools/jdk:current_java_runtime"),
            providers = [java_common.JavaRuntimeInfo],
        ),
    },
    implementation = _impl,
    outputs = {"jar": "%{name}.jar"},
)
