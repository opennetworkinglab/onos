COMPILE_DEPS = CORE_DEPS + JACKSON + KRYO + [
    "@javax_ws_rs_api//jar",
    "@org_apache_karaf_shell_console//jar",
    "//apps/dhcp/api:onos-apps-dhcp-api",
    "//utils/rest:onlab-rest",
    "//core/store/serializers:onos-core-serializers",
    "//cli:onos-cli",
]

osgi_jar_with_tests(
    api_description = "REST API for DHCP Server",
    api_package = "org.onosproject.dhcp.rest",
    api_title = "DHCP Server",
    api_version = "1.0",
    test_deps = TEST_ADAPTERS,
    web_context = "/onos/dhcp",
    deps = COMPILE_DEPS,
)
