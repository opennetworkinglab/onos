COMPILE_DEPS = CORE_DEPS + KRYO + ONOS_YANG + [
    "//models/l3vpn:onos-models-l3vpn",
    "//apps/config:onos-apps-config",
    "//core/store/serializers:onos-core-serializers",
    "//apps/yang:onos-apps-yang",
    "//apps/pce/app:onos-apps-pce-app",
    "//incubator/api:onos-incubator-api",
    "//models/common:onos-models-common",
]

TEST_DEPS = TEST_ADAPTERS + [
    "//utils/osgi:onlab-osgi-tests",
]

APPS = [
    "org.onosproject.yang",
    "org.onosproject.yang-gui",
    "org.onosproject.config",
    "org.onosproject.restconf",
    "org.onosproject.protocols.restconfserver",
    "org.onosproject.netconf",
    "org.onosproject.netconfsb",
    "org.onosproject.models.common",
    "org.onosproject.models.l3vpn",
    "org.onosproject.bgpcep",
]

osgi_jar_with_tests(
    test_deps = TEST_DEPS,
    deps = COMPILE_DEPS,
)

onos_app(
    app_name = "org.onosproject.l3vpn",
    category = "Traffic Engineering",
    description = "L3VPN YANG Application",
    required_apps = APPS,
    title = "YANG L3VPN",
    url = "http://onosproject.org",
)
