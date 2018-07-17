COMPILE_DEPS = CORE_DEPS + JACKSON + METRICS + CLI + [
    "//core/store/serializers:onos-core-serializers",
    "//core/store/primitives:onos-core-primitives",
]

osgi_jar_with_tests(
    deps = COMPILE_DEPS,
)

onos_app(
    category = "Traffic Engineering",
    description = "Application to calculate the number of packets of different types",
    title = "Packet Statistics",
    url = "http://onosproject.org",
)
