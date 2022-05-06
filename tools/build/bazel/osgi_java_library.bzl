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
load("//tools/build/bazel:pom_file.bzl", "pom_file")
load("//tools/build/bazel:java_sources.bzl", "java_sources")
load("//tools/build/bazel:java_sources.bzl", "java_sources_alt")
load("//tools/build/bazel:javadoc.bzl", "javadoc")
load("//tools/build/bazel:minimal_jar.bzl", "minimal_jar")
load("@io_grpc_grpc_java//:java_grpc_library.bzl", "java_grpc_library")

def _auto_name():
    return "onos-" + native.package_name().replace("/", "-")

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
    return native.glob(["src/main/webapp/WEB-INF/web.xml"])

def _include_resources_to_string(include_resources):
    result = ""
    for (path, filename) in include_resources.items():
        result += (path + "=" + filename)
    return result

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
    name = ctx.attr.name
    group = ctx.attr.group
    version = ctx.attr.version
    license = ""
    import_packages = ctx.attr.import_packages
    bundle_classpath = ctx.attr.bundle_classpath
    exportPackages = "*"
    include_resources = ctx.attr.include_resources
    web_context = ctx.attr.web_context
    if web_context == None or web_context == "":
        web_context = "NONE"
    web_xml = ctx.attr.web_xml
    dynamicimportPackages = ""
    karaf_commands = ctx.attr.karaf_commands
    fragment_host = ctx.attr.fragment_host
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
        include_resources,
        web_context,
        web_xml_root_path,
        dynamicimportPackages,
        "classes",
        bundle_classpath,
        karaf_commands,
        fragment_host,
        # enable/disable osgi-wrap logging
        "false",
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
        "bundle_classpath": attr.string(),
        "web_context": attr.string(),
        "web_xml": attr.label_list(allow_files = True),
        "include_resources": attr.string(),
        "karaf_commands": attr.string(),
        "fragment_host": attr.string(),
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
    Implementation of the rule to generate cfgdef files from java class source
"""

def _cfgdef_impl(ctx):
    output_jar = ctx.outputs.cfgdef_jar.path

    arguments = [
        output_jar,
    ]

    for src in ctx.files.srcs:
        arguments.append(src.path)

    ctx.actions.run(
        inputs = ctx.files.srcs,
        outputs = [ctx.outputs.cfgdef_jar],
        arguments = arguments,
        progress_message = "Running cfgdef generator on: %s" % ctx.attr.name,
        executable = ctx.executable._cfgdef_generator_exe,
    )

"""
    Rule definition to call cfgdef generator to create the *.cfgdef files from java sources
