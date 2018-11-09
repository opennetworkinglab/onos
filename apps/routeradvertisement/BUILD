COMPILE_DEPS = CORE_DEPS + JACKSON + CLI

osgi_jar_with_tests(
    karaf_command_packages = ["org.onosproject.ra.cli"],
    test_deps = TEST_ADAPTERS,
    deps = COMPILE_DEPS,
)

onos_app(
    category = "Traffic Engineering",
    description = "Application for generating IPv6 RAs",
    title = "IPv6 RA Generator",
    url = "http://onosproject.org",
)
