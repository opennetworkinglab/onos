COMPILE_DEPS = [
    '//lib:CORE_DEPS',
    '//lib:org.apache.karaf.shell.console',
    '//cli:onos-cli',
]

TEST_DEPS = [
    '//lib:TEST_ADAPTERS',
    '//core/common:onos-core-common',
]

osgi_jar_with_tests (
    deps = COMPILE_DEPS,
    test_deps = TEST_DEPS,
)

onos_app (
    title = 'Authentication App',
    category = 'Security',
    url = 'http://onosproject.org',
    description = 'ONOS authentication application.',
)
