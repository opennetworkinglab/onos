COMPILE_DEPS = CORE_DEPS + KRYO + [
    "//pipelines/fabric/api:onos-pipelines-fabric-api",
    "//protocols/p4runtime/model:onos-protocols-p4runtime-model",
    "//protocols/p4runtime/api:onos-protocols-p4runtime-api",
    "//providers/general/device:onos-providers-general-device",
    "//pipelines/basic:onos-pipelines-basic",
    "//core/store/serializers:onos-core-serializers",
    "//drivers/p4runtime:onos-drivers-p4runtime",
]

osgi_jar_with_tests(
    test_deps = TEST_ADAPTERS,
    deps = COMPILE_DEPS,
)
