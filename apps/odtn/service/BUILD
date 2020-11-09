COMPILE_DEPS = CORE_DEPS + ONOS_YANG + CLI + JACKSON + [
    "//apps/odtn/api:onos-apps-odtn-api",
    "//apps/config:onos-apps-config",
    "//models/tapi:onos-models-tapi",
    "//apps/yang:onos-apps-yang",
    "//apps/optical-model:onos-apps-optical-model",
    "//protocols/netconf/api:onos-protocols-netconf-api",
    "@jersey_client//jar",
    "@jersey_server//jar",
    "@jersey_common//jar",
    "@jersey_security//jar",
    "@httpclient_osgi//jar",
    "@httpcore_osgi//jar",
    "@javax_ws_rs_api//jar",
    "@hk2_api//jar",
    "@aopalliance_repackaged//jar",
    "@javax_inject//jar",
    "//utils/rest:onlab-rest",
]

osgi_jar_with_tests(
    karaf_command_packages = ["org.onosproject.odtn.cli.impl"],
    test_deps = TEST_ADAPTERS,
    deps = COMPILE_DEPS,
)

APPS = [
    "org.onosproject.yang",
    "org.onosproject.config",
    #"org.onosproject.configsync",
    "org.onosproject.models.tapi",
    "org.onosproject.odtn-api",

    # strictly speaking following are not mandatory
    "org.onosproject.restconf",
    "org.onosproject.drivers.netconf",  # will need if using TemplateManager
    "org.onosproject.drivers.odtn-driver",
    "org.onosproject.netconf",
    "org.onosproject.optical-model",
    #"org.onosproject.configsync-netconf",
    "org.onosproject.protocols.restconfserver",
]

onos_app(
    app_name = "org.onosproject.odtn-service",
    category = "Traffic Engineering",
    description = "ODTN Service Application",
    required_apps = APPS,
    title = "ODTN Service Application",
    url = "http://onosproject.org",
)
