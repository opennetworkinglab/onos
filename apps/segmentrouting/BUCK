COMPILE_DEPS = [
    '//lib:CORE_DEPS',
    '//lib:JACKSON',
    '//lib:KRYO',
    '//lib:org.apache.karaf.shell.console',
    '//lib:javax.ws.rs-api',
    '//cli:onos-cli',
    '//core/common:onos-core-common',
    '//core/store/serializers:onos-core-serializers',
    '//incubator/api:onos-incubator-api',
    '//utils/rest:onlab-rest',
    '//apps/route-service/api:onos-apps-route-service-api',
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
)

onos_app (
    title = 'Segment Routing',
    category = 'Traffic Steering',
    url = 'http://onosproject.org',
    included_bundles = BUNDLES,
    description = 'Segment routing application.',
    required_apps = [ 'org.onosproject.route-service' ],
)
