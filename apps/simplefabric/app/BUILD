COMPILE_DEPS = CORE_DEPS + JACKSON + CLI + REST + [
    "//apps/simplefabric/api:onos-apps-simplefabric-api",
    "@concurrent_trees//jar",
]

osgi_jar_with_tests(
    api_description = "REST API for Simple Fabric",
    api_package = "org.onosproject.simplefabric.web",
    api_title = "Simple Fabric API",
    web_context = "/onos/v1/simplefabric",
    deps = COMPILE_DEPS,
)
