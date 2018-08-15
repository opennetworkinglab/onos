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

load("//tools/build/bazel:generate_workspace.bzl", "maven_coordinates")
load(
    "//tools/build/bazel:variables.bzl",
    "APP_PREFIX",
    "DEFAULT_APP_CATEGORY",
    "ONOS_ARTIFACT_BASE",
    "ONOS_GROUP_ID",
    "ONOS_ORIGIN",
    "ONOS_VERSION",
)

# Implementation of the rule to build an ONOS application OAR file
def _onos_oar_impl(ctx):
    app_xml_file = ctx.attr.app_xml.files.to_list()[0]
    feature_xml_file = ctx.attr.feature_xml.files.to_list()[0]
    feature_xml_coords = ctx.attr.feature_xml_coords

    jar_file_args = []
    jar_files = []
    for bundle in ctx.attr.included_bundles:
        jar_file = bundle.files.to_list()[0]
        jar_files.append(jar_file)
        jar_file_coords = maven_coordinates(bundle.label)
        jar_file_args.append(jar_file.path)
        jar_file_args.append(jar_file_coords)

    arguments = [
        ctx.outputs.app_oar.path,
        feature_xml_file.path,
        feature_xml_coords,
        app_xml_file.path,
        "NONE",
    ] + jar_file_args

    ctx.actions.run(
        inputs = [app_xml_file, feature_xml_file] + jar_files,
        outputs = [ctx.outputs.app_oar],
        arguments = arguments,
        progress_message = "Running oar file generator: %s" % ctx.attr.name,
        executable = ctx.executable._onos_app_bundler,
    )

# Implementation of the rule to build an app.xml desriptor file for ONOS app
def _onos_app_xml_impl(ctx):
    # Build-up arguments for the app.xml and feature.xml generator
    arguments = [
        "-n",
        ctx.attr.feature_coords,
        "-a",
        ctx.attr.app_name,
        "-o",
        ctx.attr.origin,
        "-c",
        ctx.attr.category,
        "-u",
        ctx.attr.url,
        "-v",
        ctx.attr.version,
        "-t",
        ctx.attr.title,
        "-D",
        ctx.attr.description,
    ]

    for bundle in ctx.attr.included_bundles:
        arguments += ["-b", maven_coordinates(bundle.label)]
    for bundle in ctx.attr.excluded_bundles:
        arguments += ["-e", maven_coordinates(bundle.label)]
    for app in ctx.attr.required_apps:
        arguments += ["-d", app]

    if ctx.attr.security != "":
        arguments += ["-s", ctx.attr.security]

    ctx.actions.run(
        inputs = [],
        outputs = [ctx.outputs.app_xml],
        arguments = arguments + ["-A", "-O", ctx.outputs.app_xml.path],
        progress_message = "Generating app.xml descriptor for app: %s" % ctx.attr.name,
        executable = ctx.executable._onos_app_tools,
    )

# Implementation of the rule to build the feature.xml file for ONOS app
def _onos_feature_xml_impl(ctx):
    # Build-up arguments
    arguments = [
        "-n",
        ctx.attr.feature_coords,
        "-a",
        ctx.attr.app_name,
        "-u",
        ctx.attr.url,
        "-v",
        ctx.attr.version,
        "-t",
        ctx.attr.title,
        "-D",
        ctx.attr.description,
    ]

    for bundle in ctx.attr.included_bundles:
        arguments += ["-b", maven_coordinates(bundle.label)]
    for feature in ctx.attr.required_features:
        arguments += ["-f", feature]

    ctx.actions.run(
        inputs = [],
        outputs = [ctx.outputs.feature_xml],
        arguments = arguments + ["-F", "-O", ctx.outputs.feature_xml.path],
        progress_message = "Generating feature.xml for app: %s" % ctx.attr.name,
        executable = ctx.executable._onos_app_tools,
    )

# Rule to generate the ONOS app OAR file.
_onos_oar = rule(
    attrs = {
        "deps": attr.label_list(),
        "version": attr.string(),
        "package_name_root": attr.string(),
        "source": attr.label(),
        "app_xml": attr.label(),
        "feature_xml": attr.label(),
        "feature_xml_coords": attr.string(),
        "included_bundles": attr.label_list(),
        "_onos_app_bundler": attr.label(
            executable = True,
            cfg = "host",
            allow_files = True,
            default = Label("//tools/build/bazel:onos_app_bundler"),
        ),
    },
    outputs = {
        "app_oar": "%{name}.oar",
    },
    implementation = _onos_oar_impl,
)

