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

def dump(obj):
    print(dir(obj))
    for attr in dir(obj):
        print("obj.%s = %r" % (attr, getattr(obj, attr)))

def _impl(ctx):
    dir = ctx.label.name
    outjar = ctx.outputs.jar

    classpath = ""
    for dep in ctx.files.deps:
        classpath += ":" + dep.path

    src_list = ""
    for src in ctx.files.srcs:
        if src.path.endswith(".jar"):
            src_list += " " + src.path

    group_list = ""
    for group in ctx.attr.groups:
        packages = ""
        for p in ctx.attr.groups[group]:
            packages += ":" + p
        group_list += " -group \"%s\" %s" % (group, packages.replace(":", "", 1))

    java_runtime = ctx.attr._jdk[java_common.JavaRuntimeInfo]
    jar_exe_path = "%s/bin/jar" % java_runtime.java_home
    javadoc_exe_path = "%s/bin/javadoc" % java_runtime.java_home

    cmd = [
        "mkdir src; cd src",
        "for s in %s; do ../%s xf ../$s; done" % (src_list, jar_exe_path),
        "rm -f META-INF/MANIFEST.MF",
        "cd ..",
        "cp -r docs/src/main/javadoc/* .",
        "ls -lR doc-files overview.html",
    ]

    if ctx.attr.internal:
        cmd += ["find src -type f | egrep -v 'src/(OSGI|WEB)-INF' >> FILES"]
    else:
        cmd += ["find src -type f | egrep -v 'src/(OSGI|WEB)-INF' | egrep -v '/(impl|internal)/' >> FILES"]

    cmd += [
        "%s -encoding UTF-8 -overview overview.html -doctitle '%s' -windowtitle '%s' %s -d apidocs -classpath %s -sourcepath src %s @FILES" %
        (javadoc_exe_path, ctx.attr.title, ctx.attr.title, group_list, classpath.replace(":", "", 1), JAVA_DOCS),
        "cp -r doc-files apidocs/doc-files",
        "%s cf %s apidocs" % (jar_exe_path, outjar.path),
    ]

    ctx.actions.run_shell(
        inputs = ctx.files.srcs + ctx.files.deps,
        outputs = [outjar],
        progress_message = "Generating javadocs jar for %s" % ctx.attr.name,
        command = ";\n".join(cmd),
        tools = java_runtime.files,
    )

project_javadoc = rule(
    attrs = {
        "title": attr.string(),
        "overview": attr.string(default = "src/main/javadoc/overview.html"),
        "groups": attr.string_list_dict(),
        "deps": attr.label_list(allow_files = True),
        "srcs": attr.label_list(allow_files = True),
        "internal": attr.bool(default = False),
        "_jdk": attr.label(
            default = Label("@bazel_tools//tools/jdk:current_java_runtime"),
            providers = [java_common.JavaRuntimeInfo],
        ),
    },
    implementation = _impl,
    outputs = {"jar": "%{name}.jar"},
)
