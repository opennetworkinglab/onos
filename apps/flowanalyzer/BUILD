COMPILE_DEPS = CORE_DEPS + CLI

osgi_jar_with_tests(
    karaf_command_packages = ["org.onosproject.flowanalyzer"],
    resources = glob(["src/main/resources/**"]),
    resources_root = "src/main/resources",
    test_deps = TEST_ADAPTERS,
    deps = COMPILE_DEPS,
)

onos_app(
    category = "Monitoring",
    description = "Simple flow space analyzer.",
    title = "Flow Space Analysis",
    url = "http://onosproject.org",
)
