COMPILE_DEPS = CORE_DEPS + JACKSON + [
    "//core/store/serializers:onos-core-serializers",
    "//apps/optical-model:onos-apps-optical-model",
]

TEST_DEPS = TEST_REST + [
    "//core/api:onos-api-tests",
]

osgi_jar_with_tests(
    test_deps = TEST_DEPS,
    deps = COMPILE_DEPS,
)

onos_app(
    category = "Optical",
    description = "This application provides an interface and web GUI for monitoring and " +
                  "configuring power on ROADM devices.",
    required_apps = ["org.onosproject.optical-model"],
    title = "ROADM",
    url = "http://onosproject.org",
)
