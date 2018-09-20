load(
    "//apps/openstacknode:openstack4j.bzl",
    "ALL_PACKAGES",
    "EXCLUDE_PACKAGES",
    "INCLUDE_PACKAGES",
)

COMPILE_DEPS = CORE_DEPS + [
    "@openstack4j_core//jar",
    "//apps/openstacknode/api:onos-apps-openstacknode-api",
]

osgi_jar_with_tests(
    import_packages = INCLUDE_PACKAGES + "," + EXCLUDE_PACKAGES + "," + ALL_PACKAGES,
    deps = COMPILE_DEPS,
)
