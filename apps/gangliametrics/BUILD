COMPILE_DEPS = CORE_DEPS + METRICS + [
    "@gmetric4j//jar",
    "@metrics_ganglia//jar",
    "@remotetea_oncrpc//jar",
]

EXCLUDED_BUNDLES = [
    "@gmetric4j//jar",
    "@metrics_ganglia//jar",
    "@remotetea_oncrpc//jar",
]

osgi_jar_with_tests(
    deps = COMPILE_DEPS,
)

onos_app(
    category = "Monitoring",
    description = "Performance metric service reporter",
    excluded_bundles = EXCLUDED_BUNDLES,
    title = "Ganglia Report and Query",
    url = "http://onosproject.org",
)
