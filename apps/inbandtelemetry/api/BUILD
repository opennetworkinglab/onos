COMPILE_DEPS = CORE_DEPS + JACKSON + REST + KRYO + [
    "@jersey_client//jar",
    "//core/store/serializers:onos-core-serializers",
]

osgi_jar_with_tests(
    api_description = "REST API for Int",
    api_package = "org.onosproject.inbandtelemetry.rest",
    api_title = "INT REST",
    api_version = "1.0",
    test_deps = TEST_ADAPTERS,
    web_context = "/onos/int",
    deps = COMPILE_DEPS,
)