# Rule to generate app.xml descriptor for an ONOS application.
_onos_app_xml = rule(
    attrs = {
        "app_name": attr.string(),
        "origin": attr.string(),
        "version": attr.string(),
        "title": attr.string(),
        "category": attr.string(),
        "url": attr.string(),
        "feature_coords": attr.string(),
        "description": attr.string(),
        "apps": attr.label_list(),
        "included_bundles": attr.label_list(),
        "excluded_bundles": attr.label_list(),
        "required_apps": attr.string_list(),
        "security": attr.string(),
        "_onos_app_tools": attr.label(
            executable = True,
            cfg = "host",
            allow_files = True,
            default = Label("//tools/build/bazel:onos_app_tools"),
        ),
    },
    outputs = {
        "app_xml": "%{name}.xml",
    },
    implementation = _onos_app_xml_impl,
)

# Rule to generate feature.xml descriptor for an ONOS application.
_onos_feature_xml = rule(
    attrs = {
        "app_name": attr.string(),
        "version": attr.string(),
        "title": attr.string(),
        "category": attr.string(),
        "url": attr.string(),
        "feature_coords": attr.string(),
        "description": attr.string(),
        "included_bundles": attr.label_list(),
        "required_features": attr.string_list(),
        "_onos_app_tools": attr.label(
            executable = True,
            cfg = "host",
            allow_files = True,
            default = Label("//tools/build/bazel:onos_app_tools"),
        ),
    },
    outputs = {
        "feature_xml": "%{name}.xml",
    },
    implementation = _onos_feature_xml_impl,
)

def _basename(path):
    paths = path.split("/")
    return paths[len(paths) - 1]

def _get_base_path():
    return native.package_name()

def _get_name():
    base_path = _get_base_path()
    return ONOS_ARTIFACT_BASE + base_path.replace("/", "-")

def _get_app_name():
    base_path = _get_base_path()
    return APP_PREFIX + _basename(base_path)

def _local_label(name, suffix):
    base_label_name = "//" + native.package_name() + ":"
    return base_label_name + name + suffix

# Macro to build an ONOS application OAR file.
def onos_app(
        app_name = None,
        name = None,
        title = None,
        version = ONOS_VERSION,
        origin = ONOS_ORIGIN,
        category = DEFAULT_APP_CATEGORY,
        url = None,
        description = None,
        feature_coords = None,
        required_features = ["onos-api"],
        required_apps = [],
        included_bundles = None,
        excluded_bundles = [],
        visibility = ["//visibility:public"],
        security = None,
        **kwargs):
    if name == None:
        name = _get_name()

    if app_name == None:
        app_name = _get_app_name()

    maven_coords = "%s:%s:oar:%s" % (ONOS_GROUP_ID, name, ONOS_VERSION)
    feature_xml_coords = "mvn:%s:%s:xml:features:%s" % (ONOS_GROUP_ID, name, ONOS_VERSION)

    if title == None:
        print("Missing title for %s" % _get_name())
        title = _get_app_name()

    if included_bundles == None:
        target = _local_label(name, "")
        included_bundles = [target]

    if not feature_coords:
        feature_coords = "mvn:%s:%s:%s" % (ONOS_GROUP_ID, name, ONOS_VERSION)

    # TODO - intra app dependecies
    apps = []

    # Generate the app.xml file
    _onos_app_xml(
        name = name + "-app-xml",
        app_name = app_name,
        origin = origin,
        version = version,
        title = title,
        category = category,
        url = url,
        feature_coords = feature_coords,
        description = description,
        apps = apps,
        included_bundles = included_bundles,
        excluded_bundles = excluded_bundles,
        required_apps = required_apps,
    )

    # Generate feature.xml file
    _onos_feature_xml(
        name = name + "-feature-xml",
        app_name = app_name,
        version = version,
        title = title,
        feature_coords = feature_coords,
        description = description,
        included_bundles = included_bundles,
        required_features = required_features,
    )

    # Generate the OAR file based on the app.xml, features.xml, and specified
    # bundles to be included.
    _onos_oar(
        name = name + "-oar",
        included_bundles = included_bundles,
        app_xml = Label(_local_label(name, "-app-xml")),
        feature_xml = Label(_local_label(name, "-feature-xml")),
        feature_xml_coords = feature_xml_coords,
        visibility = visibility,
    )
