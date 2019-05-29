load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

GRPC_JAVA_VERSION = "1.21.0"
GRPC_SHA = "eaad045e911587c03cae27364920bcb0c80fb0a37bac5f54eb94af9fa0391db3"

GAPIS_COMMIT = "37cc0e5acae50ee91f00827a7010c3b07dfa5311"
GAPIS_SHA = "17d023f48ea290f25edaf25a967973b5a42ce6d71b1570862f302d95aa8b9f77"

def generate_grpc():
    # grpc-java fork that fixes the OSGi split brain problem.
    http_archive(
        name = "io_grpc_grpc_java",
        urls = ["https://github.com/opennetworkinglab/grpc-java/archive/v%s-patched.zip" % GRPC_JAVA_VERSION],
        sha256 = GRPC_SHA,
        strip_prefix = "grpc-java-%s-patched" % GRPC_JAVA_VERSION,
    )

    # Google APIs protos (status.proto, etc.)
    http_archive(
        name = "com_github_googleapis",
        urls = ["https://github.com/googleapis/googleapis/archive/%s.zip" % GAPIS_COMMIT],
        sha256 = GAPIS_SHA,
        strip_prefix = "googleapis-" + GAPIS_COMMIT,
        build_file = "//tools/build/bazel:googleapis_BUILD",
    )
