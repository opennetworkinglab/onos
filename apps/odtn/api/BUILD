COMPILE_DEPS = CORE_DEPS + JACKSON + ONOS_YANG + [
    "@onos_yang_runtime//jar",
    "//models/tapi:onos-models-tapi",
    "//models/openconfig:onos-models-openconfig",
    "//apps/yang:onos-apps-yang",
    "//apps/config:onos-apps-config",
    "//models/openconfig-infinera:onos-models-openconfig-infinera",
    "//models/openconfig-odtn:onos-models-openconfig-odtn",
    "//protocols/netconf/api:onos-protocols-netconf-api",
]

osgi_jar_with_tests(
    test_deps = TEST_ADAPTERS,
    deps = COMPILE_DEPS,
)

APPS = [
    "org.onosproject.config",
    "org.onosproject.yang",
    "org.onosproject.models.tapi",
    "org.onosproject.models.openconfig",
    "org.onosproject.models.openconfig-infinera",
    "org.onosproject.models.openconfig-odtn",
    "org.onosproject.netconf",
]

# TODO probably bucklet, etc. should escape title & description
onos_app(
    app_name = "org.onosproject.odtn-api",
    category = "Traffic Engineering",
    description = "ODTN API &amp; Utilities Application",
    required_apps = APPS,
    title = "ODTN API &amp; Utilities Application",
    url = "http://onosproject.org",
)
