COMPILE_DEPS = CORE_DEPS + JACKSON + [
    "//apps/routing-api:onos-apps-routing-api",
]

BUNDLES = [
    "//apps/routing-api:onos-apps-routing-api",
    "//apps/routing/common:onos-apps-routing-common",
    "//apps/bgprouter:onos-apps-bgprouter",
]

osgi_jar(
    deps = COMPILE_DEPS,
)

onos_app(
    category = "Traffic Engineering",
    description = "BGP router application.",
    included_bundles = BUNDLES,
    required_apps = [
        "org.onosproject.fibinstaller",
        "org.onosproject.route-service",
    ],
    title = "BGP Router",
    url = "http://onosproject.org",
)
