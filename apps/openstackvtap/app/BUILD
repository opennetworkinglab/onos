COMPILE_DEPS = CORE_DEPS + JACKSON + KRYO + CLI + REST + [
    "//core/store/serializers:onos-core-serializers",
    "//apps/openstacknetworking/api:onos-apps-openstacknetworking-api",
    "//apps/openstacknode/api:onos-apps-openstacknode-api",
    "//apps/openstackvtap/api:onos-apps-openstackvtap-api",
]

TEST_DEPS = TEST_ADAPTERS + TEST_REST + [
    "//core/api:onos-api-tests",
    "//core/common:onos-core-common-tests",
    "//web/api:onos-rest-tests",
]

osgi_jar_with_tests(
    api_description = "Openstack Vtap REST API",
    api_package = "org.onosproject.openstackvtap.web",
    api_title = "Openstack Vtap REST API",
    api_version = "1.0",
    exclude_tests = ["org.onosproject.openstackvtap.impl.OpenstackVtapTest"],
    karaf_command_packages = ["org.onosproject.openstackvtap.cli"],
    test_deps = TEST_DEPS,
    web_context = "/onos/openstackvtap",
    deps = COMPILE_DEPS,
)