"""
_cfgdef = rule(
    attrs = {
        "srcs": attr.label_list(allow_files = True),
        "_cfgdef_generator_exe": attr.label(
            executable = True,
            cfg = "host",
            allow_files = True,
            default = Label("//tools/build/cfgdef:cfgdef_generator"),
        ),
        "cfgdef_jar": attr.output(),
    },
    fragments = ["java"],
    implementation = _cfgdef_impl,
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
    output_dir = output_java[:output_java.find("generated-sources")]

    package_name = ctx.attr.package_name

    srcs_arg = ""

    for file in ctx.files.srcs:
        srcs_arg += file.path + ","

    # call swagger generator to make the swagger JSON and java files
    arguments = [
        srcs_arg,
        "",
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

    for file in ctx.files.srcs:
        srcs_arg += file.path + ","

    # call swagger generator to make the swagger JSON and java files
    arguments = [
        srcs_arg,
        "",
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
            default = Label("//tools/build/swagger:swagger_generator"),
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
            default = Label("//tools/build/swagger:swagger_generator"),
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
        visibility = ["//visibility:private"],
        generate_pom = False,
        fragment_host = ""):
    _bnd(
        name = name,
        source = jar,
        deps = deps,
        version = version,
        group = group,
        visibility = visibility,
        import_packages = import_packages,
        web_xml = None,
        fragment_host = fragment_host,
    )

    if generate_pom:
        pom_file(
            name = name + "-pom",
            artifact = name,
            deps = deps,
            visibility = visibility,
        )
    minimal_jar(name = name + "-sources", visibility = visibility)
    minimal_jar(name = name + "-javadoc", visibility = visibility)

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
        resource_jars = [],
        include_resources = {},
        test_srcs = None,
        exclude_tests = None,
        medium_tests = [],
        large_tests = [],
        enormous_tests = [],
        flaky_tests = [],
        test_resources = None,
        visibility = ["//visibility:public"],
        version = ONOS_VERSION,
        suppress_errorprone = False,
        suppress_checkstyle = False,
        suppress_javadocs = False,
        web_context = None,
        api_title = "",
        api_version = "",
        api_description = "",
        api_package = "",
        import_packages = None,
        bundle_classpath = "",
        karaf_command_packages = []):
    if name == None:
        name = _auto_name()
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
    tests_jar_deps = depset(deps + test_deps).to_list() + [name]
    all_test_deps = tests_jar_deps + [tests_name]
    web_xml = _webapp()

    native_srcs = srcs
    native_resources = resources

    if web_context != None and api_title != "":
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
        native_srcs = srcs + [name + "_swagger_java"]
        native_resources.append(name + "_swagger_json")

    javacopts = ["-XepDisableAllChecks"] if suppress_errorprone else []

    _cfgdef(
        name = name + "_cfgdef_jar",
        srcs = native_srcs,
        visibility = visibility,
        cfgdef_jar = name + "_cfgdef.jar",
    )

    # compile the Java code
    if len(resource_jars) > 0:
        native.java_library(
            name = name + "-native",
            srcs = native_srcs,
            resources = resource_jars + [name + "_cfgdef_jar"],
            deps = deps,
            visibility = visibility,
            javacopts = javacopts,
        )
    else:
        native.java_library(
            name = name + "-native",
            srcs = native_srcs,
            resources = native_resources + [name + "_cfgdef_jar"],
            deps = deps,
            visibility = visibility,
            javacopts = javacopts,
        )

    # NOTE that the additional resource_jars are modified by
    # osgi-wrap because java_library does not decompress them.
    karaf_command_packages_string = ",".join(karaf_command_packages)
    _bnd(
        name = name,
        source = name + "-native",
        deps = deps,
        version = version,
        group = group,
        visibility = visibility,
        import_packages = import_packages,
        bundle_classpath = bundle_classpath,
        web_context = web_context,
        web_xml = web_xml,
        include_resources = _include_resources_to_string(include_resources),
        karaf_commands = karaf_command_packages_string,
    )

    # rule for generating pom file for publishing
    pom_file(name = name + "-pom", artifact = name, deps = deps, visibility = visibility)

    # rule for building source jar
    if not suppress_javadocs:
        java_sources(name = name + "-sources", srcs = srcs, visibility = visibility)

    # rule for building javadocs
    if not suppress_javadocs:
        javadoc(name = name + "-javadoc", deps = deps, srcs = srcs, visibility = visibility)
    else:
        minimal_jar(name = name + "-javadoc", visibility = visibility)
        minimal_jar(name = name + "-sources", visibility = visibility)

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
            medium_tests = medium_tests,
            large_tests = large_tests,
            enormous_tests = enormous_tests,
            deps = all_test_deps,
        )

    if not suppress_checkstyle:
        checkstyle_test(
            name = name + "_checkstyle_test",
            srcs = srcs,
        )
        if test_srcs != []:
            checkstyle_test(
                name = name + "_checkstyle_tests_test",
                srcs = test_srcs,
            )

"""
    Creates an OSGI jar file from a set of source files.

    Args:
        name: Name of the rule to generate. Optional, defaults to a name based on the location in the source tree.
              For example apps/mcast/app becomes onos-apps-mcast-app
        deps: Dependencies of the generated jar file. Expressed as a list of targets
        import_packages: OSGI import list. Optional, comma separated list, defaults to "*"
        bundle_classpath: intended for including dependencies in our bundle, so that our bundle can be deployed standalone
        group: Maven group ID for the resulting jar file. Optional, defaults to 'org.onosproject'
        srcs: Source file(s) to compile. Optional list of targets, defaults to src/main/java/**/*.java
        resources_root: Relative path to the root of the tree of resources for this jar. Optional, defaults to src/main/resources
        resources: Resources to include in the jar file. Optional list of targets, defaults to all files beneath resources_root
        visibility: Visibility of the produced jar file to other BUILDs. Optional, defaults to public
        version: Version of the generated jar file. Optional, defaults to the current ONOS version
        suppress_errorprone: If true, don't run ErrorProne tests. Default is false
        suppress_checkstyle: If true, don't run checkstyle tests. Default is false
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
        resource_jars = [],
        include_resources = {},
        visibility = ["//visibility:public"],
        version = ONOS_VERSION,
        suppress_errorprone = False,
        suppress_checkstyle = False,
        suppress_javadocs = False,
        web_context = None,
        api_title = "",
        api_version = "",
        api_description = "",
        api_package = "",
        bundle_classpath = "",
        karaf_command_packages = []):
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
        resource_jars = resource_jars,
        test_srcs = [],
        exclude_tests = [],
        test_resources = [],
        visibility = visibility,
        suppress_errorprone = suppress_errorprone,
        suppress_checkstyle = suppress_checkstyle,
        suppress_javadocs = suppress_javadocs,
        version = version,
        import_packages = import_packages,
        api_title = api_title,
        api_version = api_version,
        api_description = api_description,
        api_package = api_package,
        web_context = web_context,
        bundle_classpath = bundle_classpath,
        karaf_command_packages = karaf_command_packages,
    )

