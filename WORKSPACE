workspace(
    name = "org_onosproject_onos",
    managed_directories = {
        "@gui1_npm": ["tools/gui/node_modules"],
        "@npm": ["web/gui2/node_modules"],
    },
)

load("//tools/build/bazel:bazel_version.bzl", "check_bazel_version")

check_bazel_version()

load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

# It is necessary to explicitly load this version of bazel-skylib for the
# GUI build with native bazel e.g. ts_web_test_suite or ts_library. If not specified
# here an older version is pulled in by something else. It may be possible to update
# this once other tools are updated
BAZEL_SKYLIB_VERSION = "1.0.2"

BAZEL_SKYLIB_SHA256 = "97e70364e9249702246c0e9444bccdc4b847bed1eb03c5a3ece4f83dfe6abc44"

http_archive(
    name = "bazel_skylib",
    sha256 = BAZEL_SKYLIB_SHA256,
    urls = [
        "https://github.com/bazelbuild/bazel-skylib/releases/download/%s/bazel-skylib-%s.tar.gz" % (BAZEL_SKYLIB_VERSION, BAZEL_SKYLIB_VERSION),
    ],
)

load("@bazel_skylib//:workspace.bzl", "bazel_skylib_workspace")

bazel_skylib_workspace()

load("//tools/build/bazel:local_jar.bzl", "local_atomix", "local_jar", "local_yang_tools")

# Use this to build against locally built arbitrary 3rd party artifacts
#local_jar(
#    name = "atomix",
#    path = "/Users/tom/atomix/core/target/atomix-3.0.8-SNAPSHOT.jar",
#)

# Use this to build against locally built Atomix
#local_atomix(
#    path = "/home/sdn/atomix",
#    version = "3.1.12-SNAPSHOT",
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
    omit_com_google_android_annotations = True,
    omit_com_google_api_grpc_google_common_protos = True,
    omit_com_google_auth_google_auth_library_credentials = True,
    omit_com_google_auth_google_auth_library_oauth2_http = True,
    omit_com_google_code_findbugs_jsr305 = True,
    omit_com_google_code_gson = True,
    omit_com_google_errorprone_error_prone_annotations = True,
    omit_com_google_guava = True,
    omit_com_google_guava_failureaccess = True,
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

# For GUI2 build
RULES_NODEJS_VERSION = "2.3.2"

RULES_NODEJS_SHA256 = "b3521b29c7cb0c47a1a735cce7e7e811a4f80d8e3720cf3a1b624533e4bb7cb6"

load("//tools/build/bazel:topo_workspace.bzl", "generate_topo_device")

generate_topo_device()

http_archive(
    name = "build_bazel_rules_nodejs",
    sha256 = RULES_NODEJS_SHA256,
    urls = [
        "https://github.com/bazelbuild/rules_nodejs/releases/download/%s/rules_nodejs-%s.tar.gz" % (RULES_NODEJS_VERSION, RULES_NODEJS_VERSION),
    ],
)

# Rules for compiling sass
RULES_SASS_VERSION = "1.25.0"

RULES_SASS_SHA256 = "c78be58f5e0a29a04686b628cf54faaee0094322ae0ac99da5a8a8afca59a647"

http_archive(
    name = "io_bazel_rules_sass",
    sha256 = RULES_SASS_SHA256,
    strip_prefix = "rules_sass-%s" % RULES_SASS_VERSION,
    urls = [
        "https://github.com/bazelbuild/rules_sass/archive/%s.zip" % RULES_SASS_VERSION,
        "https://mirror.bazel.build/github.com/bazelbuild/rules_sass/archive/%s.zip" % RULES_SASS_VERSION,
    ],
)

load("@build_bazel_rules_nodejs//:index.bzl", "node_repositories", "npm_install", "yarn_install")

