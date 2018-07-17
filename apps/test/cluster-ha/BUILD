COMPILE_DEPS = CORE_DEPS + CLI + KRYO + [
    "//core/store/serializers:onos-core-serializers",
]

osgi_jar(
    deps = COMPILE_DEPS,
)

onos_app(
    category = "Test Utility",
    description = "Test for ONOS Cluster HA",
    title = "Cluster HA Test",
    url = "http://onosproject.org",
)
