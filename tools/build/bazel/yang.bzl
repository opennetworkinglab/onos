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

load("//tools/build/bazel:generate_workspace.bzl", "CORE_DEPS", "ONOS_YANG")
load("//tools/build/bazel:osgi_java_library.bzl", "osgi_jar")
load("//tools/build/bazel:onos_app.bzl", "onos_app")
load(
    "//tools/build/bazel:variables.bzl",
    "APP_PREFIX",
    "DEFAULT_APP_CATEGORY",
    "ONOS_ARTIFACT_BASE",
    "ONOS_GROUP_ID",
    "ONOS_ORIGIN",
    "ONOS_VERSION",
)

REGISTRATOR = \
    "// Auto-generated code\n" + \
    "package org.onosproject.model.registrator.impl;\n" + \
    "\n" + \
    "import org.onosproject.yang.AbstractYangModelRegistrator;\n" + \
    "import org.osgi.service.component.annotations.Component;\n" + \
    "\n" + \
    "@Component(immediate = true, service = YangModelRegistrator.class)\n" + \
    "public class YangModelRegistrator extends AbstractYangModelRegistrator {\n" + \
    "    public YangModelRegistrator() {\n" + \
    "        super(YangModelRegistrator.class);\n" + \
    "    }\n" + \
    "}"

REGISTRATOR_FILE = "src/org/onosproject/model/registrator/impl/YangModelRegistrator.java"

# Implementation of the YANG library rule
def _yang_library_impl(ctx):
    generated_sources = ctx.actions.declare_directory("generated-sources")

    arguments = [
        ctx.attr.model_id,
        generated_sources.path,
    ]
    inputs = []

    for dep in ctx.files.deps:
        arguments += ["-d", dep.path]
        inputs += [dep]

    for source in ctx.files.srcs:
        arguments += [source.path]
        inputs += [source]

    ctx.actions.run(
        inputs = inputs,
        outputs = [generated_sources],
        arguments = arguments,
        progress_message = "Compiling YANG library: %s" % ctx.attr.name,
        executable = ctx.executable._yang_compiler,
    )

    java_runtime = ctx.attr._jdk[java_common.JavaRuntimeInfo]
    jar_path = "%s/bin/jar" % java_runtime.java_home

    ctx.actions.run_shell(
        inputs = [generated_sources],
        outputs = [ctx.outputs.srcjar],
        arguments = [
            ctx.outputs.srcjar.path,
            generated_sources.path,
        ],
        tools = java_runtime.files,
        command = "%s cf $1 -C $2 src" % jar_path,
        progress_message = "Assembling YANG Java sources: %s" % ctx.attr.name,
    )

    ctx.actions.run_shell(
        inputs = [generated_sources],
        outputs = [ctx.outputs.schema],
        arguments = [
            ctx.outputs.schema.path,
            generated_sources.path,
        ],
        tools = java_runtime.files,
        command = "%s cf $1 -C $2 schema" % jar_path,
        progress_message = "Assembling YANG compiled schema: %s" % ctx.attr.name,
    )

# Rule to generate YANG library from the specified set of YANG models.
_yang_library = rule(
    attrs = {
        "deps": attr.label_list(),
        "srcs": attr.label_list(allow_files = True),
        "model_id": attr.string(),
        "_yang_compiler": attr.label(
            executable = True,
            cfg = "host",
            allow_files = True,
            default = Label("//tools/build/bazel:onos_yang_compiler"),
        ),
        "_jdk": attr.label(
            default = Label("@bazel_tools//tools/jdk:current_java_runtime"),
            providers = [java_common.JavaRuntimeInfo],
        ),
    },
    outputs = {
        "srcjar": "model.srcjar",
        "schema": "schema.jar",
    },
    fragments = ["java"],
    implementation = _yang_library_impl,
)

def yang_library(
        name = None,
        deps = None,
        yang_srcs = None,
        java_srcs = None,
        custom_registrator = False,
        visibility = ["//visibility:public"]):
    if name == None:
        name = "onos-" + native.package_name().replace("/", "-")
    if yang_srcs == None:
        yang_srcs = native.glob(["src/main/yang/**/*.yang"])
    if java_srcs == None:
        java_srcs = native.glob(["src/main/java/**/*.java"])
    if deps == None:
        deps = []

    deps += CORE_DEPS + ONOS_YANG + [
        "@onos_yang_runtime//jar",
        "//apps/yang:onos-apps-yang",
    ]

    # Generate the Java sources from YANG model
    _yang_library(name = name + "-generate", model_id = name, deps = deps, srcs = yang_srcs)

    srcs = [name + "-generate"]

    if len(java_srcs):
        srcs.extend(java_srcs)
        # FIXME (carmelo): is this genrule really needed?
        # srcs += [name + "-srcjar"]
        # native.genrule(
        #     name = name + "-srcjar",
        #     srcs = java_srcs,
        #     outs = [name + ".srcjar"],
        #     cmd = "$(location //external:jar) cf $(location %s.srcjar) $(SRCS)" % name,
        #     tools = [
        #         "//external:jar",
        #     ]
        # )

    if not custom_registrator:
        srcs += [name + "-registrator"]
        native.genrule(
            name = name + "-registrator",
            outs = [REGISTRATOR_FILE],
            cmd = "echo '%s' > $(location %s)" % (REGISTRATOR, REGISTRATOR_FILE),
        )

    # Produce a Java library from the generated Java sources
    osgi_jar(
        name = name,
        srcs = srcs,
        resource_jars = [name + "-generate"],
        deps = deps,
        visibility = ["//visibility:public"],
        suppress_errorprone = True,
        suppress_checkstyle = True,
        suppress_javadocs = True,
    )

def yang_model(
        name = None,
        app_name = None,
        title = None,
        description = None,
        url = "http://onosproject.org/",
        custom_registrator = False,
        deps = None,
        yang_srcs = None,
        java_srcs = None,
        required_apps = [],
        visibility = ["//visibility:public"]):
    if name == None:
        name = "onos-" + native.package_name().replace("/", "-")

    yang_library(
        name = name,
        deps = deps,
        yang_srcs = yang_srcs,
        java_srcs = java_srcs,
        custom_registrator = custom_registrator,
        visibility = ["//visibility:public"],
    )

    onos_app(
        app_name = app_name,
        title = title,
        description = description,
        feature_name = name,
        version = ONOS_VERSION,
        url = url,
        category = "Models",
        included_bundles = [name],
        required_apps = required_apps + ["org.onosproject.yang"],
        visibility = ["//visibility:public"],
    )
