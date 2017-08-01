load("//tools/build/bazel:generate_workspace.bzl", "COMPILE", "TEST")
load("//tools/build/bazel:variables.bzl", "ONOS_VERSION")
load("//tools/build/bazel:generate_test_rules.bzl", "generate_test_rules")

def all_java_sources():
  return native.glob(["src/main/java/**/*.java"])

def all_java_test_sources():
  return native.glob(["src/test/java/**/*.java"])

def all_test_resources():
  return native.glob(["src/test/resources/**"])

def all_resources(resources_root):
  if resources_root == None:
    return native.glob(["src/main/resources/**"])
  else:
    return native.glob([resources_root + '**'])

# Implementation of the rule to call bnd to make an OSGI jar file
def _bnd_impl(ctx):

  jar = ctx.file.source.path
  output = ctx.outputs.osgi_jar.path
  cp = ""
  name = ctx.attr.source.label.name
  group = ctx.attr.package_name_root
  version = ctx.attr.version
  license = ""
  importPackages = "*"
  exportPackages = "*"
  includeResources = ""
  webContext = "NONE"
  dynamicimportPackages = ""

  inputDependencies = [ctx.file.source]

  # determine the dependencies and build the class path
  for dep in ctx.attr.deps:
      file = dep.java.outputs.jars[0].class_jar

      if cp:
         cp += ":"
      cp += file.path
      inputDependencies = inputDependencies + [file]

  # extract the class files for use by bnd
  classes = ctx.actions.declare_file("classes")
  classesPath = classes.path
  jarCommand = "mkdir -p %s && cp %s %s && cd %s && jar xf *.jar" % (classesPath, jar, classesPath, classesPath)
  ctx.actions.run_shell(
      inputs=inputDependencies,
            outputs=[classes],
            command=jarCommand,
            progress_message="Expanding jar file: %s" % jar,
        )
  inputDependencies += [classes]

  # call bnd to make the OSGI jar file
  arguments=[
            jar,
            output,
            cp,
            name,
            group,
            version,
            license,
            importPackages,
            exportPackages,
            includeResources,
            webContext,
            dynamicimportPackages,
            classesPath,
        ]
  ctx.actions.run(
      inputs=inputDependencies,
      outputs=[ctx.outputs.osgi_jar],
      arguments=arguments,
      progress_message="Running bnd wrapper on: %s" % ctx.attr.name,
      executable=ctx.executable._bnd_exe,
  )

bnd = rule(
    attrs = {
        "deps": attr.label_list(),
        "version": attr.string(),
        "package_name_root": attr.string(),
        "source": attr.label(allow_single_file = True),
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

def _fwd_bnd(name, source, deps, version, package_name_root, visibility):
  bnd(name = name, source = source, deps = deps, version = version, package_name_root = package_name_root, visibility = visibility)

def wrapped_osgi_library(name, jar, deps, version = ONOS_VERSION, package_name_root = "org.onosproject", visibility = [ "//visibility:private" ]):
  _fwd_bnd(name, jar, deps, version, package_name_root, visibility)

def osgi_jar_with_tests(name = None,
                        deps = None,
                        test_deps = None,
                        package_name_root = "org.onosproject",
                        srcs = None,
                        resources_root = None,
                        resources = None,
                        test_srcs = None,
                        exclude_tests = None,
                        test_resources = None,
                        visibility = [ "//visibility:public" ],
                        version = ONOS_VERSION):
  if name == None:
    name = "onos-" + native.package_name().replace("/", "-")
  if srcs == None:
    srcs = all_java_sources()
  if resources == None:
    resources = all_resources(resources_root)
  if test_srcs == None:
    test_srcs = all_java_test_sources()
  if test_resources == None:
    test_resources = all_test_resources()
  if exclude_tests == None:
    exclude_tests = []
  if deps == None:
    deps = COMPILE
  if test_deps == None:
    test_deps = TEST
  tests_name = name + '-tests'
  tests_jar_deps = list(depset(deps + test_deps)) + [ name ]
  all_test_deps = tests_jar_deps + [ tests_name ]

  native.java_library(name = name, srcs = srcs, resources = resources, deps = deps, visibility = visibility)
  _fwd_bnd(name + '-osgi', name, deps, version, package_name_root, visibility)
  if test_srcs != []:
    native.java_library(name = tests_name,
                        srcs = test_srcs,
                        resources = test_resources,
                        deps = tests_jar_deps,
                        visibility = visibility)

    generate_test_rules(
      name = name + "-tests-gen",
      test_files = test_srcs,
      exclude_tests = exclude_tests,
      deps = all_test_deps
    )

def osgi_jar(name = None,
             deps = None,
             package_name_root = "org.onosproject",
             srcs = None,
             resources_root = None,
             resources = None,
             visibility = [ "//visibility:public" ],
             version = ONOS_VERSION):
    if srcs == None:
      srcs = all_java_sources()
    if deps == None:
      deps = COMPILE

    osgi_jar_with_tests(name = name,
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
                        version = version)

