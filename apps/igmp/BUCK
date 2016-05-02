COMPILE_DEPS = [
    '//lib:CORE_DEPS',
    '//lib:org.apache.karaf.shell.console',
    '//cli:onos-cli',
    '//apps/olt:onos-apps-olt-api',
    '//apps/cordconfig:onos-apps-cordconfig',
]

BUNDLES = [
    '//apps/olt:onos-apps-olt-api',
    '//apps/igmp:onos-apps-igmp',
]

osgi_jar_with_tests (
    deps = COMPILE_DEPS,
)

onos_app (
    title = 'IGMP App',
    category = 'Traffic Steering',
    url = 'http://onosproject.org',
    description = 'Internet Group Message Protocol',
    included_bundles = BUNDLES,
)
