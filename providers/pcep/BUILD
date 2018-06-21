BUNDLES = [
    "@netty//jar",
    "//providers/pcep/topology:onos-providers-pcep-topology",
    "//providers/pcep/tunnel:onos-providers-pcep-tunnel",
    "//providers/pcep/cli:onos-providers-pcep-cli",
    "//protocols/pcep/server/api:onos-protocols-pcep-server-api",
    "//protocols/pcep/pcepio:onos-protocols-pcep-pcepio",
    "//protocols/pcep/server/ctl:onos-protocols-pcep-server-ctl",
    "//apps/pcep-api:onos-apps-pcep-api",
]

onos_app(
    category = "Provider",
    description = "PCEP protocol providers root.",
    included_bundles = BUNDLES,
    required_apps = [
        "org.onosproject.pcep-api",
        "org.onosproject.optical-model",
    ],
    title = "PCEP Provider",
    url = "http://onosproject.org",
)
