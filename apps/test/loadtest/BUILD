COMPILE_DEPS = CORE_DEPS + CLI + REST + [
    "//core/store/serializers:onos-core-serializers",
]

osgi_jar_with_tests(
    deps = COMPILE_DEPS,
)

onos_app(
    category = "Test Utility",
    description = "Distributed consensus load test application.",
    title = "Distributed Load Test",
    url = "http://onosproject.org",
)
