COMPILE_DEPS = CORE_DEPS + METRICS + [
    "@gmetric4j//jar",
    "@metrics_graphite//jar",
]

EXCLUDED_BUNDLES = [
    "@gmetric4j//jar",
    "@metrics_graphite//jar",
]

osgi_jar_with_tests(
    deps = COMPILE_DEPS,
)

onos_app(
    category = "Monitoring",
    description = "Performance metric service reporter and retriever for graphite",
    excluded_bundles = EXCLUDED_BUNDLES,
    title = "Graphite Report and Query",
    url = "http://onosproject.org",
)
