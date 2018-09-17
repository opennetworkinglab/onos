COMPILE_DEPS = CORE_DEPS + METRICS + CLI + [
    "@metrics_influxdb//jar",
    "@influxdb_java//jar",
    "@commons_codec//jar",
    "@retrofit//jar",
    "@okhttp//jar",
    "@logging_interceptor//jar",
    "@okio//jar",
    "@moshi//jar",
    "@converter_moshi//jar",
    "@com_google_code_gson_gson//jar",
]

BUNDLES = [
    "@metrics_influxdb//jar",
    "@influxdb_java//jar",
    "@commons_codec//jar",
    "@retrofit//jar",
    "@okhttp//jar",
    "@logging_interceptor//jar",
    "@com_google_code_gson_gson//jar",
    "@okio//jar",
    "@moshi//jar",
    "@converter_moshi//jar",
]

EXCLUDED_BUNDLES = [
    "@metrics_influxdb//jar",
    "@influxdb_java//jar",
    "@commons_codec//jar",
    "@retrofit//jar",
    "@okhttp//jar",
    "@logging_interceptor//jar",
    "@com_google_code_gson_gson//jar",
    "@okio//jar",
    "@moshi//jar",
    "@converter_moshi//jar",
]

osgi_jar_with_tests(
    exclude_tests = ["org.onosproject.influxdbmetrics.InfluxDbMetricsReporterTest"],
    test_deps = TEST_ADAPTERS,
    deps = COMPILE_DEPS,
)

onos_app(
    category = "Monitoring",
    description = "Performance metric service reporter and retriever for influxDB.",
    excluded_bundles = EXCLUDED_BUNDLES,
    included_bundles = BUNDLES,
    title = "InfluxDB Report and Query",
    url = "http://onosproject.org",
)
