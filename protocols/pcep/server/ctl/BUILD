COMPILE_DEPS = CORE_DEPS + NETTY + JACKSON + KRYO + [
    "@netty//jar",
    "//incubator/api:onos-incubator-api",
    "//protocols/pcep/pcepio:onos-protocols-pcep-pcepio",
    "//protocols/pcep/server/api:onos-protocols-pcep-server-api",
    "//core/store/serializers:onos-core-serializers",
    "//apps/pcep-api:onos-apps-pcep-api",
]

TEST_DEPS = TEST_ADAPTERS + [
    "//protocols/pcep/server/api:onos-protocols-pcep-server-api-tests",
    "//incubator/api:onos-incubator-api-tests",
]

osgi_jar_with_tests(
    test_deps = TEST_DEPS,
    deps = COMPILE_DEPS,
)
