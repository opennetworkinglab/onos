COMPILE_DEPS = CORE_DEPS + JACKSON + KRYO + CLI + REST + [
    "//core/store/serializers:onos-core-serializers",
    "//apps/tunnel/api:onos-apps-tunnel-api",
    "//apps/pcep-api:onos-apps-pcep-api",
    "//apps/pce/bandwidthmgmt:onos-apps-pce-bandwidthmgmt",
]

TEST_DEPS = TEST_REST + [
    "//apps/tunnel/api:onos-apps-tunnel-api-tests",
]

osgi_jar_with_tests(
    karaf_command_packages = ["org.onosproject.pce.cli"],
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
