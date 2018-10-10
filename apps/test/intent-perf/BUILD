COMPILE_DEPS = CORE_DEPS + JACKSON + CLI + REST + [
    "//core/store/serializers:onos-core-serializers",
]

osgi_jar_with_tests(
    karaf_command_packages = ["org.onosproject.intentperf"],
    deps = COMPILE_DEPS,
)

onos_app(
    app_name = "org.onosproject.intentperf",
    category = "Test Utility",
    description = "Intent performance test application.",
    title = "Intent Performance Test",
    url = "http://onosproject.org",
)
