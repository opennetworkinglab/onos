APPS = [
    "org.onosproject.configsync",
    "org.onosproject.yang",
    "org.onosproject.netconf",
]

COMPILE_DEPS = CORE_DEPS + [
    "@onos_yang_model//jar",
    "@onos_yang_runtime//jar",
    "//protocols/netconf/api:onos-protocols-netconf-api",
    "//apps/config:onos-apps-config",
    "//apps/configsync:onos-apps-configsync",
]

osgi_jar_with_tests(
    test_deps = TEST_ADAPTERS,
    deps = COMPILE_DEPS,
)

onos_app(
    category = "Utility",
    description = "Application to support the Dynamic configuration service.",
    required_apps = APPS,
    title = "Dynamic Configuration Synchronizer for NETCONF",
    url = "http://onosproject.org",
)
