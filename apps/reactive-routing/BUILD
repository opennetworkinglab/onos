COMPILE_DEPS = CORE_DEPS + JACKSON + [
    "@concurrent_trees//jar",
    "//apps/routing-api:onos-apps-routing-api",
    "//apps/intentsync:onos-apps-intentsync",
    "//apps/route-service/api:onos-apps-route-service-api",
]

osgi_jar(
    deps = COMPILE_DEPS,
)

onos_app(
    category = "Traffic Engineering",
    description = "SDN-IP reactive routing application.",
    required_apps = [
        "org.onosproject.intentsynchronizer",
        "org.onosproject.sdnip",
        "org.onosproject.route-service",
    ],
    title = "SDN-IP Reactive Routing",
    url = "http://onosproject.org",
)
