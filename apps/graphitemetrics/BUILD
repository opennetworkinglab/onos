COMPILE_DEPS = CORE_DEPS + METRICS + [
    "@gmetric4j//jar",
    "@metrics_graphite//jar",
]

INCLUDED_BUNDLES = [
    "@gmetric4j//jar",
    "@metrics_graphite//jar",
    "@remotetea_oncrpc//jar",
]

osgi_jar_with_tests(
    deps = COMPILE_DEPS,
)

onos_app(
    category = "Monitoring",
    description = "Performance metric service reporter and retriever for graphite",
    included_bundles = INCLUDED_BUNDLES,
    title = "Graphite Report and Query",
    url = "http://onosproject.org",
)
