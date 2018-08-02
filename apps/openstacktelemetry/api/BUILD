COMPILE_DEPS = CORE_DEPS + [
    "@kafka_clients//jar",
    "@jersey_client//jar",
    "@javax_ws_rs_api//jar",
    "@influxdb_java//jar",
]

TEST_DEPS = TEST_ADAPTERS + [
    "//core/api:onos-api-tests",
    "//core/common:onos-core-common-tests",
]

osgi_jar_with_tests(
    test_deps = TEST_DEPS,
    deps = COMPILE_DEPS,
)
