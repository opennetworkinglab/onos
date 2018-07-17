COMPILE_DEPS = CORE_DEPS + KRYO + JACKSON + REST + CLI + [
    "@jersey_server//jar",
    "//apps/t3/app:onos-apps-t3-app",
]

osgi_jar(
    api_description = "REST API for T3",
    api_package = "org.onosproject.t3.rest",
    api_title = "T3 REST API",
    api_version = "1.0",
    web_context = "/onos/v1/t3",
    deps = COMPILE_DEPS,
)
