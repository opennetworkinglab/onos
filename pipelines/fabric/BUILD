BUNDLES = [
    "//pipelines/fabric/api:onos-pipelines-fabric-api",
    "//pipelines/fabric/impl:onos-pipelines-fabric-impl",
    "//apps/inbandtelemetry/api:onos-apps-inbandtelemetry-api",
]

onos_app(
    app_name = "org.onosproject.pipelines.fabric",
    category = "Pipeline",
    description = "Provides pipelines with CORD fabric underlay support.",
    included_bundles = BUNDLES,
    required_apps = [
        "org.onosproject.drivers.p4runtime",
        "org.onosproject.pipelines.basic",
    ],
    title = "Fabric Pipeline",
    url = "http://onosproject.org",
)
