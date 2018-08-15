load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

GRPC_VER = "1.14.0"
GRPC_SHA = "d216c29f7e0eb4dce0192abbaaee9baf046a373805b6f45be3e5056ab0a616db"

GAPIS_COMMIT = "37cc0e5acae50ee91f00827a7010c3b07dfa5311"
GAPIS_SHA = "17d023f48ea290f25edaf25a967973b5a42ce6d71b1570862f302d95aa8b9f77"

def generate_grpc():
    # Patched grpc-java that fixes the OSGi split problem.
    http_archive(
        name = "io_grpc_grpc_java",
        urls = ["https://github.com/opennetworkinglab/grpc-java/archive/v%s-patched.zip" % GRPC_VER],
        sha256 = GRPC_SHA,
        strip_prefix = "grpc-java-%s-patched" % GRPC_VER,
    )

    # Google APIs protos (status.proto, etc.)
    http_archive(
        name = "com_github_googleapis",
        urls = ["https://github.com/googleapis/googleapis/archive/%s.zip" % GAPIS_COMMIT],
        sha256 = GAPIS_SHA,
        strip_prefix = "googleapis-" + GAPIS_COMMIT,
        build_file = "//tools/build/bazel:googleapis_BUILD",
    )
