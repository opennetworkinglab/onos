load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

GNOI_COMMIT = "d703187b4d437508375f55c4e4f44268ccce412f"
GNOI_SHA = "7c34f6efb48d4efd145059a06702e391840591cdb4668267f9089232de4f9617"

def generate_gnoi():
    http_archive(
        name = "com_github_openconfig_gnoi",
        urls = ["https://github.com/openconfig/gnoi/archive/%s.zip" % GNOI_COMMIT],
        sha256 = GNOI_SHA,
        strip_prefix = "gnoi-%s" % GNOI_COMMIT,
        build_file = "//tools/build/bazel:gnoi_BUILD",
    )
