COMPILE_DEPS = [
    '//lib:CORE_DEPS',
    '//lib:KRYO',
    '//core/store/serializers:onos-core-serializers',
    '//core/store/primitives:onos-core-primitives',
    '//core/api:onos-api',
    '//lib:org.apache.karaf.shell.console',
    '//cli:onos-cli',
    '//drivers/default:onos-drivers-default',
    '//apps/segmentrouting/app:onos-apps-segmentrouting-app',
    '//apps/route-service/api:onos-apps-route-service-api',
    '//apps/mcast/api:onos-apps-mcast-api',
]

TEST_DEPS = [
    '//lib:TEST_ADAPTERS',
    '//utils/misc:onlab-misc',
    '//apps/route-service/api:onos-apps-route-service-api-tests',
]

osgi_jar_with_tests (
    deps = COMPILE_DEPS,
    test_deps = TEST_DEPS,
)

onos_app (
    title = 'Trellis Troubleshooting Toolkit',
    category = 'Monitoring',
    url = 'https://wiki.opencord.org/pages/viewpage.action?pageId=4456974',
    description = 'Provides static analysis of flows and groups ' +
    'to determine the possible paths a packet may take.',
    required_apps = [
        'org.onosproject.segmentrouting',
        'org.onosproject.route-service',
        'org.onosproject.mcast',
    ],
)
