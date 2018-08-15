COMPILE_DEPS = CORE_DEPS + [
    "@jersey_client//jar",
    "@jersey_server//jar",
    "@jersey_common//jar",
    "@jersey_media_sse//jar",
    "@jersey_security//jar",
    "@httpclient_osgi//jar",
    "@httpcore_osgi//jar",
    "@javax_ws_rs_api//jar",
    "@hk2_api//jar",
    "@aopalliance_repackaged//jar",
    "@javax_inject//jar",
    "//protocols/rest/api:onos-protocols-rest-api",
]

TEST_DEPS = TEST_REST + [
    "//core/common:onos-core-common-tests",
]

osgi_jar_with_tests(
    test_deps = TEST_DEPS,
    deps = COMPILE_DEPS,
)
