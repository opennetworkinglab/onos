COMPILE_DEPS = CORE_DEPS + KRYO + CLI + [
    "@javax_ws_rs_api//jar",
    "//utils/rest:onlab-rest",
    "//core/store/serializers:onos-core-serializers",
]

osgi_jar(
    karaf_command_packages = ["org.onosproject.distributedprimitives.cli"],
    deps = COMPILE_DEPS,
)

onos_app(
    app_name = "org.onosproject.distributedprimitives",
    category = "Test Utility",
    description = "ONOS app to test distributed primitives.",
    title = "Distributed Primitives Test",
    url = "http://onosproject.org",
)
