APPS = [
    "org.onosproject.ovsdb-base",
    "org.onosproject.ovsdbhostprovider",
    "org.onosproject.tunnel",
]

onos_app(
    app_name = "org.onosproject.ovsdb",
    category = "Provider",
    description = "ONOS OVSDB southbound meta application",
    included_bundles = ["//utils/osgi:onlab-osgi"],  # FIXME to allow hollow oar files
    required_apps = APPS,
    title = "OVSDB Southbound Meta",
    url = "http://onosproject.org",
)
