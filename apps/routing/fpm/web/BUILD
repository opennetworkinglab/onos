COMPILE_DEPS = CORE_DEPS + JACKSON + REST + [
    "@jersey_server//jar",
    "//apps/routing/fpm/app:onos-apps-routing-fpm-app",
    "//apps/routing/fpm/api:onos-apps-routing-fpm-api",
]

osgi_jar(
    api_description = "REST API for FPM",
    api_package = "org.onosproject.fpm.web",
    api_title = "FPM API",
    api_version = "1.0",
    web_context = "/onos/v1/fpm",
    deps = COMPILE_DEPS,
)
