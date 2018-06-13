COMPILE_DEPS = CORE_DEPS + JACKSON + [
    "@javax_ws_rs_api//jar",
    "//utils/rest:onlab-rest",
    "//apps/mcast/api:onos-apps-mcast-api",
]

osgi_jar(
    api_description = "REST API for Multicast",
    api_package = "org.onosproject.mcast.web",
    api_title = "Multicast API",
    api_version = "1.0",
    web_context = "/onos/mcast",
    deps = COMPILE_DEPS,
)
