load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

GNMI_COMMIT = "33a1865c302903e7a2e06f35960e6bc31e84b9f6"
GNMI_SHA = "cfd412410589e8e019b90681649afdb602f410a0ac67cfa1186a20d73be43e58"

def generate_gnmi():
    http_archive(
        name = "com_github_openconfig_gnmi",
        urls = ["https://github.com/openconfig/gnmi/archive/%s.zip" % GNMI_COMMIT],
        sha256 = GNMI_SHA,
        strip_prefix = "gnmi-%s/proto" % GNMI_COMMIT,
        build_file = "//tools/build/bazel:gnmi_BUILD",
    )
