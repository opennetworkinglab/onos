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

load("//tools/build/bazel:generate_workspace.bzl", "COMPILE", "TEST")
load("//tools/build/bazel:variables.bzl", "ONOS_VERSION")
load("//tools/build/bazel:generate_test_rules.bzl", "generate_test_rules")
load("//tools/build/bazel:checkstyle.bzl", "checkstyle_test")

def _all_java_sources():
    return native.glob(["src/main/java/**/*.java"])

def _all_java_test_sources():
    return native.glob(["src/test/java/**/*.java"])

def _all_test_resources():
    return native.glob(["src/test/resources/**"])

def _all_resources(resources_root):
    if resources_root == None:
        return native.glob(["src/main/resources/**"])
    else:
        return native.glob([resources_root + "**"])

def _webapp():
    return native.glob(["src/main/webapp/**"])

"""
    Implementation of the rule to call bnd to make an OSGI jar file
"""
def _bnd_impl(ctx):
    if (len(ctx.files.source) == 1):
        input_file = ctx.files.source[0]
    else:
        # this is a list of inputs. The one we want is the last one
        # in the list that isn't a source jar
        for file in reversed(ctx.files.source):
            if ("-src" in file.path):
                continue
            else:
                input_file = file
                break

    jar = input_file.path
    output = ctx.outputs.osgi_jar.path
    name = ctx.attr.source.label.name
    group = ctx.attr.group
    version = ctx.attr.version
    license = ""
    import_packages = ctx.attr.import_packages
    exportPackages = "*"
    includeResources = ""
    web_context = ctx.attr.web_context
    if web_context == None or web_context == "":
        web_context = "NONE"
    web_xml = ctx.attr.web_xml
    dynamicimportPackages = ""
    cp = ""

    inputDependencies = [input_file]

    # determine the dependencies and build the class path
    for dep in ctx.attr.deps:
        if java_common.provider in dep:
            file = dep.files.to_list()[0]

            if cp:
                cp += ":"
            cp += file.path
            inputDependencies = inputDependencies + [file]

    # extract the class files for use by bnd
    classes = ctx.actions.declare_file("classes" + ctx.label.name.replace("/", "-"))
    classesPath = classes.path
    jarCommand = "mkdir -p %s && cp %s %s && cd %s && jar xf *.jar" % (classesPath, jar, classesPath, classesPath)
    ctx.actions.run_shell(
        inputs = inputDependencies,
        outputs = [classes],
        command = jarCommand,
        progress_message = "Expanding jar file: %s" % jar,
    )
    inputDependencies += [classes]
    web_xml_root_path = ""
    if len(web_xml) != 0:
        web_xml_root = web_xml[0].files.to_list()[0]
        inputDependencies += [web_xml_root]
        web_xml_root_path = web_xml_root.path.replace("WEB-INF/web.xml", "")

    # call bnd to make the OSGI jar file
    arguments = [
        jar,
        output,
        cp,
        name,
        group,
        version,
        license,
        import_packages,
        exportPackages,
        includeResources,
        web_context,
        web_xml_root_path,
        dynamicimportPackages,
        classesPath,
    ]

    ctx.actions.run(
        inputs = inputDependencies,
        outputs = [ctx.outputs.osgi_jar],
        arguments = arguments,
        progress_message = "Running bnd wrapper on: %s" % ctx.attr.name,
        executable = ctx.executable._bnd_exe,
    )

    deps = []
    if java_common.provider in ctx.attr.source:
        deps.append(ctx.attr.source[java_common.provider])
    deps_provider = java_common.merge(deps)
    return struct(
        providers = [deps_provider],
    )

"""
    Rule definition for calling bnd to make an OSGi jar file.
"""
_bnd = rule(
    attrs = {
        "deps": attr.label_list(),
        "version": attr.string(),
        "group": attr.string(),
        "source": attr.label(),
        "import_packages": attr.string(),
        "web_context": attr.string(),
        "web_xml": attr.label_list(allow_files = True),
        "_bnd_exe": attr.label(
            executable = True,
            cfg = "host",
            allow_files = True,
            default = Label("//utils/osgiwrap:osgi-jar"),
        ),
    },
    fragments = ["java"],
    outputs = {
        "osgi_jar": "lib%{name}.jar",
    },
    implementation = _bnd_impl,
)

"""
    Implementation of the rule to call swagger generator to create the registrator java class source
"""
def _swagger_java_impl(ctx):
    api_title = ctx.attr.api_title
    api_version = ctx.attr.api_version
    api_description = ctx.attr.api_description
    api_package = ctx.attr.api_package
    web_context = ctx.attr.web_context

    output_java = ctx.outputs.swagger_java.path
    output_dir = output_java [:output_java.find("generated-sources")]

    package_name = ctx.attr.package_name

    srcs_arg = ""
    resources_arg = ""
    input_dependencies = []

    for file in ctx.files.srcs:
        srcs_arg += file.path + ","
        input_dependencies.append(file)

    for resource in resources_arg:
        resources_arg += resource.path + ","

    # call swagger generator to make the swagger JSON and java files
    arguments = [
        srcs_arg,
        resources_arg,
        "",
        package_name + "/src/main/resources",
        output_dir,
        output_dir,
        web_context,
        api_title,
        api_version,
        api_package,
        api_description,
    ]

    ctx.actions.run(
        inputs = ctx.files.srcs,
        outputs = [ctx.outputs.swagger_java],
        arguments = arguments,
        progress_message = "Running swagger generator on: %s" % ctx.attr.name,
        executable = ctx.executable._swagger_generator_exe,
    )

