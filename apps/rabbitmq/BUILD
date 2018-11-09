COMPILE_DEPS = CORE_DEPS + [
    "@com_google_code_gson_gson//jar",
    "@amqp_client//jar",
]

BUNDLES = [
    "@com_google_code_gson_gson//jar",
    "@amqp_client//jar",
    "//apps/rabbitmq:onos-apps-rabbitmq",
]

osgi_jar_with_tests(
    resources = glob(["src/main/resources/**"]),
    resources_root = "src/main/resources",
    test_deps = TEST_ADAPTERS,
    deps = COMPILE_DEPS,
)

onos_app(
    category = "Integration",
    description = "Rabbit MQ application.",
    included_bundles = BUNDLES,
    required_apps = ["org.onosproject.proxyarp"],
    title = "Rabbit MQ Integration",
    url = "http://onosproject.org",
)
