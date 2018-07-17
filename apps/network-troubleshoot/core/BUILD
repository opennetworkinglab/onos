COMPILE_DEPS = CORE_DEPS + [
    "//incubator/api:onos-incubator-api",
    "//apps/network-troubleshoot/api:onos-apps-network-troubleshoot-api",
]

TEST_DEPS = TEST + [
    "//utils/osgi:onlab-osgi-tests",
    "//incubator/api:onos-incubator-api-tests",
]

osgi_jar_with_tests(
    test_deps = TEST_DEPS,
    deps = COMPILE_DEPS,
)
