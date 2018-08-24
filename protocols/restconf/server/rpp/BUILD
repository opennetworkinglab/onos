COMPILE_DEPS = CORE_DEPS + JACKSON + [
    "@jersey_server//jar",
    "@javax_ws_rs_api//jar",
    "@servlet_api//jar",
    "@javax_inject//jar",
    "//utils/rest:onlab-rest",
    "//apps/restconf/api:onos-apps-restconf-api",
]

TEST_DEPS = TEST_REST + [
    "//utils/osgi:onlab-osgi-tests",
    "//web/api:onos-rest-tests",
]

osgi_jar_with_tests(
    exclude_tests = ["org.onosproject.protocol.restconf.server.rpp.RestconfWebResourceTest"],
    test_deps = TEST_DEPS,
    web_context = "/onos/restconf",
    deps = COMPILE_DEPS,
)
