COMPILE_DEPS = CORE_DEPS + JACKSON + CLI + [
    "@httpclient_osgi//jar",
    "@httpcore_osgi//jar",
    "@org_apache_httpcomponents_httpasyncclient_osgi//jar",
    "@org_apache_httpcomponents_httpcore_nio//jar",
    "@org_apache_karaf_jaas//jar",
]

BUNDLES = [
    ":onos-apps-gluon",
    "@httpclient_osgi//jar",
    "@httpcore_osgi//jar",
    "@org_apache_httpcomponents_httpasyncclient_osgi//jar",
    "@org_apache_httpcomponents_httpcore_nio//jar",
]

osgi_jar_with_tests(
    deps = COMPILE_DEPS,
)

onos_app(
    app_name = "org.onosproject.gluon",
    category = "Monitoring",
    description = "To fetch data from Gluon Server over Http session.",
    included_bundles = BUNDLES,
    title = "Gluon Shim",
    url = "http://onosproject.org",
)
