COMPILE_DEPS = CORE_DEPS + NETTY + JACKSON + [
    "@org_apache_karaf_shell_console//jar",
    "//providers/lldpcommon:onos-providers-lldpcommon",
    "//cli:onos-cli",
]

BUNDLES = [
    "//providers/lldpcommon:onos-providers-lldpcommon",
    "//providers/lldp:onos-providers-lldp",
]

osgi_jar_with_tests(
    test_deps = TEST_ADAPTERS,
    deps = COMPILE_DEPS,
)

onos_app(
    app_name = "org.onosproject.lldpprovider",
    category = "Provider",
    description = "Provides link discovery to the ONOS core by eavesdropping on the LLDP control packets.",
    included_bundles = BUNDLES,
    title = "LLDP Link Provider",
    url = "http://onosproject.org",
)
