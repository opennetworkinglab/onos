COMPILE_DEPS = CORE_DEPS + JACKSON + REST + CLI + [
    "//core/store/serializers:onos-core-serializers",
    "//apps/imr/app:onos-apps-imr-app",
]

osgi_jar_with_tests(
    api_description = "REST API for IMR Application",
    api_package = "org.onosproject.imr.rest",
    api_title = "IMR REST API",
    api_version = "1.0",
    web_context = "/onos/v1/imr",
    deps = COMPILE_DEPS,
)
