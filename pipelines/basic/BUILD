COMPILE_DEPS = CORE_DEPS + [
    "@minimal_json//jar",
    "//protocols/p4runtime/model:onos-protocols-p4runtime-model",
    "//protocols/p4runtime/api:onos-protocols-p4runtime-api",
]

BUNDLES = [
    "//pipelines/basic:onos-pipelines-basic",
]

osgi_jar(
    deps = COMPILE_DEPS,
)

onos_app(
    app_name = "org.onosproject.pipelines.basic",
    category = "Pipeline",
    description = "Provides pipelines with basic L2/L3 forwarding capabilities and packet-in/out " +
                  "support.",
    included_bundles = BUNDLES,
    required_apps = [
        "org.onosproject.drivers.p4runtime",
    ],
    title = "Basic Pipelines",
    url = "http://onosproject.org",
)
