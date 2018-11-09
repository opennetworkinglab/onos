COMPILE_DEPS = CORE_DEPS + JACKSON + [
    "@org_apache_servicemix_bundles_snmp4j//jar",
    "@snmp_core//jar",
    "@mibs_net_snmp//jar",
    "@mibs_rfc//jar",
    "//protocols/snmp/api:onos-protocols-snmp-api",
    "//protocols/snmp/ctl:onos-protocols-snmp-ctl",
]

TEST_DEPS = TEST + [
    "//core/api:onos-api-tests",
]

osgi_jar_with_tests(
    test_deps = TEST_DEPS,
    deps = COMPILE_DEPS,
)
