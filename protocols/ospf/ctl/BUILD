COMPILE_DEPS = CORE_DEPS + NETTY + JACKSON + [
    "@netty//jar",
    "//protocols/ospf/api:onos-protocols-ospf-api",
    "//protocols/ospf/protocol:onos-protocols-ospf-protocol",
]

osgi_jar_with_tests(
    exclude_tests = ["org.onosproject.ospf.controller.impl.OspfPipelineFactoryTest"],
    deps = COMPILE_DEPS,
)
