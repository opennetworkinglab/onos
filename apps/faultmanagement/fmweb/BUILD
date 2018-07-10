COMPILE_DEPS = CORE_DEPS + JACKSON + KRYO + [
    "@javax_ws_rs_api//jar",
    "//incubator/api:onos-incubator-api",
    "//apps/faultmanagement/fmmgr:onos-apps-faultmanagement-fmmgr",
    "//utils/rest:onlab-rest",
]

osgi_jar_with_tests(
    api_description = "REST API for Fault Management",
    api_package = "org.onosproject.faultmanagement",
    api_title = "Fault Management Rest API",
    api_version = "/onos/v1/fm",
    test_deps = TEST_ADAPTERS,
    web_context = "/onos/dhcp",
    deps = COMPILE_DEPS,
)
