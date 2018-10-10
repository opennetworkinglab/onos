COMPILE_DEPS = CORE_DEPS + JACKSON + CLI + ONOS_YANG + [
    "//core/common:onos-core-common",
    "@onos_yang_runtime//jar",
]

BUNDLES = ONOS_YANG + [
    "@onos_yang_runtime//jar",
    "//apps/yang:onos-apps-yang",
    "//apps/yang/web:onos-apps-yang-web",
    "@org_apache_servicemix_bundles_dom4j//jar",
]

EXCLUDED_BUNDLES = [
]

osgi_jar_with_tests(
    karaf_command_packages = ["org.onosproject.yang.impl"],
    deps = COMPILE_DEPS,
)

onos_app(
    category = "Utility",
    description = "Provides ability to register compiled YANG models or even to compile YANG source " +
                  "files on the fly. Registering YANG models allows other ONOS subsystems to interact with " +
                  "orchestrators or individual network devices using XML or JSON data structured in accordance " +
                  "with those models.",
    excluded_bundles = EXCLUDED_BUNDLES,
    included_bundles = BUNDLES,
    title = "YANG Compiler and Runtime",
    url = "http://onosproject.org",
)
