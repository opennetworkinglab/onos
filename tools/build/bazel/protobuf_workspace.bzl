load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

PROTOBUF_VERSION = "3.8.0"
SHA = "1e622ce4b84b88b6d2cdf1db38d1a634fe2392d74f0b7b74ff98f3a51838ee53"

def generate_protobuf():
    http_archive(
        name = "com_google_protobuf",
        urls = ["https://github.com/protocolbuffers/protobuf/archive/v%s.zip" %
                PROTOBUF_VERSION],
        sha256 = SHA,
        strip_prefix = "protobuf-" + PROTOBUF_VERSION,
    )
