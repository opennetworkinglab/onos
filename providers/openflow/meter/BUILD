COMPILE_DEPS = CORE_DEPS + NETTY + [
    "@openflowj//jar",
    "//protocols/openflow/api:onos-protocols-openflow-api",
]

osgi_jar_with_tests(
    exclude_tests = [
        "org.onosproject.provider.of.meter.impl.OpenFlowMeterProviderTest",
    ],
    deps = COMPILE_DEPS,
)
