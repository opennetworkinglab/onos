COMPILE_DEPS = CORE_DEPS + JACKSON + KRYO + CLI + REST + [
    "//core/store/serializers:onos-core-serializers",
    "//incubator/api:onos-incubator-api",
    "//apps/pcep-api:onos-apps-pcep-api",
    "//apps/pce/bandwidthmgmt:onos-apps-pce-bandwidthmgmt",
]

TEST_DEPS = TEST_REST + [
    "//incubator/api:onos-incubator-api-tests",
]

osgi_jar_with_tests(
    test_deps = TEST_DEPS,
    deps = COMPILE_DEPS,
)

onos_app(
    app_name = "org.onosproject.pce.app",
    category = "default",
    description = "PCE as centeral controller App.",
    title = "PCE",
    url = "http://onosproject.org",
)
