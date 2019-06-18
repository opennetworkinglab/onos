load("@bazel_tools//tools/jdk:default_java_toolchain.bzl", "default_java_toolchain")

# This is where we define the JDK used to build ONOS, as well as the language
# source and target values passed to javac. The :default_toolchain and
# :default_jdk are expected to be passed as arguments when invoking bazel build
# (see onos/.bazelrc)
default_java_toolchain(
    name = "default_toolchain",
    source_version = "11",
    target_version = "11",
    visibility = ["//visibility:public"],
)

alias(
    name = "default_jdk",
    actual = "@bazel_tools//tools/jdk:remote_jdk11",
    visibility = ["//visibility:public"],
)

py_binary(
    name = "osgi_feature_bundler",
    srcs = ["osgi_feature_bundler.py"],
    main = "osgi_feature_bundler.py",
    visibility = ["//visibility:public"],
)

py_binary(
    name = "onos_app_tools",
    srcs = ["onos_app_tools.py"],
    main = "onos_app_tools.py",
    visibility = ["//visibility:public"],
)

py_binary(
    name = "onos_app_bundler",
    srcs = ["onos_app_bundler.py"],
    main = "onos_app_bundler.py",
    visibility = ["//visibility:public"],
)

py_binary(
    name = "pom_generator",
    srcs = ["pom_generator.py"],
    main = "pom_generator.py",
    visibility = ["//visibility:public"],
)

java_binary(
    name = "onos_yang_compiler",
    main_class = "org.onosproject.yang.compiler.main.YangCompilerMain",
    visibility = ["//visibility:public"],
    runtime_deps = [
        "@onos_yang_compiler_main//jar",
        "@slf4j_nop//jar",  # TODO: Change to jdk14 after fixing verbosity
    ],
)
