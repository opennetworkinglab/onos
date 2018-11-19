BUNDLES = [
    "//protocols/ovsdb/rfc:onos-protocols-ovsdb-rfc",
    "//protocols/ovsdb/api:onos-protocols-ovsdb-api",
    "//protocols/ovsdb/ctl:onos-protocols-ovsdb-ctl",
    "//providers/ovsdb/tunnel:onos-providers-ovsdb-tunnel",
    "//providers/ovsdb/device:onos-providers-ovsdb-device",
]

onos_app(
    app_name = "org.onosproject.ovsdb-base",
    category = "Provider",
    description = "ONOS OVSDB provider.",
    included_bundles = BUNDLES,
    required_apps = ["org.onosproject.tunnel"],
    title = "OVSDB Provider",
    url = "http://onosproject.org",
)
