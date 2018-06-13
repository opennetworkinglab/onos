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

# Implementation of the rule to call bnd to make an OSGI jar file
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
    group = ctx.attr.package_name_root
    version = ctx.attr.version
    license = ""
    import_packages = ctx.attr.import_packages
    exportPackages = "*"
    includeResources = ""
    webContext = "NONE"
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
        webContext,
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

_bnd = rule(
    attrs = {
        "deps": attr.label_list(),
        "version": attr.string(),
        "package_name_root": attr.string(),
        "source": attr.label(),
        "import_packages": attr.string(),
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

def wrapped_osgi_jar(name, jar, deps, version = ONOS_VERSION, package_name_root = "org.onosproject", import_packages = "*", visibility = ["//visibility:private"]):
    _bnd(name = name, source = jar, deps = deps, version = version, package_name_root = package_name_root, visibility = visibility, import_packages = import_packages)

def osgi_jar_with_tests(
        name = None,
        deps = None,
        test_deps = None,
        package_name_root = "org.onosproject",
        srcs = None,
        resources_root = None,
        resources = None,
        test_srcs = None,
        exclude_tests = None,
        test_resources = None,
        visibility = ["//visibility:public"],
        version = ONOS_VERSION,
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

    native.java_library(name = name + "-native", srcs = srcs, resources = resources, deps = deps, visibility = visibility)
    _bnd(
        name = name,
        source = name + "-native",
        deps = deps,
        version = version,
        package_name_root = package_name_root,
        visibility = visibility,
        import_packages = import_packages,
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

def osgi_jar(
        name = None,
        deps = None,
        import_packages = None,
        package_name_root = "org.onosproject",
        srcs = None,
        resources_root = None,
        resources = None,
        visibility = ["//visibility:public"],
        version = ONOS_VERSION,
        # TODO - implement these for swagger and web.xml
        web_context = "",
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
        package_name_root = package_name_root,
        srcs = srcs,
        resources = resources,
        resources_root = resources_root,
        test_srcs = [],
        exclude_tests = [],
        test_resources = [],
        visibility = visibility,
        version = version,
        import_packages = import_packages,
    )
