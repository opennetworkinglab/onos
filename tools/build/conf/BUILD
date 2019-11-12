load("//tools/build/bazel:pom_file.bzl", "pom_file")

checkstyle_source = "src/main/resources/onos/checkstyle.xml"

suppression_source = "src/main/resources/onos/suppressions.xml"

header_source = "src/main/resources/onos/onos-java.header"

filegroup(
    name = "checkstyle_xml",
    srcs = [checkstyle_source],
    visibility = ["//visibility:public"],
)

filegroup(
    name = "suppressions_xml",
    srcs = [suppression_source],
    visibility = ["//visibility:public"],
)

filegroup(
    name = "onos_java_header",
    srcs = [header_source],
    visibility = ["//visibility:public"],
)

java_library(
    name = "onos-build-conf",
    resources = glob(["src/main/resources/onos/**"]),
    visibility = ["//visibility:public"],
)

pom_file(
    name = "onos-build-conf-pom",
    artifact = "onos-build-conf",
    visibility = ["//visibility:public"],
)
