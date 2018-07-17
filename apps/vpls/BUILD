COMPILE_DEPS = CORE_DEPS + JACKSON + KRYO + REST + CLI + [
    "@jersey_client//jar",
    "//incubator/api:onos-incubator-api",
    "//core/store/serializers:onos-core-serializers",
]

osgi_jar_with_tests(
    exclude_tests = ["org.onosproject.vpls.VplsTest"],
    test_deps = TEST_ADAPTERS,
    deps = COMPILE_DEPS,
)

onos_app(
    category = "Traffic Engineering",
    description = "Application to create L2 broadcast network using VLAN.",
    required_apps = ["org.onosproject.intentsynchronizer"],
    title = "VLAN L2 Broadcast Network",
    url = "http://onosproject.org",
)
