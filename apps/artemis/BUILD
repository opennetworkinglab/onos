COMPILE_DEPS = CORE_DEPS + JACKSON + NETTY + CLI + [
    "@okhttp//jar",
    "@okio//jar",
    "@commons_net//jar",
    "@io_socket_client//jar",
    "@minimal_json//jar",
    "@engine_io_client//jar",
    "@io_netty_netty//jar",
    "@io_netty_netty_transport//jar",
    "//apps/routing-api:onos-apps-routing-api",
    "//apps/routing/common:onos-apps-routing-common",
    "//protocols/ovsdb/api:onos-protocols-ovsdb-api",
    "//apps/intentsync:onos-apps-intentsync",
    "//apps/route-service/api:onos-apps-route-service-api",
    "//protocols/ovsdb/rfc:onos-protocols-ovsdb-rfc",
]

BUNDLES = [
    "@commons_net//jar",
    "@io_socket_client//jar",
    "@minimal_json//jar",
    "@engine_io_client//jar",
    "//apps/artemis:onos-apps-artemis",
    "//apps/routing-api:onos-apps-routing-api",
    "//apps/routing/common:onos-apps-routing-common",
    "//protocols/ovsdb/api:onos-protocols-ovsdb-api",
    "//apps/route-service/api:onos-apps-route-service-api",
    "//protocols/ovsdb/rfc:onos-protocols-ovsdb-rfc",
]

EXCLUDED_BUNDLES = [
    "@okhttp//jar",
    "@okio//jar",
]

osgi_jar_with_tests(
    test_deps = TEST_ADAPTERS,
    deps = COMPILE_DEPS,
)

onos_app(
    app_name = "org.onosproject.artemis",
    category = "Monitoring",
    description = "Artemis",
    excluded_bundles = EXCLUDED_BUNDLES,
    included_bundles = BUNDLES,
    required_apps = [
        "org.onosproject.sdnip",
        "org.onosproject.openflow",
        "org.onosproject.ovsdb",
        "org.onosproject.drivers.ovsdb",
    ],
    title = "Artemis",
    url = "http://onosproject.org",
)
