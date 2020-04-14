COMPILE_DEPS = CORE_DEPS + CLI + [
    "@org_apache_karaf_bundle_core//jar",
    "@sigar//jar",
    "@jackson_databind//jar",
]

osgi_jar_with_tests(
    karaf_command_packages = [
        "org.onosproject.diagnosis.cli",
        "org.onosproject.diagnosis.impl",
    ],
    deps = COMPILE_DEPS,
)

onos_app(
    name = "onos-apps-node-diagnosis",
    app_name = "org.onosproject.node-diagnosis",
    category = "Utility",
    description = "Controller node diagnosis application. Diagnosis the node errors and trys to recover it.",
    title = "Controller node diagnosis Application",
    url = "http://onosproject.org",
)
