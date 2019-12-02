load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

TOPO_COMMIT = "8866b0a658247683bd4b852839ce91c6ba60f6ac"
TOPO_SHA = "dc63356c3d34de18b0afdf04cce01f6de83e2b7264de177e55c0595b05dbcd07"

def generate_topo_device():
    http_archive(
        name = "com_github_onosproject_onos_topo",
        urls = ["https://github.com/onosproject/onos-topo/archive/%s.zip" % TOPO_COMMIT],
        sha256 = TOPO_SHA,
        strip_prefix = "onos-topo-%s/api" % TOPO_COMMIT,
        build_file = "//tools/build/bazel:topo_BUILD",
    )
