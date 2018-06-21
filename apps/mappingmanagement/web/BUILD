COMPILE_DEPS = CORE_DEPS + JACKSON + [
    "@javax_ws_rs_api//jar",
    "//utils/rest:onlab-rest",
    "//apps/mappingmanagement/api:onos-apps-mappingmanagement-api",
]

TEST_DEPS = TEST_REST + [
    "@minimal_json//jar",
    "//web/api:onos-rest-tests",
]

osgi_jar_with_tests(
    api_description = "REST API for ONOS Mapping Management",
    api_package = "org.onosproject.mapping.web.api",
    api_title = "Mapping Management API",
    api_version = "1.0",
    test_deps = TEST_DEPS,
    web_context = "/onos/mappingmanagement",
    deps = COMPILE_DEPS,
)
