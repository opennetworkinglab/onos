COMPILE_DEPS = [
    '//lib:CORE_DEPS',
    '//lib:javax.ws.rs-api',
    '//lib:jersey-client',
    '//lib:jersey-common',
    '//utils/rest:onlab-rest',
    '//apps/olt:onos-apps-olt-api',
]

BUNDLES = [
    '//apps/olt:onos-apps-olt-api',
    '//apps/cordmcast:onos-apps-cordmcast',
]

osgi_jar_with_tests (
    deps = COMPILE_DEPS,
)

onos_app (
    title = 'CORD Multicast App',
    category = 'Traffic Steering',
    url = 'http://onosproject.org',
    description = 'CORD Multicast application',
    included_bundles = BUNDLES,
)
