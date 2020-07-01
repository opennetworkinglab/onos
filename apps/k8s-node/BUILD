BUNDLES = JACKSON + [
    "//apps/k8s-node/api:onos-apps-k8s-node-api",
    "//apps/k8s-node/app:onos-apps-k8s-node-app",
    "@commons_net//jar",
    "@k8s_client//jar",
    "@k8s_model//jar",
    "@okhttp//jar",
    "@okio//jar",
    "@logging_interceptor//jar",
    "@jackson_dataformat_yaml//jar",
    "@snakeyaml//jar",
]

onos_app(
    category = "Integration",
    description = "SONA Kubernetes Node Application.",
    included_bundles = BUNDLES,
    required_apps = [
        "org.onosproject.ovsdb-base",
        "org.onosproject.drivers.ovsdb",
    ],
    title = "Kubernetes Node Application",
    url = "https://wiki.onosproject.org/display/ONOS/SONA%3A+DC+Network+Virtualization",
)
