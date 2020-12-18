BUNDLES = JACKSON + [
    "//apps/kubevirt-node/api:onos-apps-kubevirt-node-api",
    "//apps/kubevirt-node/app:onos-apps-kubevirt-node-app",
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
    description = "SONA KubeVirt Node Application.",
    included_bundles = BUNDLES,
    required_apps = [
        "org.onosproject.ovsdb-base",
        "org.onosproject.drivers.ovsdb",
    ],
    title = "KubeVirt Node Application",
    url = "https://wiki.onosproject.org/display/ONOS/SONA%3A+DC+Network+Virtualization",
)
