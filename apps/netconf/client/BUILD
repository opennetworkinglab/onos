APPS = [
    "org.onosproject.yang",
    "org.onosproject.config",
    "org.onosproject.netconf",
]

COMPILE_DEPS = CORE_DEPS + ONOS_YANG + [
    "@onos_yang_runtime//jar",
    "//apps/config:onos-apps-config",
    "//protocols/netconf/api:onos-protocols-netconf-api",
]

osgi_jar_with_tests(
    deps = COMPILE_DEPS,
)

onos_app(
    app_name = "org.onosproject.netconfsb",
    category = "Protocol",
    description = "Exposes APIs to establish NETCONF connections to devices and to send and receive " +
                  "messages and asynchronous notifications over such connection.",
    required_apps = APPS,
    title = "NETCONF Protocol Subsystem",
    url = "http://onosproject.org",
)
