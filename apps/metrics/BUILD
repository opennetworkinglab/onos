COMPILE_DEPS = CORE_DEPS + JACKSON + METRICS + CLI + REST

osgi_jar_with_tests(
    karaf_command_packages = [
        "org.onosproject.metrics.intent.cli",
        "org.onosproject.metrics.topology.cli",
    ],
    deps = COMPILE_DEPS,
)

onos_app(
    category = "Monitoring",
    description = "Monitoring of various metrics related to topology mutation and intent programming.",
    title = "Topology &amp; Intent Metrics",
    url = "http://onosproject.org",
)
