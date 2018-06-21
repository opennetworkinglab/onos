BUNDLES = [
    "@openflowj//jar",
    "//protocols/openflow/api:onos-protocols-openflow-api",
    "//protocols/openflow/ctl:onos-protocols-openflow-ctl",
    "//providers/openflow/device:onos-providers-openflow-device",
    "//providers/openflow/packet:onos-providers-openflow-packet",
    "//providers/openflow/flow:onos-providers-openflow-flow",
    "//providers/openflow/group:onos-providers-openflow-group",
    "//providers/openflow/meter:onos-providers-openflow-meter",
]

APPS = [
    "org.onosproject.optical-model",
]

onos_app(
    app_name = "org.onosproject.openflow-base",
    category = "Provider",
    description = "Provides the base suite of device, flow and packet providers that rely on the " +
                  "OpenFlow protocol to interact with network devices.",
    included_bundles = BUNDLES,
    required_apps = APPS,
    title = "OpenFlow Base Provider",
    url = "https://wiki.onosproject.org/display/ONOS/OpenFlow",
)
