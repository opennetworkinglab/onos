COMPILE_DEPS = CORE_DEPS + JACKSON + CLI + [
    "//incubator/api:onos-incubator-api",
]

osgi_jar_with_tests(
    test_deps = TEST_ADAPTERS,
    deps = COMPILE_DEPS,
)

onos_app(
    category = "Traffic Engineering",
    description = "Application for generating IPv6 RAs",
    title = "IPv6 RA Generator",
    url = "http://onosproject.org",
)
