COMPILE_DEPS = CORE_DEPS + JACKSON + METRICS + [
    "@javax_ws_rs_api//jar",
    "//utils/rest:onlab-rest",
]

TEST_DEPS = TEST_REST + [
    "@minimal_json//jar",
    "@jersey_security//jar",
]

osgi_jar_with_tests(
    name = "onos-rest",
    api_description = "ONOS Core REST API",
    api_package = "org.onosproject.rest.impl",
    api_title = "ONOS Core REST API",
    api_version = "1.0",
    exclude_tests = ["org.onosproject.rest.resources.ResourceTest"],
    test_deps = TEST_DEPS,
    web_context = "/onos/v1",
    deps = COMPILE_DEPS,
)
