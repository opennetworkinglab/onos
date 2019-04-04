BUNDLES = [
    "@jsch//jar",
    "//protocols/ovsdb/api:onos-protocols-ovsdb-api",
    "//protocols/ovsdb/rfc:onos-protocols-ovsdb-rfc",
    "//apps/workflow/ofoverlay/app:onos-apps-workflow-ofoverlay-app",
]

onos_app(
    app_name = "org.onosproject.workflow.ofoverlay",
    category = "Utility",
    description = "Openflow overaly application",
    included_bundles = BUNDLES,
    required_apps = [
        "org.onosproject.openflow",
        "org.onosproject.ovsdb",
        "org.onosproject.drivers.ovsdb",
        "org.onosproject.workflow",
    ],
    title = "Openflow overlay",
    url = "http://onosproject.org",
)
