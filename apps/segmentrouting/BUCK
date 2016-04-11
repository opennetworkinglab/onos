COMPILE_DEPS = [
    '//lib:CORE_DEPS',
    '//lib:org.apache.karaf.shell.console',
    '//lib:javax.ws.rs-api',
    '//cli:onos-cli',
    '//incubator/api:onos-incubator-api',
    '//utils/rest:onlab-rest',

]

TEST_DEPS = [
    '//lib:TEST_ADAPTERS',
]

osgi_jar_with_tests (
    deps = COMPILE_DEPS,
    test_deps = TEST_DEPS,
)

onos_app (
    title = 'Proxy ARP/NDP App',
    category = 'Traffic Steering',
    url = 'http://onosproject.org',
    description = 'Proxy ARP/NDP application.',
)
