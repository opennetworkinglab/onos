COMPILE_DEPS = CORE_DEPS + JACKSON + REST + CLI + [
    "@metrics_core//jar",
    "//core/common:onos-core-common",
    "//core/net:onos-core-net",
    "//core/store/serializers:onos-core-serializers",
    "//apps/tunnel/api:onos-apps-tunnel-api",
    "//apps/virtual/api:onos-apps-virtual-api",
]

TEST_DEPS = TEST_ADAPTERS + [
    "@minimal_json//jar",
    "@jersey_client//jar",
    "//web/api:onos-rest-tests",
    "//utils/osgi:onlab-osgi-tests",
]

osgi_jar_with_tests(
    api_description = "REST API for Virtual Networks",
    api_package = "org.onosproject.incubator.net.virtual.rest",
    api_title = "Virtual Networks",
    api_version = "1.0",
    karaf_command_packages = ["org.onosproject.incubator.net.virtual.cli"],
    test_deps = TEST_DEPS,
    visibility = ["//visibility:public"],
    deps = COMPILE_DEPS,
)
