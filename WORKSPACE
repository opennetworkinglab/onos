workspace(
    name = "org_onosproject_onos",
    managed_directories = {
        "@gui1_npm": ["tools/gui/node_modules"],
        "@gui2_npm": ["web/gui2-fw-lib/node_modules"],
    },
)

load("//tools/build/bazel:bazel_version.bzl", "check_bazel_version")

check_bazel_version()

load("//tools/build/bazel:local_jar.bzl", "local_atomix", "local_jar", "local_yang_tools")

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

# Use this to build against locally built YANG tools
#local_yang_tools(
#    path = "/Users/andrea/onos-yang-tools",
#    version = "2.6-SNAPSHOT",
#)

load("//tools/build/bazel:generate_workspace.bzl", "generated_maven_jars")

generated_maven_jars()

load("//tools/build/bazel:protobuf_workspace.bzl", "generate_protobuf")

generate_protobuf()

load("@com_google_protobuf//:protobuf_deps.bzl", "protobuf_deps")

protobuf_deps()

load("//tools/build/bazel:grpc_workspace.bzl", "generate_grpc")

generate_grpc()

load("@io_grpc_grpc_java//:repositories.bzl", "grpc_java_repositories")

# We omit as many dependencies as we can and instead import the same via
# deps.json, so they get wrapped properly for Karaf runtime.
grpc_java_repositories(
    omit_bazel_skylib = False,
    omit_com_google_android_annotations = False,
    omit_com_google_api_grpc_google_common_protos = True,
    omit_com_google_auth_google_auth_library_credentials = True,
    omit_com_google_auth_google_auth_library_oauth2_http = True,
    omit_com_google_code_findbugs_jsr305 = True,
    omit_com_google_code_gson = True,
    omit_com_google_errorprone_error_prone_annotations = True,
    omit_com_google_guava = True,
    omit_com_google_guava_failureaccess = False,
    omit_com_google_j2objc_j2objc_annotations = True,
    omit_com_google_protobuf = True,
    omit_com_google_protobuf_javalite = True,
    omit_com_google_truth_truth = True,
    omit_com_squareup_okhttp = True,
    omit_com_squareup_okio = True,
    omit_io_grpc_grpc_proto = True,
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
    omit_io_perfmark = True,
    omit_javax_annotation = True,
    omit_junit_junit = True,
    omit_net_zlib = True,
    omit_org_apache_commons_lang3 = True,
    omit_org_codehaus_mojo_animal_sniffer_annotations = True,
)

load("//tools/build/bazel:p4lang_workspace.bzl", "generate_p4lang")

generate_p4lang()

load("//tools/build/bazel:gnmi_workspace.bzl", "generate_gnmi")

generate_gnmi()

load("//tools/build/bazel:gnoi_workspace.bzl", "generate_gnoi")

generate_gnoi()

load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

http_archive(
    name = "build_bazel_rules_nodejs",
    sha256 = "26c39450ce2d825abee5583a43733863098ed29d3cbaebf084ebaca59a21a1c8",
    urls = ["https://github.com/bazelbuild/rules_nodejs/releases/download/0.39.0/rules_nodejs-0.39.0.tar.gz"],
)

load("@build_bazel_rules_nodejs//:index.bzl", "node_repositories", "npm_install")

node_repositories(
    node_repositories = {
        "10.16.0-darwin_amd64": ("node-v10.16.0-darwin-x64.tar.gz", "node-v10.16.0-darwin-x64", "6c009df1b724026d84ae9a838c5b382662e30f6c5563a0995532f2bece39fa9c"),
        "10.16.0-linux_amd64": ("node-v10.16.0-linux-x64.tar.xz", "node-v10.16.0-linux-x64", "1827f5b99084740234de0c506f4dd2202a696ed60f76059696747c34339b9d48"),
        "10.16.0-windows_amd64": ("node-v10.16.0-win-x64.zip", "node-v10.16.0-win-x64", "aa22cb357f0fb54ccbc06b19b60e37eefea5d7dd9940912675d3ed988bf9a059"),
    },
    node_version = "10.16.0",
    package_json = ["//web/gui2-fw-lib:package.json"],
)

# The npm_install rule runs yarn anytime the package.json or package-lock.json file changes.
# It also extracts any Bazel rules distributed in an npm package.
load("@build_bazel_rules_nodejs//:index.bzl", "npm_install")

npm_install(
    # Name this npm so that Bazel Label references look like @npm//package
    name = "gui1_npm",
    package_json = "//tools/gui:package.json",
    package_lock_json = "//tools/gui:package-lock.json",
)

npm_install(
    # Name this npm so that Bazel Label references look like @npm//package
    name = "gui2_npm",
    package_json = "//web/gui2-fw-lib:package.json",
    package_lock_json = "//web/gui2-fw-lib:package-lock.json",
)

# Install any Bazel rules which were extracted earlier by the npm_install rule.
load("@gui2_npm//:install_bazel_dependencies.bzl", "install_bazel_dependencies")

install_bazel_dependencies()

# buildifier is written in Go and hence needs rules_go to be built.
# See https://github.com/bazelbuild/rules_go for the up to date setup instructions.
http_archive(
    name = "io_bazel_rules_go",
    sha256 = "9fb16af4d4836c8222142e54c9efa0bb5fc562ffc893ce2abeac3e25daead144",
    urls = [
        "https://storage.googleapis.com/bazel-mirror/github.com/bazelbuild/rules_go/releases/download/0.19.0/rules_go-0.19.0.tar.gz",
        "https://github.com/bazelbuild/rules_go/releases/download/0.19.0/rules_go-0.19.0.tar.gz",
    ],
)

load("@io_bazel_rules_go//go:deps.bzl", "go_register_toolchains", "go_rules_dependencies")

go_rules_dependencies()

go_register_toolchains()

http_archive(
    name = "bazel_gazelle",
    sha256 = "be9296bfd64882e3c08e3283c58fcb461fa6dd3c171764fcc4cf322f60615a9b",
    urls = [
        "https://storage.googleapis.com/bazel-mirror/github.com/bazelbuild/bazel-gazelle/releases/download/0.18.1/bazel-gazelle-0.18.1.tar.gz",
        "https://github.com/bazelbuild/bazel-gazelle/releases/download/0.18.1/bazel-gazelle-0.18.1.tar.gz",
    ],
)

load("@bazel_gazelle//:deps.bzl", "gazelle_dependencies")

gazelle_dependencies()

http_archive(
    name = "com_github_bazelbuild_buildtools",
    sha256 = "05eb52437fb250c7591dd6cbcfd1f9b5b61d85d6b20f04b041e0830dd1ab39b3",
    strip_prefix = "buildtools-0.29.0",
    url = "https://github.com/bazelbuild/buildtools/archive/0.29.0.zip",
)
