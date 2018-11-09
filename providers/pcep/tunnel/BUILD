COMPILE_DEPS = CORE_DEPS + NETTY + [
    "//protocols/ovsdb/api:onos-protocols-ovsdb-api",
    "//protocols/ovsdb/rfc:onos-protocols-ovsdb-rfc",
    "//apps/pcep-api:onos-apps-pcep-api",
    "//apps/tunnel/api:onos-apps-tunnel-api",
    "//protocols/pcep/pcepio:onos-protocols-pcep-pcepio",
    "//protocols/pcep/server/api:onos-protocols-pcep-server-api",
    "//protocols/pcep/server/ctl:onos-protocols-pcep-server-ctl",
]

TEST_DEPS = TEST_ADAPTERS + [
    "@io_netty_netty//jar",
    "@io_netty_netty_transport//jar",
    "//protocols/pcep/server/api:onos-protocols-pcep-server-api-tests",
    "//apps/pcep-api:onos-apps-pcep-api-tests",
    "//apps/tunnel/api:onos-apps-tunnel-api-tests",
]

osgi_jar_with_tests(
    test_deps = TEST_DEPS,
    deps = COMPILE_DEPS,
)
