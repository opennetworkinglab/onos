COMPILE_DEPS = CORE_DEPS + KRYO + JACKSON + REST + CLI + [
    "@jersey_server//jar",
    "//core/store/serializers:onos-core-serializers",
    "//apps/route-service/api:onos-apps-route-service-api",
    "//apps/dhcprelay/app:onos-apps-dhcprelay-app",
    "//apps/routing/fpm/api:onos-apps-routing-fpm-api",
]

osgi_jar(
    api_description = "REST API for DHCPRelay",
    api_package = "org.onosproject.dhcprelay.rest",
    api_title = "REST API for DHCPRelay Agent Application",
    api_version = "1.0",
    web_context = "/onos/v1/dhcprelay",
    deps = COMPILE_DEPS,
)
