load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

P4RUNTIME_VER = "1.2.0"
P4RUNTIME_SHA = "0fce7e06c63e60a8cddfe56f3db3d341953560c054d4c09ffda0e84476124f5a"

def generate_p4lang():
    http_archive(
        name = "com_github_p4lang_p4runtime",
        urls = ["https://github.com/p4lang/p4runtime/archive/v%s.zip" % P4RUNTIME_VER],
        sha256 = P4RUNTIME_SHA,
        strip_prefix = "p4runtime-%s/proto" % P4RUNTIME_VER,
        build_file = "//tools/build/bazel:p4runtime_BUILD",
    )
