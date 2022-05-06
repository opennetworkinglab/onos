load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

GRPC_JAVA_VERSION = "1.22.1"
GRPC_SHA = "073395219d611c21a460d21dfe5cb77550030d6396b400f796e568b5175c2ec0"

GAPIS_COMMIT = "37cc0e5acae50ee91f00827a7010c3b07dfa5311"
GAPIS_SHA = "17d023f48ea290f25edaf25a967973b5a42ce6d71b1570862f302d95aa8b9f77"

def generate_grpc():
    # grpc-java fork that fixes the OSGi split brain problem.
    http_archive(
        name = "io_grpc_grpc_java",
        urls = ["https://github.com/opennetworkinglab/grpc-java/archive/v%s-fix-host_javabase.zip" % GRPC_JAVA_VERSION],
        sha256 = GRPC_SHA,
        strip_prefix = "grpc-java-%s-fix-host_javabase" % GRPC_JAVA_VERSION,
    )

    # Google APIs protos (status.proto, etc.)
    http_archive(
        name = "com_github_googleapis",
        urls = ["https://github.com/googleapis/googleapis/archive/%s.zip" % GAPIS_COMMIT],
        sha256 = GAPIS_SHA,
        strip_prefix = "googleapis-" + GAPIS_COMMIT,
        build_file = "//tools/build/bazel:googleapis_BUILD",
    )
