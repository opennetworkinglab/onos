load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

P4RUNTIME_COMMIT = "028552d98b774301c51be0fe5bc97c9e95716759"
PI_COMMIT = "36ca74fae69c8d0a142f8bfd2487bee72505cf48"

P4RUNTIME_SHA = "f335573ea971c21a1a298954039a27881b7337a03f4523321b6458eb0558644a"
PI_SHA = "767476cf9232dc39f0115d1ffc38f9b81acec74da078c048f278804f325bf77e"

def generate_p4lang():
    http_archive(
        name = "com_github_p4lang_p4runtime",
        urls = ["https://github.com/p4lang/p4runtime/archive/%s.zip" % P4RUNTIME_COMMIT],
        sha256 = P4RUNTIME_SHA,
        strip_prefix = "p4runtime-%s/proto" % P4RUNTIME_COMMIT,
        build_file = "//tools/build/bazel:p4runtime_BUILD",
    )

    # Needed for PI/proto/p4/tmp/p4config.proto
    http_archive(
        name = "com_github_p4lang_pi",
        urls = ["https://github.com/p4lang/PI/archive/%s.zip" % PI_COMMIT],
        sha256 = PI_SHA,
        strip_prefix = "PI-%s/proto" % PI_COMMIT,
        build_file = "//tools/build/bazel:pi_BUILD",
    )
