COMPILE_DEPS = [
    '//lib:CORE_DEPS',
    '//lib:org.apache.karaf.shell.console',
    '//lib:javax.ws.rs-api',
    '//cli:onos-cli',
    '//core/store/serializers:onos-core-serializers',
    '//incubator/api:onos-incubator-api',
    '//utils/rest:onlab-rest',
]

BUNDLES = [
    '//apps/segmentrouting:onos-apps-segmentrouting',
    '//apps/routing-api:onos-apps-routing-api',
]

TEST_DEPS = [
    '//lib:TEST_ADAPTERS',
]

osgi_jar_with_tests (
    deps = COMPILE_DEPS,
    test_deps = TEST_DEPS,
)

onos_app (
    title = 'Segment Routing App',
    category = 'Traffic Steering',
    url = 'http://onosproject.org',
    included_bundles = BUNDLES,
    description = 'Segment routing application.',
)