"""
Implementation of the rule to call swagger generator for swagger.json file
"""
def _swagger_json_impl(ctx):
    api_title = ctx.attr.api_title
    api_version = ctx.attr.api_version
    api_description = ctx.attr.api_description
    api_package = ctx.attr.api_package
    web_context = ctx.attr.web_context

    output_json = ctx.outputs.swagger_json.path
    output_dir = output_json[:output_json.find("swagger.json")]

    package_name = ctx.attr.package_name

    srcs_arg = ""
    resources_arg = ""
    input_dependencies = []

    for file in ctx.files.srcs:
        srcs_arg += file.path + ","
        input_dependencies.append(file)

    for resource in resources_arg:
        resources_arg += resource.path + ","

    # call swagger generator to make the swagger JSON and java files
    arguments = [
        srcs_arg,
        resources_arg,
        "",
        package_name + "/src/main/resources",
        output_dir,
        output_dir,
        web_context,
        api_title,
        api_version,
        api_package,
        api_description,
    ]

    ctx.actions.run(
        inputs = ctx.files.srcs,
        outputs = [ctx.outputs.swagger_json],
        arguments = arguments,
        progress_message = "Running swagger generator on: %s" % ctx.attr.name,
        executable = ctx.executable._swagger_generator_exe,
    )

"""
    Rule definition to call swagger generator to create the registrator java class source
"""
_swagger_java = rule(
    attrs = {
        "srcs": attr.label_list(allow_files = True),
        "package_name": attr.string(),
        "api_title": attr.string(),
        "api_version": attr.string(),
        "api_description": attr.string(),
        "api_package": attr.string(),
        "web_context": attr.string(),
        "_swagger_generator_exe": attr.label(
            executable = True,
            cfg = "host",
            allow_files = True,
            default = Label("//tools/build/buck-plugin:swagger_generator"),
        ),
        "swagger_java": attr.output(),
    },
    fragments = ["java"],
    implementation = _swagger_java_impl,
)

"""
    Rule definition to call swagger generator to create the swagger JSON
"""
_swagger_json = rule(
    attrs = {
        "srcs": attr.label_list(allow_files = True),
        "package_name": attr.string(),
        "api_title": attr.string(),
        "api_version": attr.string(),
        "api_description": attr.string(),
        "api_package": attr.string(),
        "web_context": attr.string(),
        "_swagger_generator_exe": attr.label(
            executable = True,
            cfg = "host",
            allow_files = True,
            default = Label("//tools/build/buck-plugin:swagger_generator"),
        ),
        "swagger_json": attr.output(),
    },
    fragments = ["java"],
    implementation = _swagger_json_impl,
)

"""
    Converts a jar file to an OSGI compatible jar file.

    Args:
        name: name of the rule to create the OSGI jar file - required
        jar: jar file to convert - required target
        deps: dependencies needed by the jar file - required list of targets
        version: Version of the generated jar file. Optional, defaults to the current ONOS version
        group: Maven group ID for the resulting jar file. Optional, defaults to 'org.onosproject'
        import_packages: OSGI import list. Optional, comma separated list, defaults to "*"
        visibility: Visibility of the produced jar file to other BUILDs. Optional, defaults to private
"""
def wrapped_osgi_jar(
        name,
        jar,
        deps,
        version = ONOS_VERSION,
        group = "org.onosproject",
        import_packages = "*",
        visibility = ["//visibility:private"]):
    _bnd(
        name = name,
        source = jar,
        deps = deps,
        version = version,
        group = group,
        visibility = visibility,
        import_packages = import_packages,
        web_xml = None,
    )

