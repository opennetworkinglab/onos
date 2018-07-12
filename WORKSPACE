load("//tools/build/bazel:generate_workspace.bzl", "generated_maven_jars")
load("//tools/build/bazel:p4lang_workspace.bzl", "generate_p4lang")

generated_maven_jars()
generate_p4lang()

git_repository(
        name = "build_bazel_rules_nodejs",
        remote = "https://github.com/bazelbuild/rules_nodejs.git",
        tag = "0.10.0", # check for the latest tag when you install
)

load("@build_bazel_rules_nodejs//:defs.bzl", "node_repositories")
node_repositories(package_json = ["//tools/gui:package.json"])

ONOS_VERSION = '1.14.0-SNAPSHOT'
