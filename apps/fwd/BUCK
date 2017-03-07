COMPILE_DEPS = [
    '//lib:CORE_DEPS',
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
    title = 'Reactive Forwarding App',
    category = 'Traffic Steering',
    url = 'http://onosproject.org',
    description = 'Reactive forwarding application using flow objective subsystem.',
)
