COMPILE_DEPS = CORE_DEPS + CLI + REST + [
    "//core/store/serializers:onos-core-serializers",
]

osgi_jar_with_tests(
    karaf_command_packages = ["org.onosproject.primitiveperf"],
    deps = COMPILE_DEPS,
)

onos_app(
    app_name = "org.onosproject.primitiveperf",
    category = "Test Utility",
    description = "Primitive performance test application.",
    title = "Primitive Performance Test",
    url = "http://onosproject.org",
)
