COMPILE_DEPS = CORE_DEPS + JACKSON + [
    "@org_apache_servicemix_bundles_snmp4j//jar",
    "//protocols/snmp/api:onos-protocols-snmp-api",
    "//protocols/snmp/ctl:onos-protocols-snmp-ctl",
    "//providers/snmp/device:onos-providers-snmp-device",
]

osgi_jar_with_tests(
    exclude_tests = ["org.onosproject.provider.snmp.alarm.impl.SnmpAlarmProviderTest"],
    deps = COMPILE_DEPS,
)
