COMPILE_DEPS = CORE_DEPS + CLI + REST + [
    "//core/store/serializers:onos-core-serializers",
]

osgi_jar_with_tests(
    karaf_command_packages = ["org.onosproject.proxytest"],
    deps = COMPILE_DEPS,
)

onos_app(
    app_name = "org.onosproject.proxytest",
    category = "Test Utility",
    description = "Proxy test application.",
    title = "Proxy Test",
    url = "http://onosproject.org",
)
