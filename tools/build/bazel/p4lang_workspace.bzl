load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

P4RUNTIME_COMMIT = "a6f81ac53c6b56d75a9603690794196d67c5dc07"
PI_COMMIT = "539e4624f16aac39f8890a6dfb11c65040e735ad"

P4RUNTIME_SHA = "28b79868bcfd61058cdd3f77a7a021a1add19154fa6717bf921a64cece32caf3"
PI_SHA = "a16024972c15e6d35466996bbb748e4b7bef819c1c93f05a0f2228062736c35a"

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
