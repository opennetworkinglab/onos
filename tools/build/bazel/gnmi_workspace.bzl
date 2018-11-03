load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

# FIXME: Currently gNMI proto file uses incorrect path to import "gnmi_ext.proto"
#        Temporary use patch from ONF before gNMI team fix it.

GNMI_COMMIT = "onos"
GNMI_SHA = "0c4d5f168cb142f8135171204dac3ff8840a147f51fa361079f42fa585bec2ce"

def generate_gnmi():
    http_archive(
        name = "com_github_openconfig_gnmi",
        urls = ["https://github.com/opennetworkinglab/gnmi/archive/%s.zip" % GNMI_COMMIT],
        sha256 = GNMI_SHA,
        strip_prefix = "gnmi-%s/proto" % GNMI_COMMIT,
        build_file = "//tools/build/bazel:gnmi_BUILD",
    )
