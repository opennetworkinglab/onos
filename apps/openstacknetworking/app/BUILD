load(
    "//apps/openstacknode:openstack4j.bzl",
    "ALL_PACKAGES",
    "EXCLUDE_PACKAGES",
    "INCLUDE_PACKAGES",
    "get_jackson_deps_path",
    "get_openstack4j_deps_path",
)

COMPILE_DEPS = CORE_DEPS + JACKSON + KRYO + CLI + REST + [
    "//core/store/serializers:onos-core-serializers",
    "//protocols/ovsdb/api:onos-protocols-ovsdb-api",
    "//protocols/ovsdb/rfc:onos-protocols-ovsdb-rfc",
    "//apps/openstacknode/api:onos-apps-openstacknode-api",
    "//apps/openstacknetworking/api:onos-apps-openstacknetworking-api",
    "@jersey_client//jar",
    "@httpclient_osgi//jar",
    "@httpcore_osgi//jar",
    "@commons_net//jar",
    "@commons_codec//jar",
    "@openstack4j_core//jar",
    "@openstack4j_http_connector//jar",
    "@openstack4j_httpclient//jar",
    "@json_patch//jar",
    "@jackson_coreutils//jar",
    "@btf//jar",
    "@msg_simple//jar",
    "@snakeyaml//jar",
    "@sshd_core//jar",
]

EXCLUDED_BUNDLES = [
    "@sshd_core//jar",
]

TEST_DEPS = TEST_ADAPTERS + TEST_REST + [
    "//core/api:onos-api-tests",
    "//apps/openstacknode/api:onos-apps-openstacknode-api-tests",
    "//core/common:onos-core-common-tests",
    "//web/api:onos-rest-tests",
]

osgi_jar_with_tests(
    api_description = "REST API for OpenStack Networking",
    api_package = "org.onosproject.openstacknetworking.web",
    api_title = "OpenStack Networking API",
    api_version = "1.0",
    bundle_classpath = get_openstack4j_deps_path() + get_jackson_deps_path(),
    import_packages = INCLUDE_PACKAGES + "," + EXCLUDE_PACKAGES + "," + ALL_PACKAGES,
    karaf_command_packages = ["org.onosproject.openstacknetworking.cli"],
    medium_tests = ["src/test/java/org/onosproject/openstacknetworking/util/OpenstackNetworkingUtilTest"],
    test_deps = TEST_DEPS,
    web_context = "/onos/openstacknetworking",
    deps = COMPILE_DEPS,
)
