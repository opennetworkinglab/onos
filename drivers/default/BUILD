COMPILE_DEPS = CORE_DEPS + JACKSON + KRYO + [
    "@openflowj//jar",
    "//protocols/openflow/api:onos-protocols-openflow-api",
    "//core/store/serializers:onos-core-serializers",
]

BUNDLES = [
    ":onos-drivers-default",
    "@openflowj//jar",
    "//protocols/openflow/api:onos-protocols-openflow-api",
]

osgi_jar_with_tests(
    resources = glob(["src/main/resources/**"]),
    test_deps = TEST_ADAPTERS,
    visibility = ["//visibility:public"],
    deps = COMPILE_DEPS,
)

onos_app(
    app_name = "org.onosproject.drivers",
    category = "Drivers",
    description = "Suite of default drivers.",
    included_bundles = BUNDLES,
    title = "Default Drivers",
    url = "http://onosproject.org",
)
