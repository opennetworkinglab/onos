APPS = [
    "org.onosproject.yang",
    "org.onosproject.yang-gui",
    "org.onosproject.config",
    "org.onosproject.restconf",
    "org.onosproject.protocols.restconfserver",
    "org.onosproject.netconf",
    #"org.onosproject.netconfsb",
    "org.onosproject.models.openroadm",
]

BUNDLES = [
    "//apps/openroadm/service:onos-apps-openroadm-service",
    "//apps/openroadm/network:onos-apps-openroadm-network",
]

onos_app(
    app_name = "org.onosproject.openroadm",
    category = "Optical",
    description = "Open ROADM Application",
    included_bundles = BUNDLES,
    required_apps = APPS,
    title = "Open ROADM",
    url = "http://onosproject.org",
)
