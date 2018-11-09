COMPILE_DEPS = CORE_DEPS

BUNDLES = [
    "//apps/routing-api:onos-apps-routing-api",
    "//apps/routing/common:onos-apps-routing-common",
    "//apps/vrouter:onos-apps-vrouter",
]

osgi_jar(
    deps = COMPILE_DEPS,
)

onos_app(
    category = "Traffic Engineering",
    description = "Virtual router (vRouter) application.",
    included_bundles = BUNDLES,
    required_apps = [
        "org.onosproject.fibinstaller",
        "org.onosproject.cpr",
        "org.onosproject.fpm",
    ],
    title = "Virtual Router",
    url = "http://onosproject.org",
)
