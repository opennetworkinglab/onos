BUNDLES = [
    "@org_apache_servicemix_bundles_snmp4j//jar",
    "@mibs_rfc//jar",
    "@snmp_core//jar",
    "@mibs_net_snmp//jar",
    "//providers/snmp/device:onos-providers-snmp-device",
    "//providers/snmp/alarm:onos-providers-snmp-alarm",
    "//protocols/snmp/api:onos-protocols-snmp-api",
    "//protocols/snmp/ctl:onos-protocols-snmp-ctl",
]

onos_app(
    app_name = "org.onosproject.snmp",
    category = "Provider",
    description = "Provides means for ONOS to discover and trigger the initial handshake procedure " +
                  "with SNMP enabled devices from information given by network configuration.",
    included_bundles = BUNDLES,
    required_apps = ["org.onosproject.faultmanagement"],
    title = "SNMP Provider",
    url = "https://wiki.onosproject.org/display/ONOS/SNMP",
)
