load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

PROTOBUF_VERSION = "3.6.1.3"
SHA = "9510dd2afc29e7245e9e884336f848c8a6600a14ae726adb6befdb4f786f0be2"

def generate_protobuf():
    http_archive(
        name = "com_google_protobuf",
        urls = ["https://github.com/protocolbuffers/protobuf/archive/v%s.zip" %
                PROTOBUF_VERSION],
        sha256 = SHA,
        strip_prefix = "protobuf-" + PROTOBUF_VERSION,
    )
