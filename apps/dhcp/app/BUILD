COMPILE_DEPS = CORE_DEPS + JACKSON + KRYO + REST + CLI + [
    "//apps/dhcp/api:onos-apps-dhcp-api",
    "//core/store/serializers:onos-core-serializers",
]

osgi_jar_with_tests(
    api_description = "REST API for DHCP Server",
    api_package = "org.onosproject.dhcp.rest",
    api_title = "DHCP Server",
    api_version = "1.0",
    karaf_command_packages = ["org.onosproject.dhcp.cli"],
    test_deps = TEST_ADAPTERS,
    web_context = "/onos/dhcp",
    deps = COMPILE_DEPS,
)
