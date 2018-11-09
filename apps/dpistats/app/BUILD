COMPILE_DEPS = CORE_DEPS + JACKSON + REST + CLI + [
    "//apps/dpistats/api:onos-apps-dpistats-api",
]

TEST_DEPS = TEST_ADAPTERS

osgi_jar_with_tests(
    api_description = "REST API for DPI Stats",
    api_package = "org.onosproject.incubator.net.dpi",
    api_title = "DPI Stats",
    api_version = "1.0",
    karaf_command_packages = ["org.onosproject.incubator.net.dpi"],
    test_deps = TEST_DEPS,
    visibility = ["//visibility:public"],
    deps = COMPILE_DEPS,
)
