COMPILE_DEPS = CORE_DEPS + NETTY + JACKSON + CLI + [
    "//providers/lldpcommon:onos-providers-lldpcommon",
]

BUNDLES = [
    "//providers/lldpcommon:onos-providers-lldpcommon",
    "//providers/lldp:onos-providers-lldp",
]

osgi_jar_with_tests(
    karaf_command_packages = ["org.onosproject.provider.lldp.cli"],
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
