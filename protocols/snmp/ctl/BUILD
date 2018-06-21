COMPILE_DEPS = CORE_DEPS + [
    "@org_apache_servicemix_bundles_snmp4j//jar",
    "@snmp_core//jar",
    "@mibs_net_snmp//jar",
    "@mibs_rfc//jar",
    "//incubator/api:onos-incubator-api",
    "//protocols/snmp/api:onos-protocols-snmp-api",
]

osgi_jar_with_tests(
    deps = COMPILE_DEPS,
)
