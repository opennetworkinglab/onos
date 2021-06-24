COMPILE_DEPS = CORE_DEPS + KRYO + CLI + [
    "//pipelines/fabric/api:onos-pipelines-fabric-api",
    "//protocols/p4runtime/model:onos-protocols-p4runtime-model",
    "//protocols/p4runtime/api:onos-protocols-p4runtime-api",
    "//providers/general/device:onos-providers-general-device",
    "//pipelines/basic:onos-pipelines-basic",
    "//core/store/serializers:onos-core-serializers",
    "//drivers/p4runtime:onos-drivers-p4runtime",
]

TEST_DEPS = TEST_ADAPTERS + JACKSON + [
    "//protocols/p4runtime/api:onos-protocols-p4runtime-api",
    "@io_grpc_grpc_java//api",
]

osgi_jar_with_tests(
    karaf_command_packages = ["org.onosproject.pipelines.fabric.impl.behaviour.cli"],
    test_deps = TEST_DEPS,
    deps = COMPILE_DEPS,
)
