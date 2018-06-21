COMPILE_DEPS = CORE_DEPS + [
    "@org_apache_karaf_shell_console//jar",
    "//incubator/api:onos-incubator-api",
    "//providers/bgp/cfg:onos-providers-bgp-cfg",
    "//providers/bgp/topology:onos-providers-bgp-topology",
    "//protocols/bgp/api:onos-protocols-bgp-api",
    "//protocols/bgp/ctl:onos-protocols-bgp-ctl",
    "//protocols/bgp/bgpio:onos-protocols-bgp-bgpio",
    "//cli:onos-cli",
]

osgi_jar_with_tests(
    deps = COMPILE_DEPS,
)
