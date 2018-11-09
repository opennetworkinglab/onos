COMPILE_DEPS = CORE_DEPS + CLI + [
    "//providers/bgp/cfg:onos-providers-bgp-cfg",
    "//providers/bgp/topology:onos-providers-bgp-topology",
    "//protocols/bgp/api:onos-protocols-bgp-api",
    "//protocols/bgp/ctl:onos-protocols-bgp-ctl",
    "//protocols/bgp/bgpio:onos-protocols-bgp-bgpio",
]

osgi_jar_with_tests(
    karaf_command_packages = ["org.onosproject.bgp.cli"],
    deps = COMPILE_DEPS,
)