"""
    Creates an OSGI jar file from a set of protobuf and gRPC libraries.

    Args:
        name: Name of the rule to generate. Optional, defaults to a name based on the location in the source tree.
              For example apps/mcast/app becomes onos-apps-mcast-app
        proto_libs: (required) list of proto_library targets which generated Java classes will be included to this OSGi
            jar. It is important that all the given targets reference to a single proto source files, for example
            only the first 2 rules are good:

            proto_library(
                name = "foo_proto",
                srcs = ["foo.proto"],
            )

            proto_library(
                name = "bar_proto",
                srcs = ["bar.proto"],
            )

            # THIS WILL NOT WORK
            proto_library(
                name = "foo_and_bar_proto",
                srcs = ["foo.proto", "bar.proto"],
            )

        grpc_proto_lib: (optional) proto_library target that contains the schema of a gRPC service. If not passed,
            the produced jar will NOT have any gRPC stub classes.
        deps: Dependencies of the generated jar file. Expressed as a list of targets
        group: Maven group ID for the resulting jar file. Optional, defaults to 'org.onosproject'
        visibility: Visibility of the produced jar file to other BUILDs. Optional, defaults to public
        version: Version of the generated jar file. Optional, defaults to the current ONOS version
"""

def osgi_proto_jar(
        proto_libs,
        grpc_proto_lib = None,
        name = None,
        deps = [],
        group = "org.onosproject",
        visibility = ["//visibility:public"],
        version = ONOS_VERSION,
        karaf_command_packages = []):
    if name == None:
        name = _auto_name()
    proto_name = name + "-java-proto"
    native.java_proto_library(
        name = proto_name,
        deps = proto_libs,
    )
    java_sources_alt(
        name = proto_name + "-srcjar",
        srcs = [":" + proto_name],
    )
    osgi_srcs = [
        proto_name + "-srcjar",
    ]
    base_deps = [
        "//deps:com_google_protobuf_protobuf_java",
    ]
    if grpc_proto_lib != None:
        grpc_name = name + "-java-grpc"
        java_grpc_library(
            name = grpc_name,
            srcs = [grpc_proto_lib],
            deps = [":" + proto_name],
        )
        java_sources_alt(
            name = grpc_name + "-srcjar",
            srcs = [":lib%s-src.jar" % grpc_name],
        )
        osgi_srcs.append(
            ":" + grpc_name + "-srcjar",
        )
        base_deps.extend([
            "@com_google_guava_guava//jar",
            "//deps:io_grpc_grpc_api_context",
            "//deps:io_grpc_grpc_stub",
            "//deps:io_grpc_grpc_protobuf",
            "@javax_annotation_javax_annotation_api//jar",
        ])
    osgi_jar(
        name = name,
        srcs = osgi_srcs,
        deps = base_deps + deps,
        group = group,
        visibility = visibility,
        version = version,
        suppress_errorprone = True,
        suppress_checkstyle = True,
        suppress_javadocs = True,
        karaf_command_packages = karaf_command_packages,
    )
