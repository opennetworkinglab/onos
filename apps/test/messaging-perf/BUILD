COMPILE_DEPS = CORE_DEPS + KRYO + CLI + REST + [
    "//core/store/serializers:onos-core-serializers",
]

osgi_jar_with_tests(
    deps = COMPILE_DEPS,
)

onos_app(
    category = "Test Utility",
    description = "Messaging performance test application.",
    title = "Messaging Performance Test",
    url = "http://onosproject.org",
)
