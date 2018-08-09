load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

GRPC_VER = "1.14.0"
SHA = "657ee70cbbc7e8c5aa26d622329f5fc8bfa6ce5e960bcdbff802f785b0eba212"

def generate_grpc():
    http_archive(
        name = "io_grpc_grpc_java",
        urls = ["https://github.com/grpc/grpc-java/archive/v%s.zip" % GRPC_VER],
        sha256 = SHA,
        strip_prefix = "grpc-java-" + GRPC_VER,
    )
    http_archive(
        name = "io_grpc_grpc_java_core_repkg",
        urls = ["https://github.com/grpc/grpc-java/archive/v%s.zip" % GRPC_VER],
        sha256 = SHA,
        strip_prefix = "grpc-java-%s/core" % GRPC_VER,
        build_file = "//tools/build/bazel:grpc_core_repkg_BUILD",
    )
