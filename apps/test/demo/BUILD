COMPILE_DEPS = CORE_DEPS + REST + JACKSON

osgi_jar_with_tests(
    api_description = "REST API for Demo",
    api_package = "org.onosproject.demo",
    api_title = "Flow Throughput Demo",
    api_version = "1.0",
    web_context = "/onos/demo",
    deps = COMPILE_DEPS,
)

onos_app(
    category = "Test Utility",
    description = "APIs for interacting with the flow throughput test application.",
    title = "Flow Throughput Demo",
    url = "http://onosproject.org",
)
