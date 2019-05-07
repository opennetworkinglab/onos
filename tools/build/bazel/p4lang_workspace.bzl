load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

P4RUNTIME_VER = "1.0.0"
P4RUNTIME_SHA = "667464bd369b40b58dc9552be2c84e190a160b6e77137b735bd86e5b81c6adc0"

def generate_p4lang():
    http_archive(
        name = "com_github_p4lang_p4runtime",
        urls = ["https://github.com/p4lang/p4runtime/archive/v%s.zip" % P4RUNTIME_VER],
        sha256 = P4RUNTIME_SHA,
        strip_prefix = "p4runtime-%s/proto" % P4RUNTIME_VER,
        build_file = "//tools/build/bazel:p4runtime_BUILD",
    )
