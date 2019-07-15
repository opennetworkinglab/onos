load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

PROTOBUF_VERSION = "3.9.0"
SHA = "8eb5ca331ab8ca0da2baea7fc0607d86c46c80845deca57109a5d637ccb93bb4"

def generate_protobuf():
    http_archive(
        name = "com_google_protobuf",
        urls = ["https://github.com/protocolbuffers/protobuf/archive/v%s.zip" %
                PROTOBUF_VERSION],
        sha256 = SHA,
        strip_prefix = "protobuf-" + PROTOBUF_VERSION,
    )
