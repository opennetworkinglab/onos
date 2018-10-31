workspace(name = "org_onosproject_onos")

load("//tools/build/bazel:bazel_version.bzl", "check_bazel_version")

check_bazel_version()

load("//tools/build/bazel:local_jar.bzl", "local_atomix", "local_jar")

# Use this to build against locally built arbitrary 3rd party artifacts
#local_jar(
#    name = "atomix",
#    path = "/Users/tom/atomix/core/target/atomix-3.0.8-SNAPSHOT.jar",
#)

# Use this to build against locally built Atomix
#local_atomix(
#    path = "/Users/tom/atomix",
#    version = "3.0.8-SNAPSHOT",
#)

load("//tools/build/bazel:generate_workspace.bzl", "generated_maven_jars")

generated_maven_jars()

load("//tools/build/bazel:protobuf_workspace.bzl", "generate_protobuf")

generate_protobuf()

load("//tools/build/bazel:grpc_workspace.bzl", "generate_grpc")

generate_grpc()

load("@io_grpc_grpc_java//:repositories.bzl", "grpc_java_repositories")

grpc_java_repositories(
    omit_com_google_api_grpc_google_common_protos = True,
    omit_com_google_auth_google_auth_library_credentials = True,
    omit_com_google_code_findbugs_jsr305 = True,
    omit_com_google_code_gson = True,
    omit_com_google_errorprone_error_prone_annotations = True,
    omit_com_google_guava = True,
    omit_com_google_protobuf = True,
    omit_com_google_protobuf_javalite = True,
    omit_com_google_protobuf_nano_protobuf_javanano = True,
    omit_com_google_re2j = True,
    omit_com_google_truth_truth = True,
    omit_com_squareup_okhttp = True,
    omit_com_squareup_okio = True,
    omit_io_netty_buffer = True,
    omit_io_netty_codec = True,
    omit_io_netty_codec_http = True,
    omit_io_netty_codec_http2 = True,
    omit_io_netty_codec_socks = True,
    omit_io_netty_common = True,
    omit_io_netty_handler = True,
    omit_io_netty_handler_proxy = True,
    omit_io_netty_resolver = True,
    omit_io_netty_tcnative_boringssl_static = True,
    omit_io_netty_transport = True,
    omit_io_opencensus_api = True,
    omit_io_opencensus_grpc_metrics = True,
    omit_javax_annotation = False,
    omit_junit_junit = True,
    omit_org_apache_commons_lang3 = True,
)

load("//tools/build/bazel:p4lang_workspace.bzl", "generate_p4lang")

generate_p4lang()

load("//tools/build/bazel:gnmi_workspace.bzl", "generate_gnmi")

generate_gnmi()

git_repository(
    name = "build_bazel_rules_nodejs",
    remote = "https://github.com/bazelbuild/rules_nodejs.git",
    tag = "0.11.5",  # check for the latest tag when you install
)

load("@build_bazel_rules_nodejs//:package.bzl", "rules_nodejs_dependencies")

rules_nodejs_dependencies()

load("@build_bazel_rules_nodejs//:defs.bzl", "node_repositories")

node_repositories(
    node_version = "8.11.1",
    package_json = ["//tools/gui:package.json"],
)

load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

# buildifier is written in Go and hence needs rules_go to be built.
# See https://github.com/bazelbuild/rules_go for the up to date setup instructions.
http_archive(
    name = "io_bazel_rules_go",
    url = "https://github.com/bazelbuild/rules_go/releases/download/0.14.0/rules_go-0.14.0.tar.gz",
)

http_archive(
    name = "com_github_bazelbuild_buildtools",
    strip_prefix = "buildtools-a90c3a9f00e27973d3e759d17f2e2e7d9702d91b",
    url = "https://github.com/bazelbuild/buildtools/archive/a90c3a9f00e27973d3e759d17f2e2e7d9702d91b.zip",
)

load("@io_bazel_rules_go//go:def.bzl", "go_register_toolchains", "go_rules_dependencies")
load("@com_github_bazelbuild_buildtools//buildifier:deps.bzl", "buildifier_dependencies")

go_rules_dependencies()

go_register_toolchains()

buildifier_dependencies()
