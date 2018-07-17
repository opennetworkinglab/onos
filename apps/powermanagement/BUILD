COMPILE_DEPS = CORE_DEPS + JACKSON + REST + [
    "//core/store/serializers:onos-core-serializers",
]

osgi_jar_with_tests(
    api_description = "REST API for Power Management",
    api_package = "org.onosproject.powermanagement",
    api_title = "Power Management API",
    api_version = "1.0",
    test_deps = TEST_REST,
    web_context = "/onos/powermanagement",
    deps = COMPILE_DEPS,
)

onos_app(
    category = "Monitoring",
    description = "This application provides northbound interfaces for monitoring and " +
                  "configuring power.",
    title = "Power Management",
    url = "http://onosproject.org",
)
