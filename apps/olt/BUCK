COMPILE_DEPS = [
    '//lib:CORE_DEPS',
    '//lib:javax.ws.rs-api',
    '//lib:jersey-client',
    '//lib:org.apache.karaf.shell.console',
    '//utils/rest:onlab-rest',
    '//cli:onos-cli',
    '//core/store/serializers:onos-core-serializers',
    '//apps/cordconfig:onos-apps-cordconfig',
]

BUNDLES = [
    ':onos-apps-olt-api',
    ':onos-apps-olt',
]

osgi_jar_with_tests (
    name = 'onos-apps-olt-api',
    srcs = glob(['api/' + SRC + '*.java']),
    deps = COMPILE_DEPS,
    visibility = ['PUBLIC'],
)

osgi_jar_with_tests (
    srcs = glob(['app/' + SRC + '*.java']),
    deps = COMPILE_DEPS + [':onos-apps-olt-api'],
    visibility = ['PUBLIC'],
)

onos_app (
    title = 'ONOS OLT REST API',
    category = 'Security',
    url = 'http://onosproject.org',
    description = 'OLT application for CORD.',
    included_bundles = BUNDLES,
)
