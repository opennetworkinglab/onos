COMPILE_DEPS = CORE_DEPS + ONOS_YANG + [
    "@onos_yang_runtime//jar",
    "//models/openroadm:onos-models-openroadm",
    "//core/store/serializers:onos-core-serializers",
    "//apps/yang:onos-apps-yang-native",
]

osgi_jar_with_tests(
    deps = COMPILE_DEPS,
)
