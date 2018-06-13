COMPILE_DEPS = CORE_DEPS + JACKSON + [
    '@javax_ws_rs_api//jar',
    '//incubator/api:onos-incubator-api',
    '//utils/rest:onlab-rest',
    '//drivers/utilities:onos-drivers-utilities',
    '//protocols/rest/api:onos-protocols-rest-api',
]

TEST_DEPS = TEST_ADAPTERS + [
    '//core/api:onos-api-tests',
]

BUNDLES = [
    ':onos-drivers-arista',
    '//drivers/utilities:onos-drivers-utilities',
]

osgi_jar_with_tests (
    deps = COMPILE_DEPS,
    test_deps = TEST_DEPS,
    resources_root = 'src/main/resources',
    resources = glob(['src/main/resources/**']),
)

onos_app (
    app_name = 'org.onosproject.drivers.arista',
    title = 'Arista Drivers',
    category = 'Drivers',
    url = 'http://onosproject.org',
    description = 'Adds support for Arista devices.',
    included_bundles = BUNDLES,
    required_apps = [ 'org.onosproject.restsb' ],
)