# Setup the Node repositories. We need a NodeJS version that is more recent than v10.15.0
# because "selenium-webdriver" which is required for "ng e2e" cannot be installed.
node_repositories(
    node_repositories = {
        "10.16.0-linux_arm64": ("node-v10.16.0-linux-arm64.tar.gz", "node-v10.16.0-linux-arm64", "2d84a777318bc95dd2a201ab8d700aea7e20641b3ece0c048399398dc645cbd7"),
        "10.16.0-darwin_amd64": ("node-v10.16.0-darwin-x64.tar.gz", "node-v10.16.0-darwin-x64", "6c009df1b724026d84ae9a838c5b382662e30f6c5563a0995532f2bece39fa9c"),
        "10.16.0-linux_amd64": ("node-v10.16.0-linux-x64.tar.xz", "node-v10.16.0-linux-x64", "1827f5b99084740234de0c506f4dd2202a696ed60f76059696747c34339b9d48"),
        "10.16.0-windows_amd64": ("node-v10.16.0-win-x64.zip", "node-v10.16.0-win-x64", "aa22cb357f0fb54ccbc06b19b60e37eefea5d7dd9940912675d3ed988bf9a059"),
    },
    node_version = "10.16.0",
)

# TODO give this a name like `gui2_npm` once the @bazel/karma tools can tolerate a name other than `npm`
yarn_install(
    name = "npm",
    package_json = "//web/gui2:package.json",
    use_global_yarn_cache = True,
    yarn_lock = "//web/gui2:yarn.lock",
)

npm_install(
    # Name this npm so that Bazel Label references look like @npm//package
    name = "gui1_npm",
    package_json = "//tools/gui:package.json",
    package_lock_json = "//tools/gui:package-lock.json",
)

# Install any Bazel rules which were extracted earlier by the npm_install rule.
# Versions are set in web/gui2-fw-lib/package.json

RULES_WEBTESTING_VERSION = "0.3.3"

RULES_WEBTESTING_SHA256 = "9bb461d5ef08e850025480bab185fd269242d4e533bca75bfb748001ceb343c3"

http_archive(
    name = "io_bazel_rules_webtesting",
    sha256 = RULES_WEBTESTING_SHA256,
    urls = [
        "https://github.com/bazelbuild/rules_webtesting/releases/download/%s/rules_webtesting.tar.gz" % RULES_WEBTESTING_VERSION,
    ],
)

load("//tools/build/bazel:angular_workspace.bzl", "load_angular")

load_angular()

# buildifier is written in Go and hence needs rules_go to be built.
# See https://github.com/bazelbuild/rules_go for the up to date setup instructions.
RULES_GO_VERSION = "v0.19.8"

RULES_GO_SHA256 = "9976c2572587aa71f81b502cc870ef8058f6de37f5fcfaade6a5996934b4a324"

http_archive(
    name = "io_bazel_rules_go",
    sha256 = RULES_GO_SHA256,
    urls = [
        "https://storage.googleapis.com/bazel-mirror/github.com/bazelbuild/rules_go/releases/download/%s/rules_go-%s.tar.gz" % (RULES_GO_VERSION, RULES_GO_VERSION),
        "https://github.com/bazelbuild/rules_go/releases/download/%s/rules_go-%s.tar.gz" % (RULES_GO_VERSION, RULES_GO_VERSION),
    ],
)

load("@io_bazel_rules_go//go:deps.bzl", "go_register_toolchains", "go_rules_dependencies")

go_rules_dependencies()

go_register_toolchains()

GAZELLE_VERSION = "0.18.1"

GAZELLE_SHA256 = "be9296bfd64882e3c08e3283c58fcb461fa6dd3c171764fcc4cf322f60615a9b"

http_archive(
    name = "bazel_gazelle",
    sha256 = GAZELLE_SHA256,
    urls = [
        "https://storage.googleapis.com/bazel-mirror/github.com/bazelbuild/bazel-gazelle/releases/download/%s/bazel-gazelle-%s.tar.gz" % (GAZELLE_VERSION, GAZELLE_VERSION),
        "https://github.com/bazelbuild/bazel-gazelle/releases/download/%s/bazel-gazelle-%s.tar.gz" % (GAZELLE_VERSION, GAZELLE_VERSION),
    ],
)

load("@bazel_gazelle//:deps.bzl", "gazelle_dependencies")

gazelle_dependencies()

BUILDTOOLS_VERSION = "0.29.0"

BUILDTOOLS_SHA256 = "05eb52437fb250c7591dd6cbcfd1f9b5b61d85d6b20f04b041e0830dd1ab39b3"

http_archive(
    name = "com_github_bazelbuild_buildtools",
    sha256 = BUILDTOOLS_SHA256,
    strip_prefix = "buildtools-" + BUILDTOOLS_VERSION,
    url = "https://github.com/bazelbuild/buildtools/archive/%s.zip" % BUILDTOOLS_VERSION,
)
