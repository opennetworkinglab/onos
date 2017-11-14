COMPILE_DEPS = [
    '//lib:CORE_DEPS',
    '//lib:KRYO',
    '//core/store/serializers:onos-core-serializers',
    '//core/store/primitives:onos-core-primitives',
    '//core/api:onos-api',
    '//lib:org.apache.karaf.shell.console',
    '//cli:onos-cli',
]

osgi_jar_with_tests (
    deps = COMPILE_DEPS,
)

onos_app (
    title = 'Trellis Troubleshooting Toolkit',
    category = 'Utilities',
    url = 'http://onosproject.org',
    description = 'Provides static analysis of flows and groups ' +
    'to determine the possible paths a packet may take.',
)
