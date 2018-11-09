COMPILE_DEPS = CORE_DEPS + JACKSON + KRYO + [
    "@javax_ws_rs_api//jar",
    "//apps/faultmanagement/fmmgr:onos-apps-faultmanagement-fmmgr",
    "//utils/rest:onlab-rest",
]

TEST_DEPS = TEST_ADAPTERS + [
    "@jersey_test_framework_core//jar",
    "@jersey_test_framework_jetty//jar",
    "//utils/osgi:onlab-osgi-tests",
    "//web/api:onos-rest-tests",
]

osgi_jar_with_tests(
    api_description = "REST API for Fault Management",
    api_package = "org.onosproject.faultmanagement",
    api_title = "Fault Management Rest API",
    api_version = "/onos/v1/fm",
    test_deps = TEST_DEPS,
    web_context = "/onos/dhcp",
    deps = COMPILE_DEPS,
)
