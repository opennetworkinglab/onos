COMPILE_DEPS = CORE_DEPS + NETTY + JACKSON + [
    "@netty//jar",
    "//incubator/api:onos-incubator-api",
    "//protocols/ovsdb/api:onos-protocols-ovsdb-api",
    "//protocols/ovsdb/rfc:onos-protocols-ovsdb-rfc",
    "//apps/pcep-api:onos-apps-pcep-api",
    "//protocols/pcep/server/api:onos-protocols-pcep-server-api",
    "//protocols/pcep/pcepio:onos-protocols-pcep-pcepio",
    "//core/api:onos-api-tests",
    "//apps/optical-model:onos-apps-optical-model",
]

TEST_DEPS = TEST + [
    "//protocols/pcep/server/api:onos-protocols-pcep-server-api-tests",
    "//apps/pcep-api:onos-apps-pcep-api-tests",
]

osgi_jar_with_tests(
    test_deps = TEST_DEPS,
    deps = COMPILE_DEPS,
)
