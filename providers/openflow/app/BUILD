APPS = [
    "org.onosproject.hostprovider",
    "org.onosproject.lldpprovider",
    "org.onosproject.openflow-base",
]

onos_app(
    app_name = "org.onosproject.openflow",
    category = "Provider",
    description = "Suite of the OpenFlow base providers bundled together with ARP/NDP host " +
                  "location provider and LLDP link provider.",
    included_bundles = ["//utils/osgi:onlab-osgi"],  # FIXME to allow hollow oar files
    required_apps = APPS,
    title = "OpenFlow Provider Suite",
    url = "http://onosproject.org",
)
