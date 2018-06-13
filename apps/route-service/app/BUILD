COMPILE_DEPS = CORE_DEPS + JACKSON + KRYO + [
    "@org_apache_karaf_shell_console//jar",
    "@concurrent_trees//jar",
    "@javax_ws_rs_api//jar",
    "@jersey_server//jar",
    "//core/store/serializers:onos-core-serializers",
    "//apps/route-service/api:onos-apps-route-service-api",
    "//cli:onos-cli",
    "//utils/rest:onlab-rest",
]

TEST_DEPS = TEST + [
    "//apps/route-service/api:onos-apps-route-service-api-tests",
    "//core/api:onos-api-tests",
]

osgi_jar_with_tests(
    test_deps = TEST_DEPS,
    deps = COMPILE_DEPS,
    #web_context = '/onos/routeservice',
    #api_title = 'Route Service App',
    #api_version = '1.0',
    #api_description = 'REST API for Route Service App',
    #api_package = 'org.onosproject.routeservice.rest',
)
