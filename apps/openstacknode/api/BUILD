load(
    "//apps/openstacknode:openstack4j.bzl",
    "ALL_PACKAGES",
    "EXCLUDE_PACKAGES",
    "INCLUDE_PACKAGES",
)

COMPILE_DEPS = CORE_DEPS + CLI + [
    "//core/store/serializers:onos-core-serializers",
    "//protocols/ovsdb/api:onos-protocols-ovsdb-api",
    "//protocols/ovsdb/rfc:onos-protocols-ovsdb-rfc",
]

TEST_DEPS = TEST_ADAPTERS + [
    "//core/api:onos-api-tests",
    "//core/common:onos-core-common-tests",
]

osgi_jar_with_tests(
    exclude_tests = ["org.onosproject.openstacknode.api.OpenstackNodeTest"],
    import_packages = INCLUDE_PACKAGES + "," + EXCLUDE_PACKAGES + "," + ALL_PACKAGES,
    test_deps = TEST_ADAPTERS,
    deps = COMPILE_DEPS,
)
