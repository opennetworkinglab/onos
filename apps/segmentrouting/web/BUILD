COMPILE_DEPS = CORE_DEPS + JACKSON + [
    "@javax_ws_rs_api//jar",
    "//utils/rest:onlab-rest",
    "//apps/segmentrouting/app:onos-apps-segmentrouting-app",
]

osgi_jar_with_tests(
    api_description = "REST API for Segment Routing Application",
    api_package = "org.onosproject.segmentrouting.web",
    api_title = "Segment Routing Rest Server",
    api_version = "1.0",
    web_context = "/onos/segmentrouting",
    deps = COMPILE_DEPS,
)
