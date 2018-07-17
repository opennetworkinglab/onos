COMPILE_DEPS = CORE_DEPS + KRYO + [
    "//protocols/p4runtime/model:onos-protocols-p4runtime-model",
    "//protocols/p4runtime/api:onos-protocols-p4runtime-api",
    "//pipelines/basic:onos-pipelines-basic",
    "//core/store/serializers:onos-core-serializers",
]

BUNDLES = [
    "//pipelines/fabric:onos-pipelines-fabric",
]

osgi_jar_with_tests(
    exclude_tests = ["org/onosproject/pipelines/fabric/pipeliner/FabricPipelinerTest"],
    test_deps = TEST_ADAPTERS,
    deps = COMPILE_DEPS,
)

onos_app(
    app_name = "org.onosproject.pipelines.fabric",
    category = "Pipeline",
    description = "Provides pipelines with CORD fabric underlay support.",
    included_bundles = BUNDLES,
    required_apps = [
        "org.onosproject.drivers.p4runtime",
        "org.onosproject.pipelines.basic",
    ],
    title = "Fabric Pipeline",
    url = "http://onosproject.org",
)
