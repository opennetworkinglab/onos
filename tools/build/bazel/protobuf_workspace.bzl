load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

PROTOBUF_VERSION = "3.10.0"
SHA = "33cba8b89be6c81b1461f1c438424f7a1aa4e31998dbe9ed6f8319583daac8c7"

def generate_protobuf():
    http_archive(
        name = "com_google_protobuf",
        urls = ["https://github.com/protocolbuffers/protobuf/archive/v%s.zip" %
                PROTOBUF_VERSION],
        sha256 = SHA,
        strip_prefix = "protobuf-" + PROTOBUF_VERSION,
    )
