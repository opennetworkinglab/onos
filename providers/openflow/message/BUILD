COMPILE_DEPS = CORE_DEPS + METRICS + [
    "@openflowj//jar",
    "//protocols/openflow/api:onos-protocols-openflow-api",
    "//apps/cpman/api:onos-apps-cpman-api",
]

BUNDLES = [
    "//apps/cpman/api:onos-apps-cpman-api",
    ":onos-providers-openflow-message",
]

osgi_jar_with_tests(
    exclude_tests = [
        "org.onosproject.provider.of.message.impl.OpenFlowControlMessageProviderTest",
    ],
    deps = COMPILE_DEPS,
)

onos_app(
    app_name = "org.onosproject.openflow-message",
    category = "Provider",
    description = "ONOS OpenFlow control message provider.",
    included_bundles = BUNDLES,
    title = "Control Message Stats Provider",
    url = "http://onosproject.org",
)
