COMPILE_DEPS = CORE_DEPS + JACKSON + KRYO + REST + CLI + [
    "@jersey_client//jar",
    "//core/store/serializers:onos-core-serializers",
]

osgi_jar_with_tests(
    api_description = "REST API for Vpls ",
    api_package = "org.onosproject.vpls.rest",
    api_title = "VPLS REST",
    api_version = "1.0",
    exclude_tests = ["org.onosproject.vpls.VplsTest"],
    karaf_command_packages = [
        "org.onosproject.vpls.cli",
        "org.onosproject.vpls.cli.completer",
    ],
    test_deps = TEST_ADAPTERS,
    web_context = "/onos/vpls",
    deps = COMPILE_DEPS,
)

onos_app(
    category = "Traffic Engineering",
    description = "Application to create L2 broadcast network using VLAN.",
    required_apps = ["org.onosproject.intentsynchronizer"],
    title = "VLAN L2 Broadcast Network",
    url = "http://onosproject.org",
)
