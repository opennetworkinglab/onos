COMPILE_DEPS = CORE_DEPS + [
    "@minimal_json//jar",
    "//apps/pi-demo/common:onos-apps-pi-demo-common",
    "//pipelines/basic:onos-pipelines-basic",
]

osgi_jar(
    deps = COMPILE_DEPS,
)

BUNDLES = [
    "//apps/pi-demo/common:onos-apps-pi-demo-common",
    "//apps/pi-demo/ecmp:onos-apps-pi-demo-ecmp",
]

onos_app(
    app_name = "org.onosproject.pi-ecmp",
    category = "Traffic Engineering",
    description = "Provides ECMP support for a 2-stage clos fabric topology of PI-enabled devices",
    included_bundles = BUNDLES,
    required_apps = [
        "org.onosproject.pipelines.basic",
    ],
    title = "PI Demo ECMP Fabric",
    url = "http://onosproject.org",
)
