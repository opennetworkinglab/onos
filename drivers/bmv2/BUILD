COMPILE_DEPS = CORE_DEPS + KRYO + JACKSON + [
    "//core/store/serializers:onos-core-serializers",
    "//drivers/p4runtime:onos-drivers-p4runtime",
    "//pipelines/basic:onos-pipelines-basic",
    "//protocols/p4runtime/api:onos-protocols-p4runtime-api",
    "//protocols/p4runtime/model:onos-protocols-p4runtime-model",
    "//deps:io_grpc_grpc_api_context",
    "//deps:io_grpc_grpc_netty",
    "@minimal_json//jar",
    # "//protocols/bmv2/thrift-api:onos-protocols-bmv2-thrift-api",
    #"//deps:libthrift",
]

BUNDLES = [
    ":onos-drivers-bmv2",
    # "//deps:libthrift",
    # "//protocols/bmv2/thrift-api:onos-protocols-bmv2-thrift-api",
]

osgi_jar(
    # FIXME: enable building other classes as soon as we implement BUILD for
    # protocols/bmv2 (based on Thrift)
    srcs = [
        "src/main/java/org/onosproject/drivers/bmv2/Bmv2DriversLoader.java",
        "src/main/java/org/onosproject/drivers/bmv2/Bmv2PipelineProgrammable.java",
    ],
    resources = glob(["src/main/resources/**"]),
    resources_root = "src/main/resources",
    deps = COMPILE_DEPS,
)

onos_app(
    app_name = "org.onosproject.drivers.bmv2",
    category = "Drivers",
    description = "Adds support for BMv2 devices.",
    included_bundles = BUNDLES,
    required_apps = [
        "org.onosproject.drivers.stratum",
        "org.onosproject.drivers.p4runtime",
        "org.onosproject.pipelines.basic",
    ],
    title = "BMv2 Drivers",
    url = "http://onosproject.org",
)
