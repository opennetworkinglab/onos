COMPILE_DEPS = CORE_DEPS + CLI + ONOS_YANG + JACKSON + JAXB + [
"//apps/odtn/api:onos-apps-odtn-api",
"//core/store/serializers:onos-core-serializers",
"//core/store/primitives:onos-core-primitives",
"//apps/config:onos-apps-config",
"//utils/rest:onlab-rest-native",
"//models/tapi:onos-models-tapi",
"//apps/yang:onos-apps-yang",
"//protocols/netconf/api:onos-protocols-netconf-api",


]

TEST_DEPS = TEST_REST + TEST_ADAPTERS+[
"@minimal_json//jar",
"//utils/osgi:onlab-osgi-tests",
"//web/api:onos-rest-tests",
]

osgi_jar_with_tests(
deps = COMPILE_DEPS,
test_deps = TEST_DEPS,
visibility = ["//visibility:public"],
web_context = "/onos/closfwd-app",
karaf_command_packages = ["org.ctpd.closfwd","org.onosproject.cli",
"org.onosproject.cli.app",
"org.onosproject.cli.cfg",
"org.onosproject.cli.net",
"org.onosproject.cli.security",
],
)
APPS=[
"org.onosproject.yang",
"org.onosproject.config",
"org.onosproject.configsync",
"org.onosproject.models.tapi",
"org.onosproject.odtn-api",

]

BUNDLES = [

"//apps/closfwd:onos-apps-closfwd",
]


onos_app(
app_name = "org.onosproject.closfwd",
category = "Traffic Engineering",
required_apps = APPS,
included_bundles = BUNDLES,
description = "Clos App",
title = "Clos Application",
url = "http://onosproject.org",
)