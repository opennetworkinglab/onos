load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

PROTOBUF_VER = "3.6.1"
SHA = "0a4c6d0678eb2f063df332cff1a41647ef692c067b5cfb19e51bca778e79d9e0"

def generate_protobuf():
    http_archive(
        name = "com_google_protobuf",
        urls = ["https://github.com/google/protobuf/releases/download/v%s/protobuf-all-%s.zip" %
                (PROTOBUF_VER, PROTOBUF_VER)],
        sha256 = SHA,
        strip_prefix = "protobuf-" + PROTOBUF_VER,
    )
