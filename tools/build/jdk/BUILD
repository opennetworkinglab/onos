# This is where we define the JDK used to build ONOS, as well as the language
# source and target values passed to javac. The :default_toolchain and
# :default_jdk are expected to be passed as arguments when invoking bazel build
# (see onos/.bazelrc)

load("@bazel_tools//tools/jdk:default_java_toolchain.bzl", "default_java_toolchain")

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

# We use the following rule to package the same JDK used for building and make
# it available for external scripts as their JAVA_HOME, such as for `bazel run
# onos-local`.

# FIXME: @bazel_tools//tools/jdk:current_java_runtime should be fixed upstream
#  to include the missing JDK conf/security files, required by Karaf. The
#  workaround for now is to check in those files here and include them in the
#  produced JDK tar.

genrule(
    name = "default_jdk_tar",
    srcs = [
        "@bazel_tools//tools/jdk:current_java_runtime",
        ":conf-security",
    ],
    outs = ["default_jdk.tar.gz"],
    cmd = "mkdir default_jdk && " +
          "mv $(JAVABASE)/* default_jdk/ && " +
          "mv tools/build/jdk/* default_jdk/ && " +
          "tar -hczf $(location default_jdk.tar.gz) default_jdk",
    output_to_bindir = True,
    toolchains = ["@bazel_tools//tools/jdk:current_java_runtime"],
    visibility = ["//visibility:public"],
)

filegroup(
    name = "conf-security",
    srcs = glob(["conf/security/**"]),
)
