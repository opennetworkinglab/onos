COMPILE_DEPS = CORE_DEPS + NETTY + JACKSON + KRYO + CLI + REST + [
    "@io_netty_netty_transport//jar",
    "@io_netty_netty_codec//jar",
    "@io_netty_netty_handler//jar",
    "@openflowj//jar",
    "//core/store/serializers:onos-core-serializers",
    "//core/common:onos-core-common",
    "//apps/virtual/api:onos-apps-virtual-api",
    "//providers/openflow/flow:onos-providers-openflow-flow",
    "//protocols/openflow/api:onos-protocols-openflow-api",
]

BUNDLES = [
    "//apps/ofagent:onos-apps-ofagent",
    "//providers/openflow/flow:onos-providers-openflow-flow",
    "//protocols/openflow/api:onos-protocols-openflow-api",
]

TEST_DEPS = TEST_ADAPTERS + TEST_REST + [
    "@jersey_server//jar",
    "@minimal_json//jar",
    "//core/api:onos-api-tests",
    "//core/common:onos-core-common-tests",
    "//web/api:onos-rest-tests",
]

osgi_jar_with_tests(
    api_description = "REST API for OFAgent",
    api_package = "org.onosproject.ofagent.rest",
    api_title = "OFAgent API",
    api_version = "1.0",
    karaf_command_packages = ["org.onosproject.ofagent.cli"],
    test_deps = TEST_DEPS,
    web_context = "/onos/ofagent",
    deps = COMPILE_DEPS,
)

onos_app(
    category = "Traffic Engineering",
    description = "OpenFlow agent application for virtualization subsystem.",
    included_bundles = BUNDLES,
    required_apps = [
        "org.onosproject.virtual",
    ],
    title = "OpenFlow Agent",
    url = "http://onosproject.org",
)