"""
    Creates an OSGI jar and test jar file from a set of source and test files.
    See osgi_jar() for a description of shared parameters.
    Args:
        test_srcs: Test source file(s) to compile. Optional list of targets, defaults to src/test/java/**/*.java
        test_deps: Dependencies for the test jar. Optional list of targets, defaults to a common set of dependencies
        test_resources: Resources to include in the test jar. Optional list of targets, defaults to src/test/resources/**
        exclude_tests: Tests that should not be run. Useful for excluding things like test files without any @Test methods.
                       Optional ist of targets, defaults to []
"""
def osgi_jar_with_tests(
        name = None,
        deps = None,
        test_deps = None,
        group = "org.onosproject",
        srcs = None,
        resources_root = None,
        resources = None,
        test_srcs = None,
        exclude_tests = None,
        test_resources = None,
        visibility = ["//visibility:public"],
        version = ONOS_VERSION,
        web_context = None,
        api_title = "",
        api_version = "",
        api_description = "",
        api_package = "",
        import_packages = None):
    if name == None:
        name = "onos-" + native.package_name().replace("/", "-")
    if srcs == None:
        srcs = _all_java_sources()
    if resources == None:
        resources = _all_resources(resources_root)
    if test_srcs == None:
        test_srcs = _all_java_test_sources()
    if test_resources == None:
        test_resources = _all_test_resources()
    if exclude_tests == None:
        exclude_tests = []
    if deps == None:
        deps = COMPILE
    if test_deps == None:
        test_deps = TEST
    if import_packages == None:
        import_packages = "*"
    tests_name = name + "-tests"
    tests_jar_deps = list(depset(deps + test_deps)) + [name]
    all_test_deps = tests_jar_deps + [tests_name]
    web_xml = _webapp()

    native_srcs = srcs
    native_resources = resources
    if web_context != None and api_title != None and len(resources) != 0:
        # generate Swagger files if needed
        _swagger_java(
            name = name + "_swagger_java",
            srcs = srcs + resources,
            package_name = native.package_name(),
            api_title = api_title,
            api_version = api_version,
            api_description = api_description,
            web_context = web_context,
            api_package = api_package,
            swagger_java = ("src/main/resources/apidoc/generated-sources/" +
                           api_package.replace(".", "/") +
                           "/ApiDocRegistrator.java").replace("//", "/"),
        )
        _swagger_json(
            name = name + "_swagger_json",
            srcs = srcs + resources,
            package_name = native.package_name(),
            api_title = api_title,
            api_version = api_version,
            api_description = api_description,
            web_context = web_context,
            api_package = api_package,
            swagger_json = "src/main/resources/apidoc/swagger.json",
        )
        native_resources = []
        for r in resources:
             if not "definitions" in r:
                native_resources.append(r)
        native_srcs = srcs + [ name + "_swagger_java" ]
        native_resources.append(name + "_swagger_json");

    # compile the Java code
    native.java_library(name = name + "-native", srcs = native_srcs, resources = native_resources, deps = deps, visibility = visibility)

    _bnd(
        name = name,
        source = name + "-native",
        deps = deps,
        version = version,
        group = group,
        visibility = visibility,
        import_packages = import_packages,
        web_context = web_context,
        web_xml = web_xml,
    )
    if test_srcs != []:
        native.java_library(
            name = tests_name,
            srcs = test_srcs,
            resources = test_resources,
            deps = tests_jar_deps,
            visibility = visibility,
        )

        generate_test_rules(
            name = name + "-tests-gen",
            test_files = test_srcs,
            exclude_tests = exclude_tests,
            deps = all_test_deps,
        )

    checkstyle_test(
        name = name + "_checkstyle_test",
        srcs = srcs,
    )

"""
    Creates an OSGI jar file from a set of source files.

    Args:
        name: Name of the rule to generate. Optional, defaults to a name based on the location in the source tree.
              For example apps/mcast/app becomes onos-apps-mcast-app
        deps: Dependencies of the generated jar file. Expressed as a list of targets
        import_packages: OSGI import list. Optional, comma separated list, defaults to "*"
        group: Maven group ID for the resulting jar file. Optional, defaults to 'org.onosproject'
        srcs: Source file(s) to compile. Optional list of targets, defaults to src/main/java/**/*.java
        resources_root: Relative path to the root of the tree of resources for this jar. Optional, defaults to src/main/resources
        resources: Resources to include in the jar file. Optional list of targets, defaults to all files beneath resources_root
        visibility: Visibility of the produced jar file to other BUILDs. Optional, defaults to public
        version: Version of the generated jar file. Optional, defaults to the current ONOS version
        web_context: Web context for a WAB file if needed. Only needed if the jar file provides a REST API. Optional string
        api_title: Swagger API title. Optional string, only used if the jar file provides a REST API and has swagger annotations
        api_version: Swagger API version. Optional string, only used if the jar file provides a REST API and has swagger annotations
        api_description: Swagger API description. Optional string, only used if the jar file provides a REST API and has swagger annotations
        api_package: Swagger API package name. Optional string, only used if the jar file provides a REST API and has swagger annotations
"""
def osgi_jar(
        name = None,
        deps = None,
        import_packages = None,
        group = "org.onosproject",
        srcs = None,
        resources_root = None,
        resources = None,
        visibility = ["//visibility:public"],
        version = ONOS_VERSION,
        web_context = None,
        api_title = "",
        api_version = "",
        api_description = "",
        api_package = ""):
    if srcs == None:
        srcs = _all_java_sources()
    if deps == None:
        deps = COMPILE

    osgi_jar_with_tests(
        name = name,
        deps = deps,
        test_deps = [],
        group = group,
        srcs = srcs,
        resources = resources,
        resources_root = resources_root,
        test_srcs = [],
        exclude_tests = [],
        test_resources = [],
        visibility = visibility,
        version = version,
        import_packages = import_packages,
        api_title = api_title,
        api_version = api_version,
        api_description = api_description,
        api_package = api_package,
        web_context = web_context,
    )
