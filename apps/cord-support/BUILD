COMPILE_DEPS = CORE_DEPS + JACKSON + REST + [
    "//core/store/serializers:onos-core-serializers",
]

TEST_DEPS = TEST_REST + [
    "//deps:jersey-server",
]

osgi_jar_with_tests(
    api_description = "REST API for CORD inquiry to ONOS topology",
    api_package = "org.onosproject.cordsupport",
    api_title = "CORD Support",
    api_version = "1.0",
    test_deps = TEST_DEPS,
    web_context = "/onos/cord-support",
    deps = COMPILE_DEPS,
)

onos_app(
    category = "Integration",
    description = "CORD Support application.",
    title = "CORD Support",
    url = "http://onosproject.org",
)
