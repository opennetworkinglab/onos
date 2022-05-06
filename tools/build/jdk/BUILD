# We use the following rule to package the same JDK used for building and make
# it available for external scripts as their JAVA_HOME, such as for `bazel run
# onos-local`.

# FIXME: @bazel_tools//tools/jdk:current_java_runtime should be fixed upstream
#  to include the missing JDK conf/security files, required by Karaf. The
#  workaround for now is to check in those files here and include them in the
#  produced JDK tar.

genrule(
    name = "current_jdk_tar",
    srcs = [
        "@bazel_tools//tools/jdk:current_java_runtime",
        ":conf-security",
    ],
    outs = ["current_jdk.tar.gz"],
    cmd = "mkdir current_jdk && " +
          "cp -r $(JAVABASE)/* current_jdk/ && " +
          "cp -r tools/build/jdk/* current_jdk/ && " +
          "tar -hczf $(location current_jdk.tar.gz) current_jdk",
    output_to_bindir = True,
    toolchains = ["@bazel_tools//tools/jdk:current_java_runtime"],
    visibility = ["//visibility:public"],
)

filegroup(
    name = "conf-security",
    srcs = glob(["conf/security/**"]),
)
