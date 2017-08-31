COMPILE_DEPS = [
    '//lib:CORE_DEPS',
    '//lib:JACKSON',
    '//lib:KRYO',
    '//lib:org.apache.karaf.shell.console',
    '//lib:javax.ws.rs-api',
    '//cli:onos-cli',
    '//core/store/serializers:onos-core-serializers',
    '//incubator/api:onos-incubator-api',
    '//utils/rest:onlab-rest',
    '//apps/route-service/api:onos-apps-route-service-api',
    '//lib:joda-time',
]

BUNDLES = [
    '//apps/segmentrouting:onos-apps-segmentrouting',
    '//apps/routing-api:onos-apps-routing-api',
]

TEST_DEPS = [
    '//lib:TEST_ADAPTERS',
    '//incubator/api:onos-incubator-api-tests',
    '//apps/route-service/api:onos-apps-route-service-api-tests',
]

osgi_jar_with_tests (
    deps = COMPILE_DEPS,
    test_deps = TEST_DEPS,
    # TODO Uncomment here when policy/tunnel are supported
    #web_context = '/onos/segmentrouting',
    #api_title = 'Segment Routing',
    #api_version = '1.0',
    #api_description = 'REST API for Segment Routing',
    #api_package = 'org.onosproject.segmentrouting',
)

onos_app (
    title = 'Segment Routing',
    category = 'Traffic Steering',
    url = 'http://onosproject.org',
    included_bundles = BUNDLES,
    description = 'Segment routing application.',
    required_apps = [ 'org.onosproject.route-service' ],
)
