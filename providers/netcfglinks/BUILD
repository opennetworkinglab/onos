COMPILE_DEPS = CORE_DEPS + NETTY + [
    "//providers/lldpcommon:onos-providers-lldpcommon",
]

BUNDLES = [
    "//providers/lldpcommon:onos-providers-lldpcommon",
    "//providers/netcfglinks:onos-providers-netcfglinks",
]

osgi_jar_with_tests(
    test_deps = TEST_ADAPTERS,
    deps = COMPILE_DEPS,
)

onos_app(
    app_name = "org.onosproject.netcfglinksprovider",
    category = "Provider",
    description = "Allows operator to prescribe information about known infrastructure links and " +
                  "their state using the network configuration subsystem. Such information takes precedence " +
                  "over information discovered through other link discovery providers.",
    included_bundles = BUNDLES,
    title = "Network Config Link Provider",
    url = "http://onosproject.org",
)
